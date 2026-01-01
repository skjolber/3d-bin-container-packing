package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement.
 * So it does not try all possible placements (as i not all points)-
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes and containers must however only be used in a single thread at a time.
 */

public class FastBruteForcePackager extends AbstractBruteForcePackager {

	public static FastBruteForcePackagerBuilder newBuilder() {
		return new FastBruteForcePackagerBuilder();
	}

	public static class FastBruteForcePackagerBuilder {

		protected Comparator<IntermediatePackagerResult> comparator;
		
		public FastBruteForcePackagerBuilder withComparator(Comparator<IntermediatePackagerResult> comparator) {
			this.comparator = comparator;
			return this;
		}
		
		public FastBruteForcePackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator<>();
			}
			return new FastBruteForcePackager(comparator);
		}
		
	}
	
	private class FastBruteForceAdapter extends AbstractSingleThreadedBruteForceBoxItemPackagerAdapter {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceAdapter(List<BoxItem> boxItems, ContainerItemsCalculator packagerContainerItems,
				BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, packagerContainerItems, containerIterators, interrupt);
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}
	
	private class FastBruteForceGroupAdapter extends AbstractSingleThreadedBruteForceBoxItemGroupPackagerAdapter {

		private final FastPointCalculator3DStack pointCalculator;

		public FastBruteForceGroupAdapter(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, ContainerItemsCalculator packagerContainerItems,
				BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(boxItems, boxItemGroups, packagerContainerItems, containerIterators, interrupt);
			
			this.pointCalculator = new FastPointCalculator3DStack(getMaxIteratorLength() + 1);
			this.pointCalculator.clearToSize(1, 1, 1);
		}
		
		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return FastBruteForcePackager.this.pack(pointCalculator, stackPlacements, packagerContainerItems.getContainerItem(i), i, containerIterators[i], interrupt);
		}
		
	}

	@Override
	protected FastBruteForceGroupAdapter createBoxItemGroupAdapter(List<BoxItemGroup> itemGroups,
			ContainerItemsCalculator defaultContainerItemsCalculator, PackagerInterruptSupplier interrupt) {
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
		return new FastBruteForceGroupAdapter(boxItems, itemGroups, defaultContainerItemsCalculator, containerIterators, interrupt);
	}

	@Override
	protected FastBruteForceAdapter createBoxItemAdapter(List<BoxItem> boxItems, ContainerItemsCalculator defaultContainerItemsCalculator,
			PackagerInterruptSupplier interrupt) {
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
		
		return new FastBruteForceAdapter(boxItems, defaultContainerItemsCalculator, containerIterators, interrupt);
	}
	
	public FastBruteForcePackager(Comparator<IntermediatePackagerResult> comparator) {
		super(comparator);
	}

	public BruteForceIntermediatePackagerResult pack(FastPointCalculator3DStack pointCalculator,
			List<Placement> stackPlacements, ControlledContainerItem containerItem, int containerIndex,
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
			pointCalculator.clearToSize(holder.getLoadDx(), holder.getLoadDy(), holder.getLoadDz());

			int index = 0;

			do {
				// attempt to limit the number of points created
				// by calculating the minimum point volume and area
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(index);
				long minStackableVolume = iterator.getMinBoxVolume(index);

				pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), minStackableVolume);

				int count = packStackPlacement(pointCalculator, stackPlacements, iterator, stack, holder, index, interrupt, minStackableAreaIndex, freeLoadWeights[index]);
				if(count == Integer.MIN_VALUE) {
					return null; // timeout
				}

				// continue search, but see if this is the best fit so far
				// higher count implies higher volume and weight
				// since the items are the same within each permutation
				if(count > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(pointCalculator.getPoints(), iterator.getState(), stackPlacements);
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

				pointCalculator.setStackSize(rotationIndex);
				stack.setSize(rotationIndex);

				index = rotationIndex;
			} while (true);

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result

				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) == ARGUMENT_2_IS_BETTER) {
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

	public int packStackPlacement(FastPointCalculator3DStack pointCalculator, List<Placement> placements,
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

			int pointCount = pointCalculator.size();

			int bestPointIndex = -1;
			for (int k = 0; k < pointCount; k++) {
				Point point3d = pointCalculator.get(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point bestPoint = pointCalculator.get(bestPointIndex);
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

			Point point3d = pointCalculator.get(bestPointIndex);

			placement.setStackValue(stackValue);
			placement.setPoint(point3d);

			pointCalculator.add(bestPointIndex, placement);

			freeWeightLoad -= stackable.getWeight();

			stack.add(placement);

			placementIndex++;

			if(placementIndex < iterator.length()) {
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

}
