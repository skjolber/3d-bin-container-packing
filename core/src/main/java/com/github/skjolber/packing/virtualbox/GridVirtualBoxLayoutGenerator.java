package com.github.skjolber.packing.virtualbox;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;

/**
 * Fast rectangular assembly of copies of one original box item. For each allowed
 * orientation, enumerates integer factor triples {@code columns * rows * layers = count}.
 * Every cell uses that same orientation: no gaps, mixed items, invented rotations or
 * rounded-up inventory. Prime counts can therefore only form lines; use
 * {@link #partition(BoxItem, int, List, int)} to split such counts into several grids.
 *
 * <p>Candidates must fit at least one supplied container, including its weight limit.
 * Ranking prefers the most matched container axes, then the smallest surface
 * area, height, depth and width. An axis matches when the remaining container length is
 * shorter than the minimum usable dimension, i.e. the leftover strip is wasted anyway.
 * All candidates have the same volume. Every container which can hold the count as a
 * grid keeps its own best layout, so a subset of a fitting grid always has a layout
 * fitting the same container; the remaining places up to {@code maxLayouts} go to the
 * best ranked distinct envelopes. Factor lists are reused across orientations. This is
 * bounded preprocessing, not a permutation search.
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
 * objects, the returned lists or their placements during use. Placements are created
 * on first access, so layouts which are never expanded allocate no child placements.
 * Placement lists contain geometry only; there is no per-layout contact graph.
 */
public class GridVirtualBoxLayoutGenerator {
	/** Generated geometry is valid by construction; do not repeat arbitrary-layout overlap checks. */
	protected static class GridLayout extends VirtualBoxLayout {
		protected final BoxItem item;
		protected final BoxStackValue value;
		protected final int columns;
		protected final int rows;
		protected final int layers;
		protected final int count;
		protected volatile List<Placement> gridPlacements;

		protected GridLayout(Grid grid) {
			super(grid.bounds, null);
			item = grid.item;
			value = grid.value;
			columns = grid.columns;
			rows = grid.rows;
			layers = grid.layers;
			count = grid.columns * grid.rows * grid.layers;
		}

		@Override
		public List<Placement> getPlacements() {
			List<Placement> result = gridPlacements;
			if(result == null) {
				synchronized(this) {
					result = gridPlacements;
					if(result == null) {
						result = new ArrayList<>(count);
						for(int z = 0; z < layers; z++) {
							for(int y = 0; y < rows; y++) {
								for(int x = 0; x < columns; x++) {
									result.add(new Placement(item, value, -1, x * value.getDx(), y * value.getDy(), z * value.getDz(), false));
								}
							}
						}
						gridPlacements = result;
					}
				}
			}
			return result;
		}

		@Override
		protected void prepareGeometry() {
			// Factor grids are filled and non-overlapping by construction.
		}
	}

	protected static class Grid {
		protected final VirtualBoxBounds bounds;
		protected final BoxItem item;
		protected final BoxStackValue value;
		protected final int columns;
		protected final int rows;
		protected final int layers;
		protected final int matchingAxes;

		protected Grid(VirtualBoxBounds bounds, BoxItem item, BoxStackValue value, int columns, int rows, int layers, int matchingAxes) {
			this.bounds = bounds;
			this.item = item;
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

	/** Ranks by matched container axes, then shape. */
	protected static class DefaultGridComparator implements GridComparator {
		protected final GridComparator shape = new ShapeGridComparator();

		@Override
		public int compare(Grid left, Grid right) {
			int comparison = Integer.compare(right.matchingAxes(), left.matchingAxes());
			return comparison != 0 ? comparison : shape.compare(left, right);
		}
	}

	/** Prefers compact envelopes: smallest surface area, then height, depth and width. */
	protected static class ShapeGridComparator implements GridComparator {
		@Override
		public int compare(Grid left, Grid right) {
			VirtualBoxBounds a = left.bounds, b = right.bounds;
			int comparison = Long.compare((long) a.dx() * a.dy() + (long) a.dx() * a.dz() + (long) a.dy() * a.dz(),
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

	protected static final int[] NO_BLOCKS = new int[0];

	protected final GridComparator comparator = new DefaultGridComparator();
	protected final GridComparator shapeComparator = new ShapeGridComparator();
	protected final int minimumUsableDimension;

	/** Exact container axis matches only. */
	public GridVirtualBoxLayoutGenerator() {
		this(1);
	}

	/**
	 * @param minimumUsableDimension smallest dimension of any box which could use leftover space;
	 *        shorter leftovers count as matched container axes.
	 */
	public GridVirtualBoxLayoutGenerator(int minimumUsableDimension) {
		if(minimumUsableDimension <= 0) {
			throw new IllegalArgumentException("Expected a positive minimum usable dimension");
		}
		this.minimumUsableDimension = minimumUsableDimension;
	}

	/**
	 * Every returned layout contains the item's entire count. Prefer matching
	 * container dimensions, then compact envelopes. No additional rotations are invented.
	 */
	public List<VirtualBoxLayout> generate(BoxItem item, List<Container> containers, int maxLayouts, BooleanSupplier interrupt) {
		return generate(item, item.getCount(), containers, maxLayouts, interrupt);
	}

	/**
	 * Generate a subset of the original count, for partitions and selective refinement.
	 * The original item and its orientations are retained unchanged; no copied inventory
	 * is needed. On interruption, return the layouts ranked so far.
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
		// best layout per container, by that container's matched axes, then shape
		Grid[] covering = new Grid[containers.size()];
		int[] coveringMatches = new int[containers.size()];
		int[] containerMatches = new int[containers.size()];
		List<Integer> columns = divisors(count);
		List<List<Integer>> rows = new ArrayList<>(columns.size());
		for(int x : columns) {
			rows.add(divisors(count / x));
		}
		for(BoxStackValue value : item.getBox().getStackValues()) {
			validate(item, value, count);
			for(int columnIndex = 0; columnIndex < columns.size(); columnIndex++) {
				int x = columns.get(columnIndex);
				for(int y : rows.get(columnIndex)) {
					if(interrupt.getAsBoolean()) {
						return select(best, covering, maxLayouts);
					}
					int z = count / x / y;
					if(!fitsLoad(value, z)) {
						continue;
					}
					long dx = (long) x * value.getDx(), dy = (long) y * value.getDy(), dz = (long) z * value.getDz();
					int matches = -1;
					for(int i = 0; i < containers.size(); i++) {
						Container container = containers.get(i);
						containerMatches[i] = -1;
						if(dx <= container.getLoadDx() && dy <= container.getLoadDy() && dz <= container.getLoadDz()
									&& weight <= container.getMaxLoadWeight()) {
							containerMatches[i] = (container.getLoadDx() - dx < minimumUsableDimension ? 1 : 0)
									+ (container.getLoadDy() - dy < minimumUsableDimension ? 1 : 0) + (container.getLoadDz() - dz < minimumUsableDimension ? 1 : 0);
							matches = Math.max(matches, containerMatches[i]);
						}
					}
					if(matches < 0) {
						continue;
					}
					VirtualBoxBounds bounds = new VirtualBoxBounds((int) dx, (int) dy, (int) dz);
					if(containsBounds(best, bounds)) {
						continue;
					}
					Grid candidate = new Grid(bounds, item, value, x, y, z, matches);
					for(int i = 0; i < containers.size(); i++) {
						int containerMatch = containerMatches[i];
						if(containerMatch >= 0 && (covering[i] == null || containerMatch > coveringMatches[i]
								|| (containerMatch == coveringMatches[i] && shapeComparator.compare(candidate, covering[i]) < 0))) {
							covering[i] = candidate;
							coveringMatches[i] = containerMatch;
						}
					}
					insert(best, candidate);
					if(best.size() > maxLayouts) {
						best.remove(best.size() - 1);
					}
				}
			}
		}
		return select(best, covering, maxLayouts);
	}

	/**
	 * Each container's own best layout, then the best ranked layouts up to {@code maxLayouts}.
	 * The per-container layouts are kept even if there are more of them than {@code maxLayouts}.
	 */
	protected List<VirtualBoxLayout> select(List<Grid> best, Grid[] covering, int maxLayouts) {
		List<Grid> result = new ArrayList<>(maxLayouts);
		for(Grid grid : covering) {
			if(grid != null && !containsBounds(result, grid.bounds)) {
				insert(result, grid);
			}
		}
		for(Grid grid : best) {
			if(result.size() >= maxLayouts) {
				break;
			}
			if(!containsBounds(result, grid.bounds)) {
				insert(result, grid);
			}
		}
		return materialize(result);
	}

	protected void insert(List<Grid> sorted, Grid grid) {
		int position = sorted.size();
		while(position > 0 && comparator.compare(grid, sorted.get(position - 1)) < 0) {
			position--;
		}
		sorted.add(position, grid);
	}

	protected static boolean containsBounds(List<Grid> grids, VirtualBoxBounds bounds) {
		for(Grid grid : grids) {
			if(grid.bounds.equals(bounds)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Split a count into grid blocks sized to a container, for counts which do not form one
	 * fitting grid. Containers are assumed to be available in unlimited numbers.
	 *
	 * @see #partition(BoxItem, int, List, int, int)
	 */
	public int[] partition(BoxItem item, int count, List<Container> containers, int maxGridBoxes) {
		int[] available = new int[containers.size()];
		Arrays.fill(available, Integer.MAX_VALUE);
		return partition(item, count, containers, available, Integer.MAX_VALUE, false, maxGridBoxes);
	}

	/**
	 * Split a count into grid blocks sized to a container, for counts which do not form one
	 * fitting grid. For each container and orientation, the largest block is the number of
	 * whole columns, rows and layers which fit, limited by weight, internal loads and
	 * {@code maxGridBoxes}. The count is then decomposed like a loaded container:
	 *
	 * <pre>
	 *   full blocks x q  +  s full layers  +  u full rows  +  w boxes in a line
	 * </pre>
	 *
	 * Every block is a filled grid which fits that container; equal full blocks can be handed
	 * to a delegate as one counted item. Decompositions are preferred when the available
	 * containers (item counts, at most {@code maxContainerCount}) can hold every full block
	 * and the remainder. If any container has a cost calculator, blocks which fit more
	 * container types are preferred next, so that cheaper containers remain usable. Then the
	 * fewest blocks and the fewest distinct block sizes win. Blocks are returned in descending
	 * order; a block of one is an ordinary box. Returns an empty array if no orientation fits
	 * two boxes.
	 */
	public int[] partition(BoxItem item, int count, List<ContainerItem> containerItems, int maxContainerCount, int maxGridBoxes) {
		List<Container> containers = new ArrayList<>(containerItems.size());
		int[] available = new int[containerItems.size()];
		boolean costs = false;
		for(int i = 0; i < containerItems.size(); i++) {
			ContainerItem containerItem = containerItems.get(i);
			containers.add(containerItem.getContainer());
			available[i] = containerItem.getCount();
			costs |= containerItem.hasCostCalculator();
		}
		return partition(item, count, containers, available, maxContainerCount, costs, maxGridBoxes);
	}

	protected int[] partition(BoxItem item, int count, List<Container> containers, int[] available, int maxContainerCount, boolean costs, int maxGridBoxes) {
		if(count <= 0 || count > item.getCount() || maxGridBoxes <= 0) {
			throw new IllegalArgumentException("Expected positive count and grid limit");
		}
		long totalAvailable = 0;
		for(int i = 0; i < available.length; i++) {
			totalAvailable += available[i];
		}
		totalAvailable = Math.min(totalAvailable, maxContainerCount);
		int boxWeight = item.getBox().getWeight();
		boolean found = false;
		boolean bestFeasible = false;
		int bestExcluded = 0;
		long bestBlocks = 0;
		int bestDistinct = 0;
		int bestFull = 0, bestLayer = 0, bestRow = 0;
		for(BoxStackValue value : item.getBox().getStackValues()) {
			validate(item, value, 1);
			for(Container container : containers) {
				long nx = container.getLoadDx() / value.getDx();
				long ny = container.getLoadDy() / value.getDy();
				long nz = container.getLoadDz() / value.getDz();
				if(nx == 0 || ny == 0 || nz == 0) {
					// this orientation does not fit the container at all
					continue;
				}
				long weightLimit = boxWeight <= 0 ? Long.MAX_VALUE : container.getMaxLoadWeight() / boxWeight;
				long limit = Math.min(Math.min(weightLimit, maxGridBoxes), count);
				if(limit < 2) {
					continue;
				}
				// whole rows before partial rows, whole layers before partial layers
				long ex = Math.min(nx, limit);
				long ey = ex == nx ? Math.min(ny, limit / ex) : 1;
				long ez = ex == nx && ey == ny ? Math.min(nz, limit / (ex * ey)) : 1;
				while(ez > 1 && !fitsLoad(value, (int) ez)) {
					ez--;
				}
				long full = ex * ey * ez;
				if(full < 2) {
					continue;
				}
				long layer = ex * ey;
				long q = count / full;
				long remainder = count % full;
				long s = remainder / layer;
				long r = remainder % layer;
				long u = r / ex;
				long w = r % ex;
				int distinct = (q > 0 ? 1 : 0) + (s > 0 ? 1 : 0) + (u > 0 ? 1 : 0) + (w > 0 ? 1 : 0);
				long blocks = q + distinct - (q > 0 ? 1 : 0);

				// containers which can hold a full block
				long fullDx = ex * value.getDx(), fullDy = ey * value.getDy(), fullDz = ez * value.getDz();
				long fullWeight = full * boxWeight;
				long fitting = 0;
				int excluded = 0;
				for(int i = 0; i < containers.size(); i++) {
					Container candidate = containers.get(i);
					if(fullDx <= candidate.getLoadDx() && fullDy <= candidate.getLoadDy() && fullDz <= candidate.getLoadDz()
							&& fullWeight <= candidate.getMaxLoadWeight()) {
						fitting += available[i];
					} else if(available[i] > 0) {
						excluded++;
					}
				}
				boolean feasible = q <= Math.min(fitting, totalAvailable) && q + (remainder > 0 ? 1 : 0) <= totalAvailable;

				boolean better;
				if(!found) {
					better = true;
				} else if(feasible != bestFeasible) {
					better = feasible;
				} else if(costs && excluded != bestExcluded) {
					better = excluded < bestExcluded;
				} else if(blocks != bestBlocks) {
					better = blocks < bestBlocks;
				} else {
					better = distinct < bestDistinct;
				}
				if(better) {
					found = true;
					bestFeasible = feasible;
					bestExcluded = excluded;
					bestBlocks = blocks;
					bestDistinct = distinct;
					bestFull = (int) full;
					bestLayer = (int) layer;
					bestRow = (int) ex;
				}
			}
		}
		if(!found) {
			return NO_BLOCKS;
		}
		int[] result = new int[(int) bestBlocks];
		int offset = 0;
		int remaining = count;
		while(remaining >= bestFull) {
			result[offset++] = bestFull;
			remaining -= bestFull;
		}
		if(remaining >= bestLayer) {
			result[offset++] = remaining / bestLayer * bestLayer;
			remaining -= result[offset - 1];
		}
		if(remaining >= bestRow) {
			result[offset++] = remaining / bestRow * bestRow;
			remaining -= result[offset - 1];
		}
		if(remaining > 0) {
			result[offset++] = remaining;
		}
		return result;
	}

	protected static void validate(BoxItem item, BoxStackValue value, int count) {
		VirtualBoxBounds.validateDimensions(value.getDx(), value.getDy(), value.getDz());
		if(value.getVolume() <= 0 || value.getVolume() != item.getBox().getVolume()
				|| value.getVolume() > Long.MAX_VALUE / count) {
			throw new IllegalArgumentException("Expected positive, equal-volume orientations");
		}
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

	protected static List<VirtualBoxLayout> materialize(List<Grid> grids) {
		List<VirtualBoxLayout> result = new ArrayList<>(grids.size());
		for(Grid grid : grids) {
			result.add(new GridLayout(grid));
		}
		return result;
	}
}
