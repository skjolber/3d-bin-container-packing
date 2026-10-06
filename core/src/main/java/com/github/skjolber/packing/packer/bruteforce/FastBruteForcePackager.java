package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.ContainerStrategyFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.util.LoadPlacementUtility;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement.
 * So it does not try all possible placements (as i not all points)-
 * <br>
 * <br>
 * Thread-safe implementation. Packing works on copies of the input boxes and containers; it only assigns global indexes
 * to box items which have none (see {@code BoxItem.getGlobalIndex()}), so assign them before packing the same box items concurrently.
 */

public class FastBruteForcePackager extends AbstractBruteForcePackager {

	@FunctionalInterface
	public interface FastBruteForceBoxStackValuePointComparator {

		int compare(BoxStackValue stackValue, Point bestPoint, Point candidatePoint);
		
	}

	public static class DefaultFastBruteForceBoxStackValuePointComparator implements FastBruteForceBoxStackValuePointComparator {

		@Override
		public int compare(BoxStackValue stackValue, Point bestPoint, Point candidatePoint) {
			if(bestPoint.getArea() < candidatePoint.getArea()) {
				return -1;
			}
			if(bestPoint.getMinZ() < candidatePoint.getMinZ()) {
				return -1;
			}
			if(bestPoint.getArea() == candidatePoint.getArea() && bestPoint.getVolume() < candidatePoint.getVolume()) {
				return -1;
			}
			return 1;
		}
	}

	protected static final FastBruteForceBoxStackValuePointComparator DEFAULT_POINT_COMPARATOR = new DefaultFastBruteForceBoxStackValuePointComparator();

	public static FastBruteForcePackagerBuilder newBuilder() {
		return new FastBruteForcePackagerBuilder();
	}

	public static class FastBruteForcePackagerBuilder {

		protected Comparator<IntermediatePackagerResult> comparator;
		protected FastBruteForceBoxStackValuePointComparator pointComparator = DEFAULT_POINT_COMPARATOR;
		protected ContainerStrategyFactory containerStrategyFactory;
		
		public FastBruteForcePackagerBuilder withComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}

		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected int groupOrderSearch;

		/**
		 * Also search the orders of the box item groups, when there are at most this many groups left: groups are
		 * packed in order, and another order can fill a container better. The search is exponential in the number of
		 * groups (for example 120 orders for 5 groups). Not used with a box item order.
		 *
		 * @param maxGroups the maximum number of remaining groups for which to search their orders, or 0 for never
		 * @return this builder
		 */
		public FastBruteForcePackagerBuilder withGroupOrderSearch(int maxGroups) {
			if(maxGroups < 0) {
				throw new IllegalArgumentException("Expected a non-negative number of groups, got " + maxGroups);
			}
			this.groupOrderSearch = maxGroups;
			return this;
		}

		/**
		 * Set the comparator which picks the order of box item groups with the same container priority and extraction
		 * order (by default the largest group first, like the plain packager).
		 *
		 * @param comparator box item group comparator
		 * @return this builder
		 */
		public FastBruteForcePackagerBuilder withBoxItemGroupComparator(Comparator<BoxItemGroup> comparator) {
			this.boxItemGroupComparator = comparator;
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
		public FastBruteForcePackagerBuilder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public FastBruteForcePackagerBuilder withPointComparator(FastBruteForceBoxStackValuePointComparator pointComparator) {
			this.pointComparator = pointComparator;
			return this;
		}

		public FastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new BruteForceIntermediatePackagerResultComparator();
			}
			
			FastBruteForcePackager packager = new FastBruteForcePackager(comparator, pointComparator);
			if(containerStrategyFactory != null) {
				packager.setContainerStrategyFactory(containerStrategyFactory);
			}
			if(boxItemGroupComparator != null) {
				packager.setBoxItemGroupComparator(boxItemGroupComparator);
			}
			packager.setGroupOrderSearch(groupOrderSearch);
			return packager;
		}
		
	}
	
	private class FastBruteForceSession extends AbstractSingleThreadedBruteForceBoxItemSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceSession(List<BoxItem> boxItems, List<ContainerItem> containers,
				int containerCount, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, containers, containerCount, containerIterators, interrupt, hasLoadLimits(boxItems));
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceSession(FastBruteForceSession source) {
			super(source, source.load);
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		@Override
		protected FastBruteForceSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemSession(copyInitialBoxItems(), containers, containerCount, interrupt);
		}

		@Override
		public FastBruteForceSession fork() {
			return new FastBruteForceSession(this);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			containerIterators[i].reset();
			if(order != Order.NONE) {
				return packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, getLimit(containerIterators[i]));
			}
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, getLimit(containerIterators[i]));
		}
		
	}
	
	private class FastBruteForceGroupSession extends AbstractSingleThreadedBruteForceBoxItemGroupSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containers, int containerCount,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, containers, containerCount, containerIterators, interrupt, hasLoadLimits(boxItems));
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceGroupSession(FastBruteForceGroupSession source) {
			super(source, source.load);
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		@Override
		protected FastBruteForceGroupSession fresh(List<ContainerItem> containers, int containerCount) {
			return createBoxItemGroupSession(copyBoxItemGroups(initialBoxItemGroups), containers, containerCount, interrupt);
		}

		@Override
		public FastBruteForceGroupSession fork() {
			return new FastBruteForceGroupSession(this);
		}

		
		@Override
		protected BruteForceIntermediatePackagerResult packGroupOrder(int containerIndex, BoxItemPermutationRotationIterator iterator, IntermediatePackagerResult best) throws PackagerInterruptedException {
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(containerIndex), containerIndex, iterator, interrupt, fastPointComparator, best);
		}

		@Override
		protected Comparator<IntermediatePackagerResult> getIntermediatePackagerResultComparator() {
			return intermediatePackagerResultComparator;
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			if(isGroupOrderSearch()) {
				return attemptGroupOrders(i, best);
			}
			BoxItemGroup[] iteratorGroups = containerIterators[i].getBoxItemGroups();
			if(!canLoadNextGroup(iteratorGroups)) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			containerIterators[i].reset();
			if(order != Order.NONE) {
				return truncateToGroup(packInOrder(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best, Integer.MAX_VALUE), iteratorGroups);
			}
			return truncateToGroup(FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best), iteratorGroups);
		}
		
	}

	@Override
	protected FastBruteForceGroupSession createBoxItemGroupSession(List<BoxItemGroup> itemGroups,
			List<ContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt) {
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
		return new FastBruteForceGroupSession(boxItems, itemGroups, containers, containerCount, containerIterators, interrupt);
	}

	@Override
	protected FastBruteForceSession createBoxItemSession(List<BoxItem> boxItems, List<ContainerItem> containers,
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
		
		return new FastBruteForceSession(boxItems, containers, containerCount, containerIterators, interrupt);
	}

	protected final FastBruteForceBoxStackValuePointComparator fastPointComparator;

	public FastBruteForcePackager(Comparator<IntermediatePackagerResult> comparator, FastBruteForceBoxStackValuePointComparator pointComparator) {
		super(comparator);
		this.fastPointComparator = pointComparator;
	}

	protected void clearStack(Stack stack, LoadPlacementUtility loadPlacementUtility) {
		if(loadPlacementUtility != null) {
			setStackSize(stack, 0, loadPlacementUtility);
		} else {
			stack.clear();
		}
	}

	/**
	 * Remove the placements above a size from the stack, and with load limits, their loads from the boxes below.
	 */
	protected void setStackSize(Stack stack, int size, LoadPlacementUtility loadPlacementUtility) {
		if(loadPlacementUtility != null) {
			for(int i = stack.size() - 1; i >= size; i--) {
				Placement placement = stack.getPlacements().get(i);
				placement.removeSupporteesAbove();
				for(PlacementLoad supporter : placement.getSupporters()) {
					supporter.getPlacement().removeLastSupportee();
				}
				placement.clearLoad();
			}
		}
		stack.setSize(size);
	}

	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, pointComparator, null);
	}

	/**
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 */
	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator, IntermediatePackagerResult best) throws PackagerInterruptedException {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, pointComparator, best, Integer.MAX_VALUE);
	}

	/**
	 * @param limit the number of leading boxes of the permutations which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 */
	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator, IntermediatePackagerResult best,
			int limit) throws PackagerInterruptedException {
		
		Container holder = containerItem.getContainer().copy(iterator.length());
		
		Stack stack = holder.getStack();
		
		// with box load limits, the placement tracks the loads of the placed boxes
		LoadPlacementUtility loadPlacementUtility = createLoadPlacementUtility(iterator, stack);

		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);

		long[] freeLoadWeights = calculateFreeLoadWeights(holder, iterator);
		if(loadPlacementUtility != null) {
			loadPlacementUtility.initialize(iterator.length());
		}
		// box item groups: the boxes of earlier groups; the permutations keep the groups' positions
		int[] insertAfterCounts = getInsertAfterCounts(iterator);
		
		if(limit == 0) {
			return bestResult;
		}
		boolean allItemsFit = limit >= iterator.length() && canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}

		// iterator over all permutations
		permutations: 
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// iterate over all rotations

			bestPermutationResult.reset();
			int maxPackableCount = allItemsFit ? iterator.length() : Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
			int size;
			if(!allItemsFit && prefersHigherLoadVolume && getLoadVolume(iterator, maxPackableCount) < Math.max(minLoadVolume, bestResult.getLoadVolume())) {
				// no rotation of this permutation can load more than the best result
				size = maxPackableCount;
			} else {
				pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
				if(containerItem.hasInitialPoints()) {
					pointCalculator.setPoints(containerItem.getInitialPoints());
					pointCalculator.clear();
				}
			
				int index = 0;

				do {
					// attempt to limit the number of points created
					// by calculating the minimum point volume and area
					int minStackableAreaIndex = iterator.getMinStackableAreaIndex(index);
					long minStackableVolume = iterator.getMinBoxVolume(index);

					pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), minStackableVolume);

					int count = packStackPlacement(pointCalculator, stackPlacements, iterator, stack, holder, index, interrupt,
							minStackableAreaIndex, freeLoadWeights[index], loadPlacementUtility, pointComparator, maxPackableCount, insertAfterCounts);
					if(count == Integer.MIN_VALUE) {
						throw new PackagerInterruptedException();
					}

					// continue search, but see if this is the best fit so far
					// higher count implies higher volume and weight
					// since the items are the same within each permutation
					if(count > bestPermutationResult.getSize()) {
						bestPermutationResult.setState(pointCalculator.getPoints(), iterator.getState(), stackPlacements, stackPlacementCount);
						if(count == iterator.length()) {
							return bestPermutationResult;
						}
					}
					if(count >= maxPackableCount) {
						// Rotations cannot change box volume or weight, so this is the
						// longest feasible prefix for the current permutation.
						clearStack(stack, loadPlacementUtility);
						break;
					}

					// search for the next rotation which actually 
					// has a chance of affecting the result.
					// i.e. if we have four boxes, and two boxes could be placed with the 
					// current rotations, and the new rotation only changes the rotation of box 4,
					// then we know that attempting to stack again will not work since box
					// 3 will still remain in the same rotation (which could not be placed)

					int rotationIndex = iterator.nextRotation(count);

					if(rotationIndex == -1) {
						// no more rotations, continue to next permutation
						clearStack(stack, loadPlacementUtility);
						break;
					}

					pointCalculator.setStackSize(rotationIndex);
					setStackSize(stack, rotationIndex, loadPlacementUtility);

					index = rotationIndex;
				} while (true);

				if(!bestPermutationResult.isEmpty()) {
					// compare against other permutation's result

					if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) < 0) {
						// switch the two results for one another
						BruteForceIntermediatePackagerResult tmp = bestResult;
						bestResult = bestPermutationResult;
						bestPermutationResult = tmp;
					}
				}
				size = bestPermutationResult.getSize();
			}

			// get the next permutation
			// make sure there is actually free weight available
			// at the next index
			do {
				int permutationIndex = iterator.nextPermutation(size);
	
				if(permutationIndex == -1) {
					break permutations;
				}
				
				calculateFreeLoadWeights(iterator, freeLoadWeights, permutationIndex);
				
				if(freeLoadWeights[permutationIndex] > 0) {
					break;
				}
				size--;
			} while(true);
			
			
		} while (true);

		return bestResult;
	}

	/**
	 * Pack the boxes in the iterator's order (a box item order) into one container: there is a single permutation, so
	 * only the rotations are searched, and each box must be insertable after the boxes before it.
	 *
	 * @param best the best result so far, or null
	 * @param limit the number of leading boxes which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getLimit(BoxItemPermutationRotationIterator)})
	 */
	public BruteForceIntermediatePackagerResult packInOrder(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator,
			IntermediatePackagerResult best, int limit) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility loadPlacementUtility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, loadPlacementUtility != null);
		if(limit == 0) {
			return result;
		}
		// the prefix of the order which fits by volume and weight
		int maxPackableCount = Math.min(limit, getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight()));
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getLoadVolume(iterator, maxPackableCount) < minLoadVolume) {
			// cannot load more than the best result
			return result;
		}

		long[] freeLoadWeights = calculateFreeLoadWeights(holder, iterator);
		if(loadPlacementUtility != null) {
			loadPlacementUtility.initialize(iterator.length());
		}
		int[] insertAfterCounts = getInsertAfterAllCounts(iterator.length());

		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.hasInitialPoints()) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		// rotations: from the first box which a rotation change could place differently
		int index = 0;
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			int minStackableAreaIndex = iterator.getMinStackableAreaIndex(index);
			pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(index));

			int count = packStackPlacement(pointCalculator, stackPlacements, iterator, stack, holder, index, interrupt,
					minStackableAreaIndex, freeLoadWeights[index], loadPlacementUtility, pointComparator, maxPackableCount, insertAfterCounts);
			if(count == Integer.MIN_VALUE) {
				throw new PackagerInterruptedException();
			}
			if(count > result.getSize()) {
				result.setState(pointCalculator.getPoints(), iterator.getState(), stackPlacements, stackPlacementCount);
				if(count == iterator.length()) {
					return result;
				}
			}
			if(count >= maxPackableCount) {
				clearStack(stack, loadPlacementUtility);
				break;
			}
			// the next rotation of the boxes up to the first which could not be placed
			int rotationIndex = iterator.nextRotation(count);
			if(rotationIndex == -1) {
				clearStack(stack, loadPlacementUtility);
				break;
			}
			pointCalculator.setStackSize(rotationIndex);
			setStackSize(stack, rotationIndex, loadPlacementUtility);
			index = rotationIndex;
		} while (true);
		return result;
	}

	private void calculateFreeLoadWeights(BoxItemPermutationRotationIterator rotator, long[] freeLoadWeights, int permutationIndex) {
		long nextFreeLoadWeight = freeLoadWeights[permutationIndex] - rotator.getStackValue(permutationIndex).getBox().getWeight();
		for(int i = permutationIndex + 1; i < freeLoadWeights.length; i++) {
			 freeLoadWeights[i] = nextFreeLoadWeight;
			 
			 BoxStackValue value = rotator.getStackValue(i);
			 nextFreeLoadWeight -= value.getBox().getWeight();
		}
	}

	private long[] calculateFreeLoadWeights(Container containerStackValue, BoxItemPermutationRotationIterator rotator) {
		// precalculate load weights per permutations
		long[] freeLoadWeights = new long[rotator.length()];
		long freeLoadWeight = containerStackValue.getMaxLoadWeight();
		for(int i = 0; i < freeLoadWeights.length; i++) {
			 freeLoadWeights[i] = freeLoadWeight;
			 
			 BoxStackValue value = rotator.getStackValue(i);
			 freeLoadWeight -= value.getBox().getWeight();
		}
		return freeLoadWeights;
	}

	public int packStackPlacement(FastPointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad,
			LoadPlacementUtility loadPlacementUtility, FastBruteForceBoxStackValuePointComparator pointComparator) {
		int maxPackableCount = getMaxPackableCount(iterator, container.getMaxLoadVolume(), container.getMaxLoadWeight());
		return packStackPlacement(pointCalculator, placements, iterator, stack, container, placementIndex, interrupt,
				minStackableAreaIndex, freeWeightLoad, loadPlacementUtility, pointComparator, maxPackableCount, getInsertAfterCounts(iterator));
	}

	/**
	 * @param insertAfterCounts for each level, the number of boxes placed before it which it must be insertable after
	 *        (see {@link #getInsertAfterCounts(BoxItemPermutationRotationIterator)} and {@link #getInsertAfterAllCounts(int)})
	 */
	protected int packStackPlacement(FastPointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad,
			LoadPlacementUtility loadPlacementUtility, FastBruteForceBoxStackValuePointComparator pointComparator,
			int maxPackableCount, int[] insertAfterCounts) {
		if(loadPlacementUtility != null) {
			return packStackPlacementWithLoad(pointCalculator, placements, iterator, stack, container, placementIndex, interrupt, minStackableAreaIndex, freeWeightLoad,
					loadPlacementUtility, pointComparator, maxPackableCount, insertAfterCounts);
		}
		boolean checkObstacles = !container.getObstacles().isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		// pack as many items as possible from placementIndex
		while (placementIndex < maxPackableCount) {
			if(interrupt.getAsBoolean()) {
				// might have returned due to deadline
				return Integer.MIN_VALUE;
			}
			BoxStackValue stackValue = iterator.getStackValue(placementIndex);

			Box stackable = stackValue.getBox();
			if(stackable.getWeight() > freeWeightLoad) {
				break;
			}
			Placement placement = placements[placementIndex];

			int bestPointIndex = -1;
			for(int k = 0; k < pointCalculator.size(); k++) {
				SimplePoint3D candidatePoint = pointCalculator.get(k);
				if(!candidatePoint.fits3D(stackValue)) {
					continue;
				}
				if(checkObstacles && !isInsertable(candidatePoint, stackValue, container.getObstacles(), container.getAccess())) {
					continue;
				}
				if(checkExtraction && !isExtractable(candidatePoint, stackValue, stack, container.getAccess())) {
					continue;
				}
				if(insertAfterCounts.length != 0 && insertAfterCounts[placementIndex] > 0 && !isInsertableAfter(candidatePoint, stackValue, stack, insertAfterCounts[placementIndex], container.getAccess())) {
					continue;
				}
				if(bestPointIndex == -1 || pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), candidatePoint) > 0) {
					bestPointIndex = k;
				}
			}

			if(bestPointIndex == -1) { // interrupted
				break;
			}

			SimplePoint3D point3d = pointCalculator.get(bestPointIndex);

			placement.setStackValue(stackValue);
			placement.setPoint(point3d);

			pointCalculator.add(bestPointIndex, placement);

			freeWeightLoad -= stackable.getWeight();

			stack.add(placement);

			placementIndex++;

			if(placementIndex < maxPackableCount) {
				// check whether minimum point volume and area should be adjusted 
				boolean minArea = placementIndex == minStackableAreaIndex;
				if(minArea) {
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);

					pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(placementIndex));
				} else {
					pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(placementIndex));
				}
			}
		}

		return placementIndex;
	}

	/**
	 * As {@link #packStackPlacement(FastPointCalculator3DStack, Placement[], BoxItemPermutationRotationIterator, Stack, Container, int, PackagerInterruptSupplier, int, long, LoadPlacementUtility, FastBruteForceBoxStackValuePointComparator, int, int[])},
	 * for boxes with load limits: each box is placed at the best point where the boxes below can carry it, and the loads
	 * of the placed boxes are tracked.
	 *
	 * @return the index of the first box which was not placed, or Integer.MIN_VALUE if interrupted
	 */
	protected int packStackPlacementWithLoad(FastPointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad,
			LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator,
			int maxPackableCount, int[] insertAfterCounts) {
		boolean checkObstacles = !container.getObstacles().isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		while (placementIndex < maxPackableCount) {
			if(interrupt.getAsBoolean()) {
				return Integer.MIN_VALUE;
			}

			BoxStackValue stackValue = iterator.getStackValue(placementIndex);
			Box box = stackValue.getBox();
			if(box.getWeight() > freeWeightLoad) {
				break;
			}
			int bestPointIndex = -1;
			for(int k = 0; k < pointCalculator.size(); k++) {
				SimplePoint3D point = pointCalculator.get(k);
				if(!point.fits3D(stackValue)) {
					continue;
				}
				if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
					continue;
				}
				if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
					continue;
				}
				if(insertAfterCounts.length != 0 && insertAfterCounts[placementIndex] > 0 && !isInsertableAfter(point, stackValue, stack, insertAfterCounts[placementIndex], container.getAccess())) {
					continue;
				}
				if(bestPointIndex != -1 && pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), point) <= 0) {
					continue;
				}

				utility.populatePointSupporters(point);
				utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
				long supportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);
				if(supportedArea == -1L) {
					continue;
				}

				bestPointIndex = k;
			}

			if(bestPointIndex == -1) {
				break;
			}

			SimplePoint3D point = pointCalculator.get(bestPointIndex);
			// Candidate evaluation leaves the utility primed for the last point checked,
			// so repopulate its caches for the selected point before adding the load.
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			long bestSupportedArea = utility.getSupportedAreaAtPoint(point, stackValue, false);

			Placement placement = placements[placementIndex];
			placement.clearLoad();
			placement.setStackValue(stackValue);
			placement.setPoint(point);
			placement.setIndex(stack.size());
			placement.setSupportedArea(bestSupportedArea);

			pointCalculator.add(bestPointIndex, placement);
			stack.add(placement);
			utility.addSupportersLoad(placement);

			freeWeightLoad -= box.getWeight();
			placementIndex++;

			if(placementIndex < maxPackableCount) {
				if(placementIndex == minStackableAreaIndex) {
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);
					pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(placementIndex));
				} else {
					pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(placementIndex));
				}
			}
		}

		return placementIndex;
	}

	@Override
	protected boolean supportsSkipping() {
		return false;
	}
}
