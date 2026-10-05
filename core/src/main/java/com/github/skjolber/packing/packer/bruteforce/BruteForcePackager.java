package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.eclipse.collections.api.iterator.IntIterator;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.FilteredReversedBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager.FastBruteForceBoxStackValuePointComparator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public class BruteForcePackager extends AbstractBruteForcePackager {

	protected final BruteForcePointIteratorFilter pointFilter;
	
	// assumes packaging of a reversed permutation is the same as the permutation. Not recommended if using a pointFilter.
	protected final boolean filterReversePermutations;

	@FunctionalInterface
	public interface BruteForcePointIteratorFilter {

		/**
		 * Get points for the stack value.Intended to guide the brute force search for a solution.
		 *
		 * Implementations must return only indexes to points which can hold the target {@linkplain BoxStackValue}.
		 *
		 * @param points the available points
		 * @param stackValue the value to place
		 * @return an iterator over point indexes
		 */
		IntIterator getPoints(DefaultPointCalculator3D points, BoxStackValue stackValue);
	}

	@FunctionalInterface
	public interface BruteForcePointLimit {

		/**
		 * Return the number of promising fitting points to explore for this
		 * placement step. Returning zero prunes the step.
		 */
		int getPointLimit(DefaultPointCalculator3D points, BoxStackValue stackValue);
	}

	/** A point limit which always returns the same positive number. */
	public static record FixedPointLimit(int pointLimit) implements BruteForcePointLimit {

		public FixedPointLimit {
			if(pointLimit < 1) {
				throw new IllegalArgumentException("Expected a positive point limit");
			}
		}

		@Override
		public int getPointLimit(DefaultPointCalculator3D points, BoxStackValue stackValue) {
			return pointLimit;
		}
	}

	/** A point limit which retains every current point. */
	public static class AllPointLimit implements BruteForcePointLimit {

		@Override
		public int getPointLimit(DefaultPointCalculator3D points, BoxStackValue stackValue) {
			return points.size();
		}
	}

	public static class DefaultPointFilter implements BruteForcePointIteratorFilter {

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
			return new IntIterator() {
				private int index;
				private int next = -1;

				@Override
				public boolean hasNext() {
					while(next == -1 && index < pointCalculator.size()) {
						int candidate = index++;
						if(pointCalculator.get(candidate).fits3D(stackValue)) {
							next = candidate;
						}
					}
					return next != -1;
				}

				@Override
				public int next() {
					if(!hasNext()) {
						return -1;
					}
					int result = next;
					next = -1;
					return result;
				}
			};
		}
	}

	/**
	 * Returns fitting points ordered by how closely their available volume and,
	 * then, footprint area match the box being placed. This is a search-order
	 * heuristic only: it does not discard any fitting points.
	 */
	public static class ClosestVolumeAndAreaPointFilter implements BruteForcePointIteratorFilter {

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
			int[] indexes = new int[pointCalculator.size()];
			int count = 0;
			for(int i = 0; i < pointCalculator.size(); i++) {
				if(pointCalculator.get(i).fits3D(stackValue)) {
					indexes[count++] = i;
				}
			}

			sort(pointCalculator, stackValue, indexes, 0, count - 1);
			int resultCount = count;
			return new IntIterator() {
				private int offset;

				@Override
				public boolean hasNext() {
					return offset < resultCount;
				}

				@Override
				public int next() {
					return hasNext() ? indexes[offset++] : -1;
				}
			};
		}

		private void sort(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue, int[] indexes, int low, int high) {
			while(low < high) {
				int pivot = indexes[(low + high) >>> 1];
				int left = low;
				int right = high;
				while(left <= right) {
					while(compare(pointCalculator, stackValue, indexes[left], pivot) < 0) {
						left++;
					}
					while(compare(pointCalculator, stackValue, indexes[right], pivot) > 0) {
						right--;
					}
					if(left <= right) {
						int value = indexes[left];
						indexes[left++] = indexes[right];
						indexes[right--] = value;
					}
				}
				if(right - low < high - left) {
					if(low < right) {
						sort(pointCalculator, stackValue, indexes, low, right);
					}
					low = left;
				} else {
					if(left < high) {
						sort(pointCalculator, stackValue, indexes, left, high);
					}
					high = right;
				}
			}
		}

		private int compare(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue, int leftIndex, int rightIndex) {
			Point left = pointCalculator.get(leftIndex);
			Point right = pointCalculator.get(rightIndex);

			int result = Long.compare(distance(left.getVolume(), stackValue.getVolume()), distance(right.getVolume(), stackValue.getVolume()));
			if(result != 0) {
				return result;
			}
			result = Long.compare(distance(left.getArea(), stackValue.getArea()), distance(right.getArea(), stackValue.getArea()));
			if(result != 0) {
				return result;
			}
			return Integer.compare(leftIndex, rightIndex);
		}

		private long distance(long left, long right) {
			return left >= right ? left - right : right - left;
		}
	}

	/**
	 * Selects and orders the best fitting point indexes without changing the
	 * point calculator. A supplied candidate filter is applied before ranking.
	 */
	public static class MostPromisingPointFilter implements BruteForcePointIteratorFilter {

		private static final IntIterator EMPTY = new IntIterator() {
			@Override
			public boolean hasNext() {
				return false;
			}

			@Override
			public int next() {
				return -1;
			}
		};

		private final BruteForcePointLimit pointLimit;
		private final FastBruteForceBoxStackValuePointComparator pointComparator;
		private final BruteForcePointIteratorFilter candidateFilter;

		/** Select fitting points using the standard fast brute-force ranking. */
		public MostPromisingPointFilter(BruteForcePointLimit pointLimit) {
			this(pointLimit, FastBruteForcePackager.DEFAULT_POINT_COMPARATOR, null);
		}

		public MostPromisingPointFilter(int pointLimit, FastBruteForceBoxStackValuePointComparator pointComparator) {
			this(new FixedPointLimit(pointLimit), pointComparator, null);
		}

		/**
		 * Select fitting points using the standard fast brute-force ranking after
		 * applying the supplied candidate filter.
		 */
		public MostPromisingPointFilter(BruteForcePointLimit pointLimit, BruteForcePointIteratorFilter candidateFilter) {
			this(pointLimit, FastBruteForcePackager.DEFAULT_POINT_COMPARATOR, candidateFilter);
		}

		public MostPromisingPointFilter(BruteForcePointLimit pointLimit, FastBruteForceBoxStackValuePointComparator pointComparator,
				BruteForcePointIteratorFilter candidateFilter) {
			this.pointLimit = Objects.requireNonNull(pointLimit);
			this.pointComparator = Objects.requireNonNull(pointComparator);
			this.candidateFilter = candidateFilter;
		}

		@Override
		public IntIterator getPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue) {
			int pointLimit = this.pointLimit.getPointLimit(pointCalculator, stackValue);
			if(pointLimit <= 0) {
				return EMPTY;
			}
			if(pointLimit >= pointCalculator.size()) {
				return candidateFilter != null ? candidateFilter.getPoints(pointCalculator, stackValue)
						: DEFAULT_POINT_FILTER.getPoints(pointCalculator, stackValue);
			}

			int[] indexes = new int[pointLimit];
			int count = candidateFilter == null
					? selectAllFittingPoints(pointCalculator, stackValue, indexes, pointLimit)
					: selectFilteredPoints(pointCalculator, stackValue, indexes, pointLimit);
			return new SelectedPointIterator(indexes, count);
		}

		private int selectAllFittingPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue, int[] indexes, int pointLimit) {
			int count = 0;
			for(int i = 0; i < pointCalculator.size(); i++) {
				if(pointCalculator.get(i).fits3D(stackValue)) {
					count = insert(pointCalculator, stackValue, indexes, count, i, pointLimit);
				}
			}
			return count;
		}

		private int selectFilteredPoints(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue, int[] indexes, int pointLimit) {
			int count = 0;
			IntIterator iterator = candidateFilter.getPoints(pointCalculator, stackValue);
			while(iterator.hasNext()) {
				int index = iterator.next();
				if(index >= 0 && index < pointCalculator.size() && pointCalculator.get(index).fits3D(stackValue)) {
					count = insert(pointCalculator, stackValue, indexes, count, index, pointLimit);
				}
			}
			return count;
		}

		private int insert(DefaultPointCalculator3D pointCalculator, BoxStackValue stackValue, int[] indexes, int count, int candidateIndex, int pointLimit) {
			for(int i = 0; i < count; i++) {
				if(indexes[i] == candidateIndex) {
					return count;
				}
			}

			Point candidate = pointCalculator.get(candidateIndex);
			int insertionIndex = count;
			while(insertionIndex > 0 && pointComparator.compare(stackValue, pointCalculator.get(indexes[insertionIndex - 1]), candidate) > 0) {
				insertionIndex--;
			}
			if(insertionIndex == pointLimit) {
				return count;
			}

			int newCount = Math.min(count + 1, pointLimit);
			int moveCount = newCount - insertionIndex - 1;
			if(moveCount > 0) {
				System.arraycopy(indexes, insertionIndex, indexes, insertionIndex + 1, moveCount);
			}
			indexes[insertionIndex] = candidateIndex;
			return newCount;
		}
	}

	private static class SelectedPointIterator implements IntIterator {

		private final int[] indexes;
		private final int count;
		private int offset;

		private SelectedPointIterator(int[] indexes, int count) {
			this.indexes = indexes;
			this.count = count;
		}

		@Override
		public boolean hasNext() {
			return offset < count;
		}

		@Override
		public int next() {
			return hasNext() ? indexes[offset++] : -1;
		}
	}

	protected static final BruteForcePointIteratorFilter DEFAULT_POINT_FILTER = new DefaultPointFilter();

	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}

	public static class BruteForcePackagerBuilder {

		protected Comparator<IntermediatePackagerResult> comparator;
		protected BruteForcePointIteratorFilter pointFilter;
		protected boolean filterReversePermutations = false;
		protected ContainerStrategyFactory containerStrategyFactory;
		
		public BruteForcePackagerBuilder withComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}
		
		/**
		 * Set the factory which selects the container strategy: which containers to use, and in which order.
		 * By default, cost-aware packing is used when the containers have costs, otherwise the first container
		 * (in preference order) which holds the boxes.
		 *
		 * @param factory container strategy factory
		 * @return this builder
		 */
		public BruteForcePackagerBuilder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public BruteForcePackagerBuilder withPointFilter(BruteForcePointIteratorFilter pointFilter) {
			this.pointFilter = pointFilter;
			return this;
		}

		public BruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			BruteForcePackager packager = new BruteForcePackager(comparator, pointFilter, filterReversePermutations);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			return packager;
		}
		
		public BruteForcePackagerBuilder withSkipReversePermutations(boolean filterReversePermutations) {
			this.filterReversePermutations = filterReversePermutations;
			return this;
		}
	}
	
	private class BruteForceSession extends AbstractSingleThreadedBruteForceBoxItemSession {

		protected final PointCalculator3DStack pointCalculator;
		
		public BruteForceSession(List<BoxItem> boxItems, List<ContainerItem> containers,
				int containerCount, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, containers, containerCount, containerIterators, interrupt, BruteForcePackager.this.supportsLoad());
			
			this.pointCalculator =  new PointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.reset(1, 1, 1);
		}

		private BruteForceSession(BruteForceSession source) {
			super(source, BruteForcePackager.this.supportsLoad());
			this.pointCalculator = new PointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.reset(1, 1, 1);
		}

		@Override
		protected BruteForceSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemSession(copyInitialBoxItems(), containers, containerCount, interrupt);
		}

		@Override
		public BruteForceSession fork() {
			return new BruteForceSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			BoxItemPermutationRotationIterator iterator = containerIterators[i];
			// a previous attempt left the iterator at its last permutation and rotations
			iterator.reset();
			
			if(filterReversePermutations && abortOnAnyBoxTooBig && !isOrdered()) {
				iterator = new FilteredReversedBoxItemPermutationRotationIterator(iterator);
			}
			if(order != Order.NONE) {
				return packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, iterator, interrupt, pointFilter, best, getLimit(containerIterators[i]));
			}
			return BruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, iterator, interrupt, pointFilter, best, getLimit(containerIterators[i]));
		}
		
	}
	
	private class BruteForceGroupSession extends AbstractSingleThreadedBruteForceBoxItemGroupSession {

		protected final PointCalculator3DStack pointCalculator;

		public BruteForceGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, 
				List<ContainerItem> containers, int containerCount,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, containers, containerCount, containerIterators, interrupt, BruteForcePackager.this.supportsLoad());
			
			this.pointCalculator =  new PointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.reset(1, 1, 1);
		}

		private BruteForceGroupSession(BruteForceGroupSession source) {
			super(source, BruteForcePackager.this.supportsLoad());
			this.pointCalculator = new PointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.reset(1, 1, 1);
		}

		@Override
		protected BruteForceGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), containers, containerCount, interrupt);
		}

		@Override
		public BruteForceGroupSession fork() {
			return new BruteForceGroupSession(this);
		}

		
		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			BoxItemGroup[] iteratorGroups = containerIterators[i].getBoxItemGroups();
			if(!canLoadNextGroup(iteratorGroups)) {
				return null;
			}
			BoxItemPermutationRotationIterator iterator = containerIterators[i];
			// a previous attempt left the iterator at its last permutation and rotations
			iterator.reset();
			
			if(filterReversePermutations && abortOnAnyBoxTooBig && order == Order.NONE) {
				iterator = new FilteredReversedBoxItemPermutationRotationIterator(iterator);
			}
			if(order != Order.NONE) {
				return truncateToGroup(packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, iterator, interrupt, pointFilter, best, Integer.MAX_VALUE), iteratorGroups);
			}
			return truncateToGroup(BruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, iterator, interrupt, pointFilter, best), iteratorGroups);
		}

	}

	public BruteForcePackager(Comparator<IntermediatePackagerResult> comparator, boolean filterReversePermutations) {
		this(comparator, null, filterReversePermutations);
	}

	public BruteForcePackager(Comparator<IntermediatePackagerResult> comparator, BruteForcePointIteratorFilter pointFilter, boolean filterReversePermutations) {
		super(comparator);
		this.pointFilter = pointFilter;
		this.filterReversePermutations = filterReversePermutations;
	}

	@Override
	protected BruteForceGroupSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt) {
		DefaultBoxItemGroupPermutationRotationIterator[] containerIterators = new DefaultBoxItemGroupPermutationRotationIterator[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			ContainerItem containerItem = containers.get(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemGroupPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItemGroups(itemGroups)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		List<BoxItem> boxItems = new ArrayList<>();
		for (BoxItemGroup boxItemGroup : itemGroups) {
			boxItems.addAll(boxItemGroup.getItems());
		}
		return new BruteForceGroupSession(boxItems, itemGroups, containers, containerCount, containerIterators, interrupt);
	}

	@Override
	protected BruteForceSession createBoxItemSession(List<BoxItem> boxItems, List<ContainerItem> containers,
			int containerCount, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			ContainerItem containerItem = containers.get(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		return new BruteForceSession(boxItems, containers, containerCount, containerIterators, interrupt);
	}

	@Override
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		return null;
	}

}
