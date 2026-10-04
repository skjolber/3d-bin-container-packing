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
import com.github.skjolber.packing.api.Placement;
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
 * Thread-safe implementation. The input boxes and containers must however only be used in a single thread at a time.
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
			return packager;
		}
		
	}
	
	private class FastBruteForceSession extends AbstractSingleThreadedBruteForceBoxItemSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceSession(List<BoxItem> boxItems, List<ContainerItem> containers,
				int containerCount, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, containers, containerCount, containerIterators, interrupt, FastBruteForcePackager.this.supportsLoad());
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceSession(FastBruteForceSession source) {
			super(source, FastBruteForcePackager.this.supportsLoad());
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
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, stackPlacementCount, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt, fastPointComparator, best);
		}
		
	}
	
	private class FastBruteForceGroupSession extends AbstractSingleThreadedBruteForceBoxItemGroupSession {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceGroupSession(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containers, int containerCount,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, containers, containerCount, containerIterators, interrupt, FastBruteForcePackager.this.supportsLoad());
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		private FastBruteForceGroupSession(FastBruteForceGroupSession source) {
			super(source, FastBruteForcePackager.this.supportsLoad());
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
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			BoxItemGroup[] iteratorGroups = containerIterators[i].getBoxItemGroups();
			if(!canLoadNextGroup(iteratorGroups)) {
				return null;
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
		stack.clear();
	}

	protected void setStackSize(Stack stack, int size, LoadPlacementUtility loadPlacementUtility) {
		stack.setSize(size);
	}

	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator) {
		return pack(pointCalculator, stackPlacements, stackPlacementCount, containerItem, containerIndex, iterator, interrupt, pointComparator, null);
	}

	/**
	 * @param best the best result so far, or null. When results with less load volume always compare worse,
	 *        returns an empty result if no result can load more than {@code best}.
	 */
	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			Placement[] stackPlacements, int stackPlacementCount, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt, FastBruteForceBoxStackValuePointComparator pointComparator, IntermediatePackagerResult best) {
		
		Container holder = containerItem.getContainer().clone(iterator.length());
		
		Stack stack = holder.getStack();
		
		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, supportsLoad());
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(iterator.length()), containerIndex, iterator, supportsLoad());

		long[] freeLoadWeights = calculateFreeLoadWeights(holder, iterator);
		LoadPlacementUtility loadPlacementUtility = createLoadPlacementUtility(iterator, stack);
		if(loadPlacementUtility != null) {
			loadPlacementUtility.initialize(iterator.length());
		}
		
		boolean allItemsFit = canPackAll(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());

		// results with less load volume than the best result so far are never selected
		long minLoadVolume = getMinLoadVolume(best);
		if(minLoadVolume > 0L && getMaxLoadVolume(iterator, holder, allItemsFit) < minLoadVolume) {
			return bestResult;
		}

		// iterator over all permutations
		permutations: 
		do {
			if(interrupt.getAsBoolean()) {
				return null;
			}
			// iterate over all rotations

			bestPermutationResult.reset();
			int maxPackableCount = allItemsFit ? iterator.length() : getMaxPackableCount(iterator, holder.getMaxLoadVolume(), holder.getMaxLoadWeight());
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
							minStackableAreaIndex, freeLoadWeights[index], loadPlacementUtility, pointComparator, maxPackableCount);
					if(count == Integer.MIN_VALUE) {
						return null; // timeout
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
				minStackableAreaIndex, freeWeightLoad, loadPlacementUtility, pointComparator, maxPackableCount);
	}

	protected int packStackPlacement(FastPointCalculator3DStack pointCalculator, Placement[] placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad,
			LoadPlacementUtility loadPlacementUtility, FastBruteForceBoxStackValuePointComparator pointComparator,
			int maxPackableCount) {
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

	@Override
	protected LoadPlacementUtility createLoadPlacementUtility(BoxItemPermutationRotationIterator iterator, Stack stack) {
		return null;
	}
}
