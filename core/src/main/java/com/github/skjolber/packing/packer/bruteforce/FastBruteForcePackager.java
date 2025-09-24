package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.PackResultComparator;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement.
 * So it does not try all possible placements (as i not all extreme-points)-
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes and containers must however only be used in a single thread at a time.
 */

public class FastBruteForcePackager extends AbstractBruteForcePackager {

	// implementation notes:
	//  * new BoxItemControlsBuilder / BoxItemGroupControls per permutation 
	//
	// placement results:
	//  * comparing box items is not necessary
	//  * comparing groups is not necessary
	//
	// filtering:
	// boxes: (resets for each permutation)
	//  * box items
	//  * box item groups 
	//
	// points: (resets for each box)
	//  * finding best point is necessary
	//  * filtering available points per box item is necessary
	//
	// strategy:
	//  * find first result (current implementation)
	//  * find best result according to comparator (future implementation)
	//

	public static FastBruteForcePackagerBuilder newBuilder() {
		return new FastBruteForcePackagerBuilder();
	}

	public static class FastBruteForcePackagerBuilder {

		protected Comparator<BruteForceIntermediatePackagerResult> comparator;
		
		public FastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			return new FastBruteForcePackager(comparator);
		}
		
	}
	
	private class FastBruteForceAdapter extends AbstractSingleThreadedBruteForceBoxItemPackagerAdapter {

		private final FastExtremePoints3DStack extremePoints;

		public FastBruteForceAdapter(List<BoxItem> boxItems, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, containerIterators, interrupt);
			
			this.extremePoints = new FastExtremePoints3DStack(getMaxIteratorLength() + 1);
			this.extremePoints.clearToSize(1, 1, 1);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return FastBruteForcePackager.this.pack(extremePoints, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}
	
	private class FastBruteForceGroupAdapter extends AbstractSingleThreadedBruteForceBoxItemGroupPackagerAdapter {

		private final FastExtremePoints3DStack extremePoints;

		public FastBruteForceGroupAdapter(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, BoxPriority priority,
				ContainerItemsCalculator packagerContainerItems,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, priority, packagerContainerItems, containerIterators, interrupt);
			
			this.extremePoints = new FastExtremePoints3DStack(getMaxIteratorLength() + 1);
			this.extremePoints.clearToSize(1, 1, 1);
		}
		
		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return FastBruteForcePackager.this.pack(extremePoints, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}

	@Override
	protected FastBruteForceGroupAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups,
			BoxPriority priority, ContainerItemsCalculator defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
		DefaultBoxItemGroupPermutationRotationIterator[] containerIterators = new DefaultBoxItemGroupPermutationRotationIterator[defaultContainerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < defaultContainerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = defaultContainerItemsCalculator.getContainerItem(i);
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
		return new FastBruteForceGroupAdapter(boxItems, itemGroups, priority, defaultContainerItemsCalculator, containerIterators, interrupt);
	}

	@Override
	protected FastBruteForceAdapter createBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority,
			ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[defaultContainerItemsCalculator.getContainerItemCount()];

		for (int i = 0; i < defaultContainerItemsCalculator.getContainerItemCount(); i++) {
			ContainerItem containerItem = defaultContainerItemsCalculator.getContainerItem(i);
			Container container = containerItem.getContainer();

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		return new FastBruteForceAdapter(boxItems, priority, defaultContainerItemsCalculator, containerIterators, interrupt);
	}
	
	public FastBruteForcePackager(Comparator<BruteForceIntermediatePackagerResult> comparator) {
		super(comparator);
	}

	public BruteForceIntermediatePackagerResult pack(FastExtremePoints3DStack extremePoints,
			List<Placement> stackPlacements, ContainerItem containerItem, int containerIndex,
			BoxItemPermutationRotationIterator iterator,
			PackagerInterruptSupplier interrupt) {
		
		Container holder = containerItem.getContainer().clone();
		
		Stack stack = holder.getStack();
		
		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), containerIndex, iterator);
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), containerIndex, iterator);

		long[] freeLoadWeights = calculateFreeLoadWeights(holder, iterator);
		
		// iterator over all permutations
		permutations: 
		do {
			if(interrupt.getAsBoolean()) {
				return null;
			}
			// iterate over all rotations

			bestPermutationResult.reset();
			extremePoints.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());

			int index = 0;

			do {
				// attempt to limit the number of points created
				// by calculating the minimum point volume and area
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(index);
				long minStackableVolume = iterator.getMinBoxVolume(index);

				extremePoints.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), minStackableVolume);

				int count = packStackPlacement(extremePoints, stackPlacements, iterator, stack, holder, index, interrupt, minStackableAreaIndex, freeLoadWeights[index]);
				if(count == Integer.MIN_VALUE) {
					return null; // timeout
				}

				// continue search, but see if this is the best fit so far
				// higher count implies higher volume and weight
				// since the items are the same within each permutation
				if(count > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(extremePoints.getPoints(), iterator.getState(), stackPlacements);
					if(count == iterator.length()) {
						return bestPermutationResult;
					}
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
					stack.clear();
					break;
				}

				extremePoints.setStackSize(rotationIndex);
				stack.setSize(rotationIndex);

				index = rotationIndex;
			} while (true);

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result

				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
					// switch the two results for one another
					BruteForceIntermediatePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}

			// get the next permutation
			// make sure there is actually free weight available
			// at the next index
			int size = bestPermutationResult.getSize();
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

	public int packStackPlacement(FastExtremePoints3DStack extremePoints3D, List<Placement> placements,
			BoxItemPermutationRotationIterator iterator, Stack stack, Container container, int placementIndex,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex, long freeWeightLoad) {
		// pack as many items as possible from placementIndex

		while (placementIndex < iterator.length()) {
			if(interrupt.getAsBoolean()) {
				// might have returned due to deadline
				return Integer.MIN_VALUE;
			}
			BoxStackValue stackValue = iterator.getStackValue(placementIndex);

			Box stackable = stackValue.getBox();
			if(stackable.getWeight() > freeWeightLoad) {
				break;
			}

			Placement placement = placements.get(placementIndex);

			int pointCount = extremePoints3D.size();

			int bestPointIndex = -1;
			for (int k = 0; k < pointCount; k++) {
				Point point3d = extremePoints3D.get(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point bestPoint = extremePoints3D.get(bestPointIndex);
					if(bestPoint.getArea() < point3d.getArea()) {
						continue;
					} else if(bestPoint.getArea() == point3d.getArea() && bestPoint.getVolume() < point3d.getVolume()) {
						continue;
					}
				}
				bestPointIndex = k;
			}

			if(bestPointIndex == -1) { // interrupted
				break;
			}

			Point point3d = extremePoints3D.get(bestPointIndex);

			placement.setStackValue(stackValue);
			placement.setPoint(point3d);

			extremePoints3D.add(bestPointIndex, placement);

			freeWeightLoad -= stackable.getWeight();

			stack.add(placement);

			placementIndex++;

			if(placementIndex < iterator.length()) {
				// check whether minimum point volume and area should be adjusted 
				boolean minArea = placementIndex == minStackableAreaIndex;
				if(minArea) {
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);

					extremePoints3D.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(placementIndex));
				} else {
					extremePoints3D.setMinimumVolumeLimit(iterator.getMinBoxVolume(placementIndex));
				}
			}
		}

		return placementIndex;
	}

}
