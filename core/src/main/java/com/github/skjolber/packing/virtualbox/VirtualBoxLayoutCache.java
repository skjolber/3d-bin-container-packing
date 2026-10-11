package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/**
 * Operation-local cache. Keys refer to original inventory positions and counts,
 * never product IDs, mutable local indexes or another operation's box objects.
 * Equal keys return the same {@link VirtualBox} instance, so equal blocks can be
 * merged into one counted delegate item. Container limits and generator settings
 * remain fixed for the cache lifetime.
 */
public class VirtualBoxLayoutCache {
	/** Cached marker for counts without a fitting grid. */
	protected static final VirtualBox NONE = new VirtualBox(List.of(), 0);

	protected final List<BoxItem> originals;
	protected final List<Container> containers;
	protected final List<ContainerItem> containerItems;
	protected final int maxContainerCount;
	protected final GridVirtualBoxLayoutGenerator grids;
	protected final int maxGridBoxes;
	protected final int maxLayouts;
	protected final LongObjectHashMap<VirtualBox> cached = new LongObjectHashMap<>();

	/**
	 * @param containerItems available containers (positive counts), for partitioning
	 * @param maxContainerCount maximum number of containers in a result
	 */
	protected VirtualBoxLayoutCache(List<BoxItem> originals, List<ContainerItem> containerItems, int maxContainerCount, GridVirtualBoxLayoutGenerator grids,
			int maxGridBoxes, int maxLayouts) {
		this.originals = originals;
		this.containerItems = containerItems;
		this.maxContainerCount = maxContainerCount;
		this.containers = new ArrayList<>(containerItems.size());
		for(ContainerItem containerItem : containerItems) {
			containers.add(containerItem.getContainer());
		}
		this.grids = grids;
		this.maxGridBoxes = maxGridBoxes;
		this.maxLayouts = maxLayouts;
	}

	/**
	 * Original inventory position and requested subset count; neither input is mutated.
	 * Returns null if the count forms no fitting grid, or on interruption.
	 */
	protected VirtualBox get(int originalIndex, int count, PackagerInterruptSupplier stop) {
		long key = key(originalIndex, count);
		VirtualBox result = cached.get(key);
		if(result != null) {
			return result == NONE ? null : result;
		}
		if(count <= 1 || count > maxGridBoxes || stop.getAsBoolean()) {
			return null;
		}
		// Reuse original orientations and identities: no box copies or placement remapping.
		List<VirtualBoxLayout> layouts = grids.generate(originals.get(originalIndex), count, containers, maxLayouts, stop::getAsBoolean);
		if(stop.getAsBoolean()) {
			return null;
		}
		result = layouts.isEmpty() ? NONE : VirtualBox.of(layouts);
		cached.put(key, result);
		return result == NONE ? null : result;
	}

	/** Container-sized grid blocks for a count which does not form one fitting grid. */
	protected int[] partition(int originalIndex, int count) {
		return grids.partition(originals.get(originalIndex), count, containerItems, maxContainerCount, maxGridBoxes);
	}

	protected static long key(int originalIndex, int count) {
		return ((long) originalIndex << 32) | count;
	}
}
