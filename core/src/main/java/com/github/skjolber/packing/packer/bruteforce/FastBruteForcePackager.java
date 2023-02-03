package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement. So it does not try all possible placements (as i not all extreme-points)-
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class FastBruteForcePackager extends AbstractPackager<BruteForcePackagerResult, FastBruteForcePackagerResultBuilder> {

	public static FastBruteForcePackagerBuilder newBuilder() {
		return new FastBruteForcePackagerBuilder();
	}

	public static class FastBruteForcePackagerBuilder extends AbstractPackagerBuilder<FastBruteForcePackager, FastBruteForcePackagerBuilder> {

		public FastBruteForcePackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new FastBruteForcePackager(containers, checkpointsPerDeadlineCheck, packResultComparator);
		}
	}

	private class FastBruteForceAdapter implements Adapter<BruteForcePackagerResult> {

		private final ContainerStackValue[] containerStackValue;
		private final DefaultPermutationRotationIterator[] iterators;
		private final List<Container> containers;
		private final BooleanSupplier interrupt;
		private final FastExtremePoints3DStack extremePoints3D;
		private List<StackPlacement> stackPlacements;

		public FastBruteForceAdapter(List<StackableItem> stackableItems, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.iterators = new DefaultPermutationRotationIterator[containers.size()];
			this.containerStackValue = new ContainerStackValue[containers.size()];

			for (int i = 0; i < containers.size(); i++) {
				Container container = containers.get(i);

				ContainerStackValue stackValue = container.getStackValues()[0];

				containerStackValue[i] = stackValue;

				Dimension dimension = new Dimension(stackValue.getDx(), stackValue.getDy(), stackValue.getDz());

				StackConstraint constraint = stackValue.getConstraint();

				iterators[i] = DefaultPermutationRotationIterator
						.newBuilder()
						.withLoadSize(dimension)
						.withStackableItems(stackableItems)
						.withMaxLoadWeight(stackValue.getMaxLoadWeight())
						.withFilter(stackable -> constraint == null || constraint.canAccept(stackable))
						.build();
			}

			this.interrupt = interrupt;

			int count = 0;
			for (DefaultPermutationRotationIterator iterator : iterators) {
				count = Math.max(count, iterator.length());
			}

			this.stackPlacements = getPlacements(count);
			this.extremePoints3D = new FastExtremePoints3DStack(1, 1, 1, count);
		}

		private List<StackPlacement> getPlacements(int size) {
			// each box will at most have a single placement with a space (and its remainder).
			List<StackPlacement> placements = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				placements.add(new StackPlacement());
			}
			return placements;
		}

		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult best) {
			if(iterators[i].length() == 0) {
				return BruteForcePackagerResult.EMPTY;
			}
			// TODO break if this container cannot beat the existing best result
			return FastBruteForcePackager.this.pack(extremePoints3D, stackPlacements, containers.get(i), containerStackValue[i], iterators[i], interrupt);
		}

		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			Container container = bruteForceResult.getContainer();
			Stack stack = container.getStack();

			int size = stack.getSize();
			if(stackPlacements.size() > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators

				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();

				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}

				for (PermutationRotationIterator it : iterators) {
					it.removePermutations(p);
				}
				stackPlacements = stackPlacements.subList(size, this.stackPlacements.size());
			} else {
				stackPlacements = Collections.emptyList();
			}

			return container;
		}
	}

	public FastBruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		super(containers, checkpointsPerDeadlineCheck, packResultComparator);
	}

	@Override
	public FastBruteForcePackagerResultBuilder newResultBuilder() {
		return new FastBruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}

	@Override
	protected Adapter<BruteForcePackagerResult> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new FastBruteForceAdapter(boxes, containers, interrupt);
	}

	public BruteForcePackagerResult pack(FastExtremePoints3DStack extremePoints, List<StackPlacement> stackPlacements, Container targetContainer, ContainerStackValue containerStackValue,
			DefaultPermutationRotationIterator rotator, BooleanSupplier interrupt) {
		Stack stack = new DefaultStack(containerStackValue);

		Container holder = new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(),
				targetContainer.getStackValues(), stack);

		BruteForcePackagerResult bestResult = new BruteForcePackagerResult(holder, rotator);
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForcePackagerResult bestPermutationResult = new BruteForcePackagerResult(holder, rotator);

		// iterator over all permutations
		do {

			if(interrupt.getAsBoolean()) {
				return null;
			}
			// iterate over all rotations

			bestPermutationResult.reset();
			extremePoints.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());

			int index = 0;

			int firstMinStackableVolumeIndex = rotator.getMinStackableVolumeIndex(0);
			int minStackableVolumeIndex = firstMinStackableVolumeIndex;

			do {
				// attempt to limit the number of points created
				// by calculating the minimum point volume and area
				int minStackableAreaIndex = rotator.getMinStackableAreaIndex(index);

				extremePoints.setMinimumAreaAndVolumeLimit(rotator.get(minStackableAreaIndex).getValue().getArea(), rotator.get(minStackableVolumeIndex).getValue().getVolume());

				int count = packStackPlacement(extremePoints, stackPlacements, rotator, stack, index, interrupt, minStackableAreaIndex, minStackableVolumeIndex);
				if(count == Integer.MIN_VALUE) {
					return null; // timeout
				}

				// continue search, but see if this is the best fit so far
				// higher count implies higher volume and weight
				// since the items are the same within each permutation
				if(count > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(extremePoints.getPoints(), rotator.getState(), stackPlacements);
					if(count == rotator.length()) {
						return bestPermutationResult;
					}
				}

				// search for the next rotation which actually 
				// has a chance of affecting the result.
				// i.e. if we have four boxes, and two boxes could be placed with the 
				// current rotations, and the new rotation only changes the rotation of box 4,
				// then we know that attempting to stack again will not work since box
				// 3 will still remain in the same rotation (which could not be placed)

				int rotationIndex = rotator.nextRotation(count);

				if(rotationIndex == -1) {
					// no more rotations, continue to next permutation
					extremePoints.setStackSize(0);
					stack.clear();
					break;
				}

				extremePoints.setStackSize(rotationIndex);
				stack.setSize(rotationIndex);

				index = rotationIndex;

				if(index > minStackableVolumeIndex) {
					// these could be cached?
					minStackableVolumeIndex = rotator.getMinStackableVolumeIndex(index);
				} else {
					minStackableVolumeIndex = firstMinStackableVolumeIndex;
				}
			} while (true);

			int permutationIndex = rotator.nextPermutation(bestPermutationResult.getSize());

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result

				if(bestResult.isEmpty() || packResultComparator.compare(bestResult, bestPermutationResult) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
					// switch the two results for one another
					BruteForcePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}

			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		return bestResult;
	}

	public int packStackPlacement(FastExtremePoints3DStack extremePoints3D, List<StackPlacement> placements, PermutationRotationIterator iterator, Stack stack, int placementIndex,
			BooleanSupplier interrupt, int minStackableAreaIndex, int minStackableVolumeIndex) {
		// pack as many items as possible from placementIndex
		ContainerStackValue containerStackValue = stack.getContainerStackValue();

		StackConstraint constraint = containerStackValue.getConstraint();

		int freeWeightLoad = stack.getFreeWeightLoad();

		while (placementIndex < iterator.length()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				return Integer.MIN_VALUE;
			}
			PermutationRotation permutationRotation = iterator.get(placementIndex);

			Stackable stackable = permutationRotation.getStackable();
			if(stackable.getWeight() > freeWeightLoad) {
				break;
			}

			if(constraint != null && !constraint.accepts(stack, stackable)) {
				break;
			}

			StackPlacement placement = placements.get(placementIndex);

			StackValue stackValue = permutationRotation.getValue();

			int pointCount = extremePoints3D.getValueCount();

			int bestPointIndex = -1;
			for (int k = 0; k < pointCount; k++) {
				Point3D<StackPlacement> point3d = extremePoints3D.getValue(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}
				if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point3D<StackPlacement> bestPoint = extremePoints3D.getValue(bestPointIndex);
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

			Point3D<StackPlacement> point3d = extremePoints3D.getValue(bestPointIndex);

			placement.setStackable(stackable);
			placement.setStackValue(stackValue);
			placement.setX(point3d.getMinX());
			placement.setY(point3d.getMinY());
			placement.setZ(point3d.getMinZ());

			extremePoints3D.add(bestPointIndex, placement);

			freeWeightLoad -= stackable.getWeight();

			stack.add(placement);

			placementIndex++;

			if(placementIndex < iterator.length()) {
				// check whether minimum point volume and area should be adjusted 
				boolean minArea = placementIndex == minStackableAreaIndex;
				boolean minVolume = placementIndex == minStackableVolumeIndex;
				if(minArea && minVolume) {
					minStackableVolumeIndex = iterator.getMinStackableVolumeIndex(placementIndex);
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);

					extremePoints3D.setMinimumAreaAndVolumeLimit(iterator.get(minStackableAreaIndex).getValue().getArea(), iterator.get(minStackableVolumeIndex).getValue().getVolume());
				} else if(minArea) {
					minStackableAreaIndex = iterator.getMinStackableAreaIndex(placementIndex);
					extremePoints3D.setMinimumAreaLimit(iterator.get(minStackableAreaIndex).getValue().getArea());
				} else if(minVolume) {
					minStackableVolumeIndex = iterator.getMinStackableVolumeIndex(placementIndex);
					extremePoints3D.setMinimumVolumeLimit(iterator.get(minStackableVolumeIndex).getValue().getVolume());
				}
			}
		}

		return placementIndex;
	}

}
