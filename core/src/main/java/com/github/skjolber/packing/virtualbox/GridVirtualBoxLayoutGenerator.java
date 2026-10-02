package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;

/**
 * Fast rectangular assembly of copies of one original box item. For each allowed
 * orientation, enumerates integer factor triples {@code columns * rows * layers = count}.
 * Every cell uses that same orientation: no gaps, mixed items, invented rotations or
 * rounded-up inventory. Prime counts can therefore only form lines.
 *
 * <p>Candidates must fit at least one supplied container, including its weight limit.
 * Ranking prefers the most exactly matched container axes, then the smallest surface
 * area, height, depth and width. All candidates have the same volume. Only the best
 * {@code maxLayouts} distinct envelopes are materialized; factor lists are reused
 * across orientations. This is bounded preprocessing, not a permutation search.
 *
 * <p>Internal load constraints are checked here in constant time per candidate.
 * Each column has full-face contacts; its bottom box carries the greatest weight,
 * contact pressure and number of boxes above it. Identical-item constraints hold by
 * construction. No mutable load graph is needed to decide whether a grid is valid.
 * These checks cover the grid alone, not loads from other packed items. The wrapper
 * bypasses aggregation for load-constrained operations; these layouts must not be
 * treated as load-aware envelope boxes.
 *
 * <p>Returned layouts borrow original box items and orientations. Do not modify those
 * objects, the returned lists or their placements during use. Placement lists contain
 * geometry only; there is no per-layout contact graph or physical search state.
 */
public class GridVirtualBoxLayoutGenerator {
	/** Generated geometry is valid by construction; do not repeat arbitrary-layout overlap checks. */
	protected static class GridLayout extends VirtualBoxLayout {
		protected final BoxStackValue value;

		protected GridLayout(Grid grid, List<Placement> placements) {
			super(grid.bounds, placements);
			value = grid.value;
		}

		@Override
		protected void prepareGeometry() {
			// Factor grids are filled and non-overlapping by construction.
		}

	}

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
			if(comparison != 0) {
				return comparison;
			}
			VirtualBoxBounds a = left.bounds, b = right.bounds;
			comparison = Long.compare((long) a.dx() * a.dy() + (long) a.dx() * a.dz() + (long) a.dy() * a.dz(),
					(long) b.dx() * b.dy() + (long) b.dx() * b.dz() + (long) b.dy() * b.dz());
			if(comparison != 0) {
				return comparison;
			}
			comparison = Integer.compare(a.dz(), b.dz());
			if(comparison != 0) {
				return comparison;
			}
			comparison = Integer.compare(a.dy(), b.dy());
			return comparison != 0 ? comparison : Integer.compare(a.dx(), b.dx());
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
		return generate(item, item.getCount(), containers, maxLayouts, interrupt);
	}

	/**
	 * Generate a subset of the original count, for selective refinement. The original
	 * item and its orientations are retained unchanged; no cloned inventory is needed.
	 * On interruption, return only layouts whose placements have been completed.
	 */
	public List<VirtualBoxLayout> generate(BoxItem item, int count, List<Container> containers, int maxLayouts, BooleanSupplier interrupt) {
		if(count <= 0 || count > item.getCount() || maxLayouts <= 0) {
			throw new IllegalArgumentException("Expected positive count and layout limit");
		}
		if(item.getGroup() != null) {
			throw new IllegalArgumentException("Virtual layouts do not support box-item groups");
		}
		long weight = (long) count * item.getBox().getWeight();
		if(weight > Integer.MAX_VALUE || item.getBox().getWeight() < 0 || containers.isEmpty() || interrupt.getAsBoolean()) {
			return List.of();
		}
		List<Grid> best = new ArrayList<>();
		List<Integer> columns = divisors(count);
		List<List<Integer>> rows = new ArrayList<>(columns.size());
		for(int x : columns) {
			rows.add(divisors(count / x));
		}
		for(BoxStackValue value : item.getBox().getStackValues()) {
			VirtualBoxBounds.validateDimensions(value.getDx(), value.getDy(), value.getDz());
			if(value.getVolume() <= 0 || value.getVolume() != item.getBox().getVolume()
					|| value.getVolume() > Long.MAX_VALUE / count) {
				throw new IllegalArgumentException("Expected positive, equal-volume orientations");
			}
			for(int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
				int x = columns.get(columnIndex);
				for(int y : rows.get(columnIndex)) {
					if(interrupt.getAsBoolean()) {
						return materialize(count, best, interrupt);
					}
					int z = count / x / y;
					if(!fitsLoad(value, z)) {
						continue;
					}
					long dx = (long) x * value.getDx(), dy = (long) y * value.getDy(), dz = (long) z * value.getDz();
					int matches = -1;
					for(Container container : containers) {
						if(dx <= container.getLoadDx() && dy <= container.getLoadDy() && dz <= container.getLoadDz()
									&& weight <= container.getMaxLoadWeight()) {
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
		return materialize(count, best, interrupt);
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

	protected static List<VirtualBoxLayout> materialize(int count, List<Grid> grids, BooleanSupplier interrupt) {
		List<VirtualBoxLayout> result = new ArrayList<>(grids.size());
		for(Grid grid : grids) {
			List<Placement> placements = new ArrayList<>(count);
			for(int z = 0; z < grid.layers(); z++) {
				for(int y = 0; y < grid.rows(); y++) {
					for(int x = 0; x < grid.columns(); x++) {
						if(interrupt.getAsBoolean()) {
							return result;
						}
						placements.add(new Placement(grid.value(), -1, x * grid.value().getDx(), y * grid.value().getDy(), z * grid.value().getDz(), false));
					}
				}
			}
			result.add(new GridLayout(grid, placements));
		}
		return result;
	}
}
