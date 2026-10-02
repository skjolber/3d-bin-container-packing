package com.github.skjolber.packing.virtualbox;

import java.util.List;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/**
 * Operation-local cache. Keys refer to original inventory positions and counts,
 * never product IDs, mutable local indexes or another operation's box objects.
 * Container limits and generator settings remain fixed for the cache lifetime.
 */
public class VirtualBoxLayoutCache {
	protected final List<BoxItem> originals;
	protected final List<Container> containers;
	protected final GridVirtualBoxLayoutGenerator grids;
	protected final int maxGridBoxes;
	protected final int maxLayouts;
	protected final LongObjectHashMap<List<VirtualBoxLayout>> cached = new LongObjectHashMap<>();

	protected VirtualBoxLayoutCache(List<BoxItem> originals, List<Container> containers, GridVirtualBoxLayoutGenerator grids,
			int maxGridBoxes, int maxLayouts) {
		this.originals = originals;
		this.containers = containers;
		this.grids = grids;
		this.maxGridBoxes = maxGridBoxes;
		this.maxLayouts = maxLayouts;
	}

	protected void put(int originalIndex, int count, List<VirtualBoxLayout> layouts) {
		cached.put(key(originalIndex, count), layouts);
	}

	/** Original inventory position and requested subset count; neither input is mutated. */
	protected List<VirtualBoxLayout> get(int originalIndex, int count, PackagerInterruptSupplier stop) {
		long key = key(originalIndex, count);
		List<VirtualBoxLayout> result = cached.get(key);
		if(result != null) {
			return result;
		}
		if(stop.getAsBoolean()) {
			return List.of();
		}
		// Reuse original orientations and identities: no box clones or placement remapping.
		result = List.of();
		if(count > 1 && count <= maxGridBoxes) {
			result = grids.generate(originals.get(originalIndex), count, containers, maxLayouts, stop::getAsBoolean);
		}
		if(!stop.getAsBoolean()) {
			cached.put(key, result);
		}
		return result;
	}

	protected static long key(int originalIndex, int count) {
		return ((long) originalIndex << 32) | count;
	}
}
