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
import com.github.skjolber.packing.iterator.PermutationRotationState;
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

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected Comparator<IntermediatePackagerResult> comparator;
		protected FastBruteForceBoxStackValuePointComparator pointComparator = DEFAULT_POINT_COMPARATOR;
		protected ContainerStrategyFactory containerStrategyFactory;
		
		public Builder withIntermediatePackagerResultComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}

		protected Comparator<BoxItemGroup> boxItemGroupComparator;
		protected int groupOrderSearch;
		protected boolean requireFullSupport;

		/**
		 * Place boxes only where they rest completely on the floor or on the boxes below: at the free points, and
		 * shifted from a free point onto the corner of a box below (as the plain packager's full support). Boxes do not
		 * rest on obstacles.
		 *
		 * @param requireFullSupport true to require full support
		 * @return this builder
		 */
		public Builder withRequireFullSupport(boolean requireFullSupport) {
			this.requireFullSupport = requireFullSupport;
			return this;
		}

		/**
		 * Also search the orders of the box item groups, when there are at most this many groups left: groups are
		 * packed in order, and another order can fill a container better. The search is exponential in the number of
		 * groups (for example 120 orders for 5 groups). Not used with a box item order.
		 *
		 * @param maxGroups the maximum number of remaining groups for which to search their orders, or 0 for never
		 * @return this builder
		 */
		public Builder withGroupOrderSearch(int maxGroups) {
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
		public Builder withBoxItemGroupComparator(Comparator<BoxItemGroup> comparator) {
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
		public Builder withContainerStrategyFactory(ContainerStrategyFactory factory) {
			this.containerStrategyFactory = Objects.requireNonNull(factory);
			return this;
		}

		public Builder withPointComparator(FastBruteForceBoxStackValuePointComparator pointComparator) {
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
			packager.setRequireFullSupport(requireFullSupport);
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
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
				return packInOrderSkipping(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator,
						null, getMaxContainerPriority(containerIterators[i]));
			}
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
			// when skipping, the first group may be skipped
			if(order != Order.CHRONOLOGICAL_ALLOW_SKIPPING && !canLoadNextGroup(iteratorGroups)) {
				return null;
			}
			// a previous attempt left the iterator at its last permutation and rotations
			containerIterators[i].reset();
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING) {
				// groups are skipped whole
				return packInOrderSkipping(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator,
						getGroupSkipEnds(iteratorGroups, containerIterators[i].length()), getMaxContainerPriority(containerIterators[i]));
			}
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

	/**
	 * Pack the boxes in the iterator's order (a box item order, {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}) into one
	 * container: any box may be skipped (it waits for a later container), and the boxes which are placed are in the order,
	 * each insertable after the boxes before it. As
	 * {@link AbstractBruteForcePackager#packInOrderSkipping(PointCalculator3DStack, Placement[], int, ContainerItem, int, BoxItemPermutationRotationIterator, PackagerInterruptSupplier, BruteForcePointIteratorFilter, int[], int)},
	 * but the search (see {@link #searchInOrderSkipping}) tries each rotation of a box at its best point only.
	 *
	 * @param skipEnds for each box, the box to continue with when skipping it, or -1 if it cannot be skipped (box item
	 *        groups are skipped whole), or null if each box can be skipped
	 * @param maxContainerPriority the highest container priority which may be placed (see
	 *        {@link AbstractBruteForceBoxItemSession#getMaxContainerPriority(BoxItemPermutationRotationIterator)})
	 * @return the result, or an empty result; the result may hold any of the boxes (see
	 *         {@link BruteForceIntermediatePackagerResult#isAnyRemaining()})
	 */
	public BruteForceIntermediatePackagerResult packInOrderSkipping(FastPointCalculator3DStack pointCalculator, Placement[] stackPlacements, int stackPlacementCount,
			ContainerItem containerItem, int containerIndex, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt,
			FastBruteForceBoxStackValuePointComparator pointComparator, int[] skipEnds, int maxContainerPriority) throws PackagerInterruptedException {
		Container holder = containerItem.getContainer().copy(iterator.length());
		Stack stack = holder.getStack();

		LoadPlacementUtility utility = createLoadPlacementUtility(iterator, stack);
		BruteForceIntermediatePackagerResult result = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, utility != null);
		result.setAnyRemaining(true);
		int length = iterator.length();
		if(length == 0 || stackPlacements.length == 0) {
			return result;
		}

		int[] permutations = iterator.getPermutations();
		BoxItem[] boxItems = iterator.getBoxItems();
		long[] minAreas = getMinAreas(boxItems, permutations);

		pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());
		if(containerItem.hasInitialPoints()) {
			pointCalculator.setPoints(containerItem.getInitialPoints());
			pointCalculator.clear();
		}
		pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[0], iterator.getMinBoxVolume(0));
		if(utility != null) {
			utility.initialize(length);
		}
		SkippingBest best = new SkippingBest(length);
		if(requireFullSupport) {
			searchInOrderSkippingFullSupport(pointCalculator, stackPlacements, boxItems, permutations, minAreas, skipEnds, maxContainerPriority, iterator, stack, holder, interrupt, utility,
					pointComparator, best);
		} else {
			searchInOrderSkipping(pointCalculator, stackPlacements, boxItems, permutations, minAreas, skipEnds, maxContainerPriority, iterator, stack, holder, interrupt, utility,
					pointComparator, best);
		}
		stack.clear();
		if(best.count > 0) {
			int[] placedPermutations = new int[best.count];
			int[] placedRotations = new int[best.count];
			System.arraycopy(best.permutations, 0, placedPermutations, 0, best.count);
			System.arraycopy(best.rotations, 0, placedRotations, 0, best.count);
			result.setState(best.points, new PermutationRotationState(placedRotations, placedPermutations), stackPlacements, stackPlacementCount);
		}
		result.markDirty();
		return result;
	}

	/**
	 * Depth-first search over a box item order where boxes may be skipped. Each level tries each rotation of its box at
	 * the best point (by the point comparator) where the box is insertable after the boxes placed before it; then the
	 * level also tries skipping its box. Levels are boxes, while the point calculator has a frame for each placed box.
	 *
	 * @param skipEnds for each level, the level to continue with when skipping it, -1 if it cannot be skipped, or null
	 *        for the next level
	 * @param maxContainerPriority the highest container priority which may be placed
	 * @param best the best arrangement: the most volume, then the most boxes
	 */
	protected void searchInOrderSkipping(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItem[] boxItems, int[] permutations, long[] minAreas,
			int[] skipEnds, int maxContainerPriority, BoxItemPermutationRotationIterator iterator, Stack stack, Container container, PackagerInterruptSupplier interrupt,
			LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator, SkippingBest best) throws PackagerInterruptedException {
		int length = permutations.length;
		long maxLoadVolume = container.getMaxLoadVolume();

		// for each level: how it continues, how it was reached, and the state when entering it
		boolean[] skipping = new boolean[length + 1];
		int[] rotationIndexes = new int[length];
		int[] parents = new int[length + 1];
		int[] placedCounts = new int[length + 1];
		long[] placedVolumes = new long[length + 1];
		int[] freeLoadWeights = new int[length + 1];
		int[] maxContainerPriorities = new int[length + 1];
		int[] placedPermutations = new int[length];
		int[] placedRotations = new int[length];

		// the volume of the boxes from each level on, as a bound
		long[] remainingVolumes = new long[length + 1];
		for (int i = length - 1; i >= 0; i--) {
			remainingVolumes[i] = remainingVolumes[i + 1] + boxItems[permutations[i]].getBox().getVolume();
		}

		int level = 0;
		parents[0] = -1;
		freeLoadWeights[0] = container.getMaxLoadWeight();
		maxContainerPriorities[0] = maxContainerPriority;
		boolean descend = true;
		while(true) {
			if(descend) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int placedCount = placedCounts[level];
				long placedVolume = placedVolumes[level];
				if(level == length) {
					// every box is placed or skipped: keep the arrangement if the best so far
					if(placedVolume > best.volume || (placedVolume == best.volume && placedCount > best.count)) {
						best.volume = placedVolume;
						best.count = placedCount;
						best.points = pointCalculator.getPoints();
						System.arraycopy(placedPermutations, 0, best.permutations, 0, placedCount);
						System.arraycopy(placedRotations, 0, best.rotations, 0, placedCount);
					}
					level = parents[level];
					descend = false;
					continue;
				}
				if(placedVolume + remainingVolumes[level] < best.volume || (placedVolume + remainingVolumes[level] == best.volume && placedCount + length - level <= best.count)) {
					// cannot beat the best arrangement
					level = parents[level];
					descend = false;
					if(level == -1) {
						return;
					}
					continue;
				}
				BoxItem boxItem = boxItems[permutations[level]];
				Box box = boxItem.getBox();
				skipping[level] = !(boxItem.getContainerPriority() <= maxContainerPriorities[level]
						&& box.getWeight() <= freeLoadWeights[level]
						&& placedVolume + box.getVolume() <= maxLoadVolume);
				rotationIndexes[level] = 0;
			} else if(!skipping[level]) {
				// back from the next level: remove this level's placement and restore the free points
				int placedCount = placedCounts[level];
				if(utility != null) {
					Placement placement = placements[placedCount];
					placement.removeSupporteesAbove();
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				pointCalculator.setStackSize(placedCount);
				if(best.count == length) {
					// the best arrangement places all boxes: unwind without trying more candidates
					level = parents[level];
					if(level == -1) {
						return;
					}
					continue;
				}
			} else {
				// back from skipping this level's box
				level = parents[level];
				if(level == -1) {
					return;
				}
				continue;
			}

			int placedCount = placedCounts[level];
			if(!skipping[level]) {
				// the next rotation of this level's box which fits at a point
				BoxStackValue[] stackValues = boxItems[permutations[level]].getBox().getStackValues();
				BoxStackValue stackValue = null;
				int pointIndex = -1;
				while(pointIndex == -1 && rotationIndexes[level] < stackValues.length) {
					stackValue = stackValues[rotationIndexes[level]];
					rotationIndexes[level]++;
					if(utility == null) {
						pointIndex = getBestPoint(pointCalculator, stackValue, stack, placedCount, container, iterator, pointComparator);
					} else {
						pointIndex = getBestPointWithLoad(pointCalculator, stackValue, stack, placedCount, container, iterator, utility, pointComparator);
					}
				}
				if(pointIndex != -1) {
					// place the box and continue with the next level
					Placement placement = placements[placedCount];
					SimplePoint3D point = pointCalculator.get(pointIndex);
					if(utility != null) {
						// the utility caches are primed for the last point checked: repopulate them for the selected point
						utility.populatePointSupporters(point);
						utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
						placement.clearLoad();
						placement.setIndex(stack.size());
						placement.setSupportedArea(utility.getSupportedAreaAtPoint(point, stackValue, false));
					}
					placement.setStackValue(stackValue);
					placement.setPoint(point);
					pointCalculator.add(pointIndex, placement);
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					int nextLevel = level + 1;
					if(nextLevel < length) {
						if(minAreas[nextLevel] != minAreas[level]) {
							pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[nextLevel], iterator.getMinBoxVolume(nextLevel));
						} else {
							pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
						}
					}
					placedPermutations[placedCount] = permutations[level];
					placedRotations[placedCount] = rotationIndexes[level] - 1;
					enter(nextLevel, level, placedCount + 1, placedVolumes[level] + stackValue.getBox().getVolume(), freeLoadWeights[level] - stackValue.getBox().getWeight(),
							maxContainerPriorities[level], parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
					level = nextLevel;
					descend = true;
					continue;
				}
				// no more rotations: try skipping the box
				skipping[level] = true;
			}

			// skip this level's box: it waits for a later container, and so do the boxes of a higher container priority
			int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
			if(skipEnd == -1) {
				// cannot skip here (inside a box item group)
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}
			int skippedContainerPriority = Math.min(maxContainerPriorities[level], boxItems[permutations[level]].getContainerPriority());
			enter(skipEnd, level, placedCount, placedVolumes[level], freeLoadWeights[level], skippedContainerPriority,
					parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
			level = skipEnd;
			descend = true;
		}
	}

	/**
	 * As {@link #searchInOrderSkipping(FastPointCalculator3DStack, Placement[], BoxItem[], int[], long[], int[], int, BoxItemPermutationRotationIterator, Stack, Container, PackagerInterruptSupplier, LoadPlacementUtility, FastBruteForceBoxStackValuePointComparator, SkippingBest)},
	 * with full support required: each level tries each rotation of its box at the best position (by the point comparator)
	 * where the box rests completely on the floor or on the boxes below (see {@link FullSupportCandidates}); then the level
	 * also tries skipping its box.
	 *
	 * @param skipEnds for each level, the level to continue with when skipping it, -1 if it cannot be skipped, or null
	 *        for the next level
	 * @param maxContainerPriority the highest container priority which may be placed
	 * @param best the best arrangement: the most volume, then the most boxes
	 */
	protected void searchInOrderSkippingFullSupport(FastPointCalculator3DStack pointCalculator, Placement[] placements, BoxItem[] boxItems, int[] permutations, long[] minAreas,
			int[] skipEnds, int maxContainerPriority, BoxItemPermutationRotationIterator iterator, Stack stack, Container container, PackagerInterruptSupplier interrupt,
			LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator, SkippingBest best) throws PackagerInterruptedException {
		int length = permutations.length;
		long maxLoadVolume = container.getMaxLoadVolume();

		// for each level: how it continues, how it was reached, and the state when entering it
		boolean[] skipping = new boolean[length + 1];
		int[] rotationIndexes = new int[length];
		int[] parents = new int[length + 1];
		int[] placedCounts = new int[length + 1];
		long[] placedVolumes = new long[length + 1];
		int[] freeLoadWeights = new int[length + 1];
		int[] maxContainerPriorities = new int[length + 1];
		int[] placedPermutations = new int[length];
		int[] placedRotations = new int[length];

		// the volume of the boxes from each level on, as a bound
		long[] remainingVolumes = new long[length + 1];
		for (int i = length - 1; i >= 0; i--) {
			remainingVolumes[i] = remainingVolumes[i + 1] + boxItems[permutations[i]].getBox().getVolume();
		}

		int level = 0;
		parents[0] = -1;
		freeLoadWeights[0] = container.getMaxLoadWeight();
		maxContainerPriorities[0] = maxContainerPriority;
		boolean descend = true;
		while(true) {
			if(descend) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				int placedCount = placedCounts[level];
				long placedVolume = placedVolumes[level];
				if(level == length) {
					// every box is placed or skipped: keep the arrangement if the best so far
					if(placedVolume > best.volume || (placedVolume == best.volume && placedCount > best.count)) {
						best.volume = placedVolume;
						best.count = placedCount;
						best.points = pointCalculator.getPoints();
						System.arraycopy(placedPermutations, 0, best.permutations, 0, placedCount);
						System.arraycopy(placedRotations, 0, best.rotations, 0, placedCount);
					}
					level = parents[level];
					descend = false;
					continue;
				}
				if(placedVolume + remainingVolumes[level] < best.volume || (placedVolume + remainingVolumes[level] == best.volume && placedCount + length - level <= best.count)) {
					// cannot beat the best arrangement
					level = parents[level];
					descend = false;
					if(level == -1) {
						return;
					}
					continue;
				}
				BoxItem boxItem = boxItems[permutations[level]];
				Box box = boxItem.getBox();
				skipping[level] = !(boxItem.getContainerPriority() <= maxContainerPriorities[level]
						&& box.getWeight() <= freeLoadWeights[level]
						&& placedVolume + box.getVolume() <= maxLoadVolume);
				rotationIndexes[level] = 0;
			} else if(!skipping[level]) {
				// back from the next level: remove this level's placement and restore the free points
				int placedCount = placedCounts[level];
				if(utility != null) {
					Placement placement = placements[placedCount];
					placement.removeSupporteesAbove();
					for(PlacementLoad placementLoad : placement.getSupporters()) {
						placementLoad.getPlacement().removeLastSupportee();
					}
					placement.clearLoad();
				}
				stack.remove(stack.size() - 1);
				pointCalculator.setStackSize(placedCount);
				if(best.count == length) {
					// the best arrangement places all boxes: unwind without trying more candidates
					level = parents[level];
					if(level == -1) {
						return;
					}
					continue;
				}
			} else {
				// back from skipping this level's box
				level = parents[level];
				if(level == -1) {
					return;
				}
				continue;
			}

			int placedCount = placedCounts[level];
			if(!skipping[level]) {
				// the next rotation of this level's box which is fully supported at a position
				BoxStackValue[] stackValues = boxItems[permutations[level]].getBox().getStackValues();
				FullSupportCandidates candidates = pointCalculator.getFullSupportCandidates();
				BoxStackValue stackValue = null;
				int candidate = -1;
				while(candidate == -1 && rotationIndexes[level] < stackValues.length) {
					stackValue = stackValues[rotationIndexes[level]];
					rotationIndexes[level]++;
					candidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
					candidate = getBestFullySupported(candidates, stackValue, stack, placedCount, container, iterator, utility, pointComparator);
				}
				if(candidate != -1) {
					// place the box and continue with the next level
					Placement placement = placements[placedCount];
					SimplePoint3D point = candidates.getPoint(candidate);
					int pointIndex = candidates.getPointIndex(candidate);
					if(utility != null) {
						// the utility caches are primed for the last point checked: repopulate them for the selected point
						utility.populatePointSupporters(point);
						utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
						placement.clearLoad();
						placement.setIndex(stack.size());
						placement.setSupportedArea(utility.getSupportedAreaAtPoint(point, stackValue, false));
					}
					placement.setStackValue(stackValue);
					placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());
					pointCalculator.add(pointIndex, placement, point);
					stack.add(placement);
					if(utility != null) {
						utility.addSupportersLoad(placement);
					}
					int nextLevel = level + 1;
					if(nextLevel < length) {
						if(minAreas[nextLevel] != minAreas[level]) {
							pointCalculator.setMinimumAreaAndVolumeLimit(minAreas[nextLevel], iterator.getMinBoxVolume(nextLevel));
						} else {
							pointCalculator.setMinimumVolumeLimit(iterator.getMinBoxVolume(nextLevel));
						}
					}
					placedPermutations[placedCount] = permutations[level];
					placedRotations[placedCount] = rotationIndexes[level] - 1;
					enter(nextLevel, level, placedCount + 1, placedVolumes[level] + stackValue.getBox().getVolume(), freeLoadWeights[level] - stackValue.getBox().getWeight(),
							maxContainerPriorities[level], parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
					level = nextLevel;
					descend = true;
					continue;
				}
				// no more rotations: try skipping the box
				skipping[level] = true;
			}

			// skip this level's box: it waits for a later container, and so do the boxes of a higher container priority
			int skipEnd = skipEnds != null ? skipEnds[level] : level + 1;
			if(skipEnd == -1) {
				// cannot skip here (inside a box item group)
				level = parents[level];
				descend = false;
				if(level == -1) {
					return;
				}
				continue;
			}
			int skippedContainerPriority = Math.min(maxContainerPriorities[level], boxItems[permutations[level]].getContainerPriority());
			enter(skipEnd, level, placedCount, placedVolumes[level], freeLoadWeights[level], skippedContainerPriority,
					parents, placedCounts, placedVolumes, freeLoadWeights, maxContainerPriorities);
			level = skipEnd;
			descend = true;
		}
	}

	/**
	 * As {@link #packStackPlacement(FastPointCalculator3DStack, Placement[], BoxItemPermutationRotationIterator, Stack, Container, int, PackagerInterruptSupplier, int, long, LoadPlacementUtility, FastBruteForceBoxStackValuePointComparator, int, int[])},
	 * with full support required: each box is placed at the best position (by the point comparator) where it rests
	 * completely on the floor or on the boxes below (see {@link FullSupportCandidates}). With load limits, the boxes below
	 * must also carry it.
	 *
	 * @return the index of the first box which was not placed, or Integer.MIN_VALUE if interrupted
	 */
	protected int packStackPlacementFullSupport(FastPointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad,
			LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator,
			int maxPackableCount, int[] insertAfterCounts) {
		FullSupportCandidates candidates = pointCalculator.getFullSupportCandidates();
		while (placementIndex < maxPackableCount) {
			if(interrupt.getAsBoolean()) {
				return Integer.MIN_VALUE;
			}

			BoxStackValue stackValue = iterator.getStackValue(placementIndex);
			Box box = stackValue.getBox();
			if(box.getWeight() > freeWeightLoad) {
				break;
			}
			candidates.populate(pointCalculator, null, stack.getPlacements(), stackValue);
			int insertAfterCount = insertAfterCounts.length != 0 ? insertAfterCounts[placementIndex] : 0;
			int candidate = getBestFullySupported(candidates, stackValue, stack, insertAfterCount, container, iterator, utility, pointComparator);
			if(candidate == -1) {
				break;
			}

			SimplePoint3D point = candidates.getPoint(candidate);
			int pointIndex = candidates.getPointIndex(candidate);
			Placement placement = placements[placementIndex];
			if(utility != null) {
				// the utility caches are primed for the last position checked: repopulate them for the selected position
				utility.populatePointSupporters(point);
				utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
				placement.clearLoad();
				placement.setIndex(stack.size());
				placement.setSupportedArea(utility.getSupportedAreaAtPoint(point, stackValue, false));
			}
			placement.setStackValue(stackValue);
			placement.setPoint(pointIndex, point.getMinX(), point.getMinY(), point.getMinZ());

			pointCalculator.add(pointIndex, placement, point);
			stack.add(placement);
			if(utility != null) {
				utility.addSupportersLoad(placement);
			}

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

	/**
	 * @param insertAfterCount the number of boxes placed before the box, which it must be insertable after
	 * @return the best position (by the point comparator) where the box is fully supported, insertable and, with load
	 *         limits, carried by the boxes below; or -1 if none
	 */
	protected int getBestFullySupported(FullSupportCandidates candidates, BoxStackValue stackValue, Stack stack, int insertAfterCount, Container container,
			BoxItemPermutationRotationIterator iterator, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator) {
		boolean checkObstacles = !container.getObstacles().isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		int best = -1;
		for(int i = 0; i < candidates.size(); i++) {
			SimplePoint3D point = candidates.getPoint(i);
			if(insertAfterCount > 0 && !isInsertableAfter(point, stackValue, stack, insertAfterCount, container.getAccess())) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(best != -1 && pointComparator.compare(stackValue, candidates.getPoint(best), point) <= 0) {
				continue;
			}
			if(utility != null) {
				// validate the load only for positions which would be selected
				utility.populatePointSupporters(point);
				utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
				if(utility.getSupportedAreaAtPoint(point, stackValue, false) == -1L) {
					continue;
				}
			}
			best = i;
		}
		return best;
	}

	/**
	 * @param placedCount the number of boxes placed, which the box must be insertable after
	 * @return the best point (by the point comparator) for a box with a box item order, or -1 if none
	 */
	protected int getBestPoint(FastPointCalculator3DStack pointCalculator, BoxStackValue stackValue, Stack stack, int placedCount, Container container,
			BoxItemPermutationRotationIterator iterator, FastBruteForceBoxStackValuePointComparator pointComparator) {
		boolean checkObstacles = !container.getObstacles().isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		int bestPointIndex = -1;
		for(int k = 0; k < pointCalculator.size(); k++) {
			SimplePoint3D point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}
			if(placedCount > 0 && !isInsertableAfter(point, stackValue, stack, placedCount, container.getAccess())) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(bestPointIndex == -1 || pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), point) > 0) {
				bestPointIndex = k;
			}
		}
		return bestPointIndex;
	}

	/**
	 * As {@link #getBestPoint(FastPointCalculator3DStack, BoxStackValue, Stack, int, Container, BoxItemPermutationRotationIterator, FastBruteForceBoxStackValuePointComparator)},
	 * for boxes with load limits: the boxes below must carry the box at the point.
	 */
	protected int getBestPointWithLoad(FastPointCalculator3DStack pointCalculator, BoxStackValue stackValue, Stack stack, int placedCount, Container container,
			BoxItemPermutationRotationIterator iterator, LoadPlacementUtility utility, FastBruteForceBoxStackValuePointComparator pointComparator) {
		boolean checkObstacles = !container.getObstacles().isEmpty();
		boolean checkExtraction = hasExtractionOrders(iterator);
		int bestPointIndex = -1;
		for(int k = 0; k < pointCalculator.size(); k++) {
			SimplePoint3D point = pointCalculator.get(k);
			if(!point.fits3D(stackValue)) {
				continue;
			}
			if(placedCount > 0 && !isInsertableAfter(point, stackValue, stack, placedCount, container.getAccess())) {
				continue;
			}
			if(checkObstacles && !isInsertable(point, stackValue, container.getObstacles(), container.getAccess())) {
				continue;
			}
			if(checkExtraction && !isExtractable(point, stackValue, stack, container.getAccess())) {
				continue;
			}
			if(bestPointIndex != -1 && pointComparator.compare(stackValue, pointCalculator.get(bestPointIndex), point) <= 0) {
				continue;
			}
			// validate the load only for points which would be selected
			utility.populatePointSupporters(point);
			utility.populatePointSupportees(point, stackValue.getDz(), stackValue.getDz());
			if(utility.getSupportedAreaAtPoint(point, stackValue, false) == -1L) {
				continue;
			}
			bestPointIndex = k;
		}
		return bestPointIndex;
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
		if(requireFullSupport) {
			return packStackPlacementFullSupport(pointCalculator, placements, iterator, stack, container, placementIndex, interrupt, minStackableAreaIndex, freeWeightLoad,
					loadPlacementUtility, pointComparator, maxPackableCount, insertAfterCounts);
		}
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
}
