package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.virtualbox.bounds.VirtualBoxBounds;

/** Enumerates factor grids without searching permutations or allocating every candidate's placements. */
public class GridVirtualBoxLayoutGenerator {
	protected static class Grid {
		protected final VirtualBoxBounds bounds;
		protected final BoxStackValue value;
		protected final int columns;
		protected final int rows;
		protected final int layers;
		protected final int matchingAxes;

		protected Grid(VirtualBoxBounds bounds, BoxStackValue value, int columns, int rows, int layers, int matchingAxes) {
			this.bounds = bounds;
			this.value = value;
			this.columns = columns;
			this.rows = rows;
			this.layers = layers;
			this.matchingAxes = matchingAxes;
		}

		protected VirtualBoxBounds bounds() { return bounds; }
		protected BoxStackValue value() { return value; }
		protected int columns() { return columns; }
		protected int rows() { return rows; }
		protected int layers() { return layers; }
		protected int matchingAxes() { return matchingAxes; }
	}

	@FunctionalInterface
	protected interface GridComparator {
		int compare(Grid left, Grid right);
	}

	protected static class DefaultGridComparator implements GridComparator {
		@Override
		public int compare(Grid left, Grid right) {
			int comparison = Integer.compare(right.matchingAxes(), left.matchingAxes());
			return comparison != 0 ? comparison : VirtualBoxBounds.MIN_VOLUME.compare(left.bounds(), right.bounds());
		}
	}

	protected final GridComparator comparator = new DefaultGridComparator();

	public GridVirtualBoxLayoutGenerator() {
	}

	/**
	 * Every returned layout contains the item's entire count. Prefer matching
	 * container dimensions, then compact envelopes. No additional rotations are invented.
	 */
	public List<VirtualBoxLayout> generate(BoxItem item, List<Container> containers, int maxLayouts, BooleanSupplier interrupt) {
		if(item.getCount() <= 0 || maxLayouts <= 0) {
			throw new IllegalArgumentException("Expected positive count and layout limit");
		}
		if(item.getGroup() != null) {
			throw new IllegalArgumentException("Virtual layouts do not support box-item groups");
		}
		if(item.getWeight() > Integer.MAX_VALUE || item.getBox().getWeight() < 0) {
			return List.of();
		}
		List<Grid> best = new ArrayList<>();
		List<Integer> columns = divisors(item.getCount());
		for(BoxStackValue value : item.getBox().getStackValues()) {
			if(value.getVolume() <= 0 || value.getVolume() != item.getBox().getVolume()) {
				throw new IllegalArgumentException("Expected positive, equal-volume orientations");
			}
			for(int x : columns) {
				for(int y : divisors(item.getCount() / x)) {
					if(interrupt.getAsBoolean()) {
						return materialize(item, best, interrupt);
					}
					int z = item.getCount() / x / y;
					if(!fitsLoad(value, z)) {
						continue;
					}
					long dx = (long) x * value.getDx(), dy = (long) y * value.getDy(), dz = (long) z * value.getDz();
					int matches = -1;
					for(Container container : containers) {
						if(dx <= container.getLoadDx() && dy <= container.getLoadDy() && dz <= container.getLoadDz()
								&& item.getWeight() <= container.getMaxLoadWeight()) {
							matches = Math.max(matches, (dx == container.getLoadDx() ? 1 : 0)
									+ (dy == container.getLoadDy() ? 1 : 0) + (dz == container.getLoadDz() ? 1 : 0));
						}
					}
					if(matches < 0) {
						continue;
					}
					VirtualBoxBounds bounds = new VirtualBoxBounds((int) dx, (int) dy, (int) dz);
					boolean duplicate = false;
					for(Grid grid : best) {
						if(grid.bounds().equals(bounds)) {
							duplicate = true;
							break;
						}
					}
					if(duplicate) {
						continue;
					}
					Grid candidate = new Grid(bounds, value, x, y, z, matches);
					int position = best.size();
					while(position > 0 && comparator.compare(candidate, best.get(position - 1)) < 0) {
						position--;
					}
					best.add(position, candidate);
					if(best.size() > maxLayouts) {
						best.remove(best.size() - 1);
					}
				}
			}
		}
		return materialize(item, best, interrupt);
	}

	/**
	 * A uniform grid is a set of identical full-contact columns. The bottom box
	 * carries the maximum weight, pressure and depth; all upper layers carry less.
	 */
	protected boolean fitsLoad(BoxStackValue value, int layers) {
		long weight = (long) (layers - 1) * value.getBox().getWeight();
		return (!value.isMaxLoadWeight() || weight <= value.getMaxLoadWeight())
				&& (!value.isMaxLoadPressure() || Box.calculatePressure(value.getArea(), weight) <= value.getMaxLoadPressure())
				&& (!value.isMaxLoadBoxCount() || layers - 1 <= value.getMaxLoadBoxCount());
	}

	protected static List<Integer> divisors(int count) {
		List<Integer> result = new ArrayList<>();
		for(int i = 1; i <= count / i; i++) {
			if(count % i == 0) {
				result.add(i);
				if(i != count / i) {
					result.add(count / i);
				}
			}
		}
		java.util.Collections.sort(result);
		return result;
	}

	protected static List<VirtualBoxLayout> materialize(BoxItem item, List<Grid> grids, BooleanSupplier interrupt) {
		List<VirtualBoxLayout> result = new ArrayList<>();
		for(Grid grid : grids) {
			List<Placement> placements = new ArrayList<>(item.getCount());
			for(int z = 0; z < grid.layers(); z++) {
				for(int y = 0; y < grid.rows(); y++) {
					for(int x = 0; x < grid.columns(); x++) {
						if(interrupt.getAsBoolean()) {
							return List.copyOf(result);
						}
						placements.add(new Placement(grid.value(), -1, x * grid.value().getDx(), y * grid.value().getDy(), z * grid.value().getDz(), false));
					}
				}
			}
			VirtualBoxLayout layout = new VirtualBoxLayout(grid.bounds(), placements);

			result.add(layout);
		}
		return List.copyOf(result);
	}
}
