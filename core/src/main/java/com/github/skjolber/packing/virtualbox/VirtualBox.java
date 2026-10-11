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
 * selects one layout per placed copy, never all alternatives. Equal virtual boxes may be
 * passed to the delegate as one item with a count greater than one.
 */
public class VirtualBox {
	protected final List<VirtualBoxLayout> layouts;
	protected final int weight;

	/** Retain layouts directly; callers must not change the list or its contents during use. */
	protected VirtualBox(List<VirtualBoxLayout> layouts, int weight) {
		this.layouts = layouts;
		this.weight = weight;
	}

	/**
	 * Validate equivalent inventory and aggregate weight once, outside placement construction.
	 * The list, layouts and original inventory are borrowed and must not be modified during use.
	 */
	public static VirtualBox of(List<VirtualBoxLayout> layouts) {
		if(layouts.isEmpty()) {
			throw new IllegalArgumentException("Expected at least one layout");
		}
		// Grid generation already proves geometry, identity, count and weight bounds.
		// Compare this metadata instead of recounting every child into identity maps.
		if(layouts.get(0) instanceof GridVirtualBoxLayoutGenerator.GridLayout first) {
			boolean grids = true;
			for(VirtualBoxLayout layout : layouts) {
				if(!(layout instanceof GridVirtualBoxLayoutGenerator.GridLayout grid)) {
					grids = false;
					break;
				}
				if(grid.item != first.item || grid.count != first.count) {
					throw new IllegalArgumentException("Layouts must contain the same original inventory");
				}
				grid.prepare();
			}
			if(grids) {
				return new VirtualBox(layouts, (int) ((long) first.count * first.value.getBox().getWeight()));
			}
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
			layout.prepare();
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

	/** Create operation-local inventory. Stack-value indexes identify layouts, including after copying. */
	public BoxItem toBoxItem(int globalIndex) {
		return toBoxItem(globalIndex, 1);
	}

	/**
	 * Create operation-local inventory for {@code count} interchangeable copies of this virtual box.
	 * A counted item lets permutation searches treat equal blocks as identical boxes.
	 */
	public BoxItem toBoxItem(int globalIndex, int count) {
		BoxStackValue[] values = new BoxStackValue[layouts.size()];
		for(int i = 0; i < values.length; i++) {
			var bounds = layouts.get(i).getBounds();
			values[i] = BoxStackValue.newBuilder().withDimensions(bounds.dx(), bounds.dy(), bounds.dz()).withIndex(i).build();
		}
		Box box = new Box(null, "Virtual box", layouts.get(0).getBounds().getVolume(), weight, values, Map.of());
		return new BoxItem(box, count, -1, globalIndex);
	}
}
