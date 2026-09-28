package com.github.skjolber.packing.virtualbox;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;

/**
 * Alternative filled layouts of exactly the same original inventory. A packing
 * selects one layout, never all alternatives. Every delegate item has count one.
 */
public class VirtualBox {
	protected final List<VirtualBoxLayout> layouts;
	protected final int weight;

	protected VirtualBox(List<VirtualBoxLayout> layouts, int weight) {
		this.layouts = List.copyOf(layouts);
		this.weight = weight;
	}

	/** Validate equivalent inventory and aggregate weight once, outside placement construction. */
	public static VirtualBox of(List<VirtualBoxLayout> layouts) {
		if(layouts.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one layout");
		}
		Map<BoxItem, Integer> inventory = inventory(layouts.get(0));
		long totalWeight = 0;
		for(var entry : inventory.entrySet()) {
			if(entry.getKey().getBox().getWeight() < 0) {
				throw new IllegalArgumentException("Expected non-negative weights");
			}
			totalWeight += (long) entry.getKey().getBox().getWeight() * entry.getValue();
			if(totalWeight > Integer.MAX_VALUE) {
				throw new IllegalArgumentException("Virtual box weight exceeds the Box weight range");
			}
		}
		for(VirtualBoxLayout layout : layouts) {
			Map<BoxItem, Integer> candidate = inventory(layout);
			if(candidate.size() != inventory.size()) {
				throw new IllegalArgumentException("Layouts must contain the same original inventory");
			}
			for(var entry : candidate.entrySet()) {
				if(!entry.getValue().equals(inventory.get(entry.getKey()))) {
					throw new IllegalArgumentException("Layouts must contain the same original inventory");
				}
			}
		}
		return new VirtualBox(layouts, (int) totalWeight);
	}

	protected static Map<BoxItem, Integer> inventory(VirtualBoxLayout layout) {
		Map<BoxItem, Integer> result = new IdentityHashMap<>();
		for(Placement placement : layout.getPlacements()) {
			result.merge(placement.getBoxItem(), 1, Integer::sum);
		}
		return result;
	}

	public List<VirtualBoxLayout> getLayouts() {
		return layouts;
	}

	public int getWeight() {
		return weight;
	}

	/** Create operation-local inventory. Stack-value indexes identify layouts, including after cloning. */
	public BoxItem toBoxItem(int globalIndex) {
		BoxStackValue[] values = new BoxStackValue[layouts.size()];
		for(int i = 0; i < values.length; i++) {
			var bounds = layouts.get(i).getBoundingBox();
			values[i] = BoxStackValue.newBuilder().withDimensions(bounds.dx(), bounds.dy(), bounds.dz()).withIndex(i).build();
		}
		Box box = new Box(null, "Virtual box", layouts.get(0).getBoundingBox().getVolume(), weight, values, Map.of(), null);
		return new BoxItem(box, 1, -1, globalIndex);
	}
}
