package com.github.skjolber.packing.virtualbox;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.virtualbox.bounds.BruteForceVirtualBoxBoundsGenerator;
import com.github.skjolber.packing.virtualbox.bounds.LoadBruteForceVirtualBoxBoundsGenerator;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBoundsComparator;

/** Bounded, layout search, including internal load constraints, for small mixed or repeated inventories. */
public class BruteForceVirtualBoxLayoutGenerator {
	protected final int maxBoxes;
	protected final int maxLayouts;

	public BruteForceVirtualBoxLayoutGenerator(int maxBoxes, int maxLayouts) {
		if(maxBoxes < 2 || maxLayouts <= 0) {
			throw new IllegalArgumentException("Expected at least two boxes and a positive layout limit");
		}
		this.maxBoxes = maxBoxes;
		this.maxLayouts = maxLayouts;
	}

	/**
	 * Search within actual container limits using the caller's interrupt supplier,
	 * which may also enforce a deadline. This method borrows and does not close it.
	 * An interruption can return already discovered filled layouts, never partial or hollow ones.
	 */
	public List<VirtualBoxLayout> generate(List<BoxItem> items, List<Container> containers, PackagerInterruptSupplier interrupt) {
		long count = 0, volume = 0, weight = 0;
		boolean load = false;
		Map<Box, BoxItem> originals = new java.util.IdentityHashMap<>();
		for(BoxItem item : items) {
			if(interrupt.getAsBoolean()) {
				return List.of();
			}
			if(item.getCount() <= 0 || item.getGroup() != null
					|| item.getBox().getWeight() < 0 || item.getBox().getVolume() <= 0) {
				throw new IllegalArgumentException("Expected positive, ungrouped inventory");
			}
			if(originals.put(item.getBox(), item) != null) {
				throw new IllegalArgumentException("Expected distinct original Box instances");
			}
			load |= item.getBox().isMaxLoad() || item.getBox().isLoadIdenticalBoxOnly();
			count += item.getCount();
			if(count > maxBoxes || item.getBox().getVolume() > (Long.MAX_VALUE - volume) / item.getCount()) {
				return List.of();
			}
			volume += item.getVolume();
			weight += item.getWeight();
			if(weight > Integer.MAX_VALUE) {
				return List.of();
			}
		}
		if(count < 2) {
			return List.of();
		}
		final long totalVolume = volume;
		Map<VirtualBoxBounds, VirtualBoxLayout> layouts = new LinkedHashMap<>();
		try(BruteForceVirtualBoxBoundsGenerator search = load ? new LoadBruteForceVirtualBoxBoundsGenerator() : new BruteForceVirtualBoxBoundsGenerator()) {
			for(Container container : containers) {
				if(interrupt.getAsBoolean()) {
					break;
				}
				if(volume > container.getMaxLoadVolume() || weight > container.getMaxLoadWeight()) {
					continue;
				}
				// Filled assemblies rank ahead of hollow envelopes even when an axis goal is unreachable.
				VirtualBoxBoundsComparator xOrder = filledFirst(totalVolume, VirtualBoxBounds.MIN_X);
				VirtualBoxBoundsComparator yOrder = filledFirst(totalVolume, VirtualBoxBounds.MIN_Y);
				VirtualBoxBoundsComparator zOrder = filledFirst(totalVolume, VirtualBoxBounds.MIN_Z);
				int xTarget = minimumExtent(volume, container.getLoadDy(), container.getLoadDz());
				int yTarget = minimumExtent(volume, container.getLoadDx(), container.getLoadDz());
				int zTarget = minimumExtent(volume, container.getLoadDx(), container.getLoadDy());
				var result = search.newResultBuilder().withBoxItems(items).withContainer(container)
						.withObjective("filled", b -> b.getVolume() == totalVolume, VirtualBoxBounds.MIN_VOLUME)
						.withObjective("x", b -> b.getVolume() == totalVolume && b.dx() <= xTarget, xOrder)
						.withObjective("y", b -> b.getVolume() == totalVolume && b.dy() <= yTarget, yOrder)
						.withObjective("z", b -> b.getVolume() == totalVolume && b.dz() <= zTarget, zOrder)
						.withInterrupt(interrupt::getAsBoolean).build();
				for(var layout : result.getResults()) {
					VirtualBoxBounds bounds = layout.getBoundingBox();
					// Goals are stopping criteria, not hard filters. Check every best-so-far result.
					if(bounds.getVolume() != totalVolume || layouts.containsKey(bounds)) {
						continue;
					}
					// Search snapshots already retain original box identities and relative coordinates.
					layouts.put(bounds, layout);
					if(layouts.size() == maxLayouts) {
						return List.copyOf(layouts.values());
					}
				}
			}
		}
		return List.copyOf(layouts.values());
	}

	protected VirtualBoxBoundsComparator filledFirst(long volume, VirtualBoxBoundsComparator tieBreaker) {
		return (left, right) -> {
			int comparison = Boolean.compare(right.getVolume() == volume, left.getVolume() == volume);
			return comparison != 0 ? comparison : tieBreaker.compare(left, right);
		};
	}

	protected static int minimumExtent(long volume, int a, int b) {
		long area = (long) a * b;
		return (int) (volume / area + (volume % area == 0 ? 0 : 1));
	}
}
