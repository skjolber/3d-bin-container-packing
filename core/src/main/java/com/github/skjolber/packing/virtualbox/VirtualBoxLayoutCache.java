package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/**
 * Operation-local cache. Keys refer to original inventory positions and counts,
 * never product IDs, mutable local indexes or another operation's box objects.
 * Container limits and generator settings remain fixed for the cache lifetime.
 */
public class VirtualBoxLayoutCache {
	protected static class Key {
		protected final int[] counts;
		protected Key(int[] counts) { this.counts = counts; }
		@Override
		public int hashCode() { return Arrays.hashCode(counts); }
		@Override
		public boolean equals(Object other) { return other instanceof Key key && Arrays.equals(counts, key.counts); }
	}

	protected final List<BoxItem> originals;
	protected final List<Container> containers;
	protected final GridVirtualBoxLayoutGenerator grids;
	protected final BruteForceVirtualBoxLayoutGenerator search;
	protected final int maxGridBoxes;
	protected final int maxLayouts;
	protected final Map<Key, List<VirtualBoxLayout>> cached = new HashMap<>();
	protected int remainingSearches;

	protected VirtualBoxLayoutCache(List<BoxItem> originals, List<Container> containers, GridVirtualBoxLayoutGenerator grids,
			BruteForceVirtualBoxLayoutGenerator search, int maxGridBoxes, int maxLayouts, int remainingSearches) {
		this.originals = originals;
		this.containers = containers;
		this.grids = grids;
		this.search = search;
		this.maxGridBoxes = maxGridBoxes;
		this.maxLayouts = maxLayouts;
		this.remainingSearches = remainingSearches;
	}

	protected void put(int[] inventory, List<VirtualBoxLayout> layouts) {
		cached.put(new Key(inventory), layouts);
	}

	/** Sparse inventory: original position, count, original position, count, ... */
	protected List<VirtualBoxLayout> get(int[] inventory, PackagerInterruptSupplier stop) {
		Key key = new Key(inventory);
		List<VirtualBoxLayout> result = cached.get(key);
		if(result != null) {
			return result;
		}
		if(stop.getAsBoolean()) {
			return List.of();
		}
		List<BoxItem> subset = new ArrayList<>();
		Map<BoxStackValue, BoxStackValue> originalValues = new IdentityHashMap<>();
		long count = 0;
		for(int i = 0; i < inventory.length; i += 2) {
			BoxItem original = originals.get(inventory[i]);
			BoxItem copy = new BoxItem(original.getBox().clone(), inventory[i + 1], -1, original.getGlobalIndex());
			BoxStackValue[] values = copy.getBox().getStackValues();
			BoxStackValue[] source = original.getBox().getStackValues();
			for(int j = 0; j < values.length; j++) {
				originalValues.put(values[j], source[j]);
			}
			subset.add(copy);
			count += copy.getCount();
		}
		List<VirtualBoxLayout> generated = List.of();
		if(count > 1 && subset.size() == 1 && count <= maxGridBoxes) {
			generated = grids.generate(subset.get(0), containers, maxLayouts, stop::getAsBoolean);
		}
		if(count > 1 && generated.isEmpty() && search != null && remainingSearches > 0 && !stop.getAsBoolean()) {
			remainingSearches--;
			generated = search.generate(subset, containers, stop);
		}
		List<VirtualBoxLayout> restored = new ArrayList<>(generated.size());
		for(VirtualBoxLayout layout : generated) {
			List<Placement> placements = new ArrayList<>(layout.getPlacements().size());
			for(Placement placement : layout.getPlacements()) {
				placements.add(new Placement(originalValues.get(placement.getStackValue()), -1,
						placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(), false));
			}
			restored.add(new VirtualBoxLayout(layout.getBoundingBox(), placements));
		}
		result = List.copyOf(restored);
		if(!stop.getAsBoolean()) {
			cached.put(key, result);
		}
		return result;
	}
}
