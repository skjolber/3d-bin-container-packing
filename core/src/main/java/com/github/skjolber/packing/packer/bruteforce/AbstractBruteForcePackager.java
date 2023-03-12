package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public abstract class AbstractBruteForcePackager extends AbstractPackager<BruteForcePackagerResult, BruteForcePackagerResultBuilder> {

	private static Logger LOGGER = Logger.getLogger(AbstractBruteForcePackager.class.getName());

	public AbstractBruteForcePackager(int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		super(checkpointsPerDeadlineCheck, packResultComparator);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}

	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new StackPlacement());
		}
		return placements;
	}

	public BruteForcePackagerResult pack(ExtremePoints3DStack extremePoints, List<StackPlacement> stackPlacements, Container targetContainer, int index, ContainerStackValue stackValue,
			PermutationRotationIterator iterator, BooleanSupplier interrupt) {
		DefaultStack stack = new DefaultStack(stackValue);
		Container holder = new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(),
				targetContainer.getStackValues(), stack);

		BruteForcePackagerResult bestResult = new BruteForcePackagerResult(holder, index, iterator);
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForcePackagerResult bestPermutationResult = new BruteForcePackagerResult(holder, index, iterator);

		// iterator over all permutations
		do {
			if(interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			bestPermutationResult.reset();

			int firstMinStackableVolumeIndex = iterator.getMinStackableVolumeIndex(0);
			int minStackableVolumeIndex = firstMinStackableVolumeIndex;

			do {
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

				List<Point3D<StackPlacement>> points = packStackPlacement(extremePoints, stackPlacements, iterator, stack, interrupt, minStackableAreaIndex, minStackableVolumeIndex);
				if(points == null) {
					return null; // timeout
				}
				if(points.size() > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(points, iterator.getState(), stackPlacements);
					if(points.size() == iterator.length()) {
						// best possible result for this container
						return bestPermutationResult;
					}
				}

				holder.getStack().clear();

				// search for the next rotation which actually 
				// has a chance of affecting the result.
				// i.e. if we have four boxes, and two boxes could be placed with the 
				// current rotations, and the new rotation only changes the rotation of box 4,
				// then we know that attempting to stack again will not work

				int rotationIndex = iterator.nextRotation(points.size());

				if(rotationIndex == -1) {
					// no more rotations, continue to next permutation
					break;
				}

				if(rotationIndex > minStackableVolumeIndex) {
					// these could be cached?
					minStackableVolumeIndex = iterator.getMinStackableVolumeIndex(rotationIndex);
				} else {
					minStackableVolumeIndex = firstMinStackableVolumeIndex;
				}

			} while (true);

			// search for the next permutation which actually 
			// has a chance of affecting the result.

			int permutationIndex = iterator.nextPermutation(bestPermutationResult.getSize());

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

	public List<Point3D<StackPlacement>> packStackPlacement(ExtremePoints3DStack extremePoints, List<StackPlacement> placements, PermutationRotationIterator iterator, Stack stack,
			BooleanSupplier interrupt, int minStackableAreaIndex, int minStackableVolumeIndex) {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		ContainerStackValue containerStackValue = stack.getContainerStackValue();

		int maxLoadWeight = containerStackValue.getMaxLoadWeight();

		extremePoints.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		extremePoints.setMinimumAreaAndVolumeLimit(iterator.get(minStackableAreaIndex).getValue().getArea(), iterator.get(minStackableVolumeIndex).getValue().getVolume());

		try {
			// note: currently implemented as a recursive algorithm
			return packStackPlacement(extremePoints, placements, iterator, stack, maxLoadWeight, 0, interrupt, containerStackValue.getConstraint(), minStackableAreaIndex, minStackableVolumeIndex);
		} catch (StackOverflowError e) {
			LOGGER.warning("Stack overflow occoured for " + placements.size() + " boxes. Limit number of boxes or increase thread stack");
			return null;
		}
	}

	private List<Point3D<StackPlacement>> packStackPlacement(ExtremePoints3DStack extremePointsStack, List<StackPlacement> placements, PermutationRotationIterator rotator, Stack stack,
			int maxLoadWeight, int placementIndex, BooleanSupplier interrupt, StackConstraint constraint, int minStackableAreaIndex, int minStackableVolumeIndex) {
		if(interrupt.getAsBoolean()) {
			// fit2d below might have returned due to deadline
			return null;
		}

		PermutationRotation permutationRotation = rotator.get(placementIndex);

		Stackable stackable = permutationRotation.getStackable();
		if(stackable.getWeight() > maxLoadWeight) {
			return null;
		}

		if(constraint != null && !constraint.accepts(stack, stackable)) {
			return extremePointsStack.getPoints();
		}

		StackPlacement placement = placements.get(placementIndex);
		StackValue stackValue = permutationRotation.getValue();

		placement.setStackable(stackable);
		placement.setStackValue(stackValue);

		maxLoadWeight -= stackable.getWeight();

		List<Point3D<StackPlacement>> best = extremePointsStack.getPoints();

		extremePointsStack.push();

		int currentPointsCount = extremePointsStack.getValueCount();

		for (int k = 0; k < currentPointsCount; k++) {
			Point3D<StackPlacement> point3d = extremePointsStack.getValue(k);

			if(!point3d.fits3D(stackValue)) {
				continue;
			}
			if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
				continue;
			}

			placement.setX(point3d.getMinX());
			placement.setY(point3d.getMinY());
			placement.setZ(point3d.getMinZ());

			extremePointsStack.add(k, placement);

			if(placementIndex + 1 >= rotator.length()) {
				best = extremePointsStack.getPoints();

				break;
			}

			stack.add(placement);

			// should minimum area / volume be adjusted?
			int nextMinStackableAreaIndex;
			int nextMinStackableVolumeIndex;

			boolean minArea = placementIndex == minStackableAreaIndex;
			boolean minVolume = placementIndex == minStackableVolumeIndex;
			if(minArea && minVolume) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);
				nextMinStackableVolumeIndex = rotator.getMinStackableVolumeIndex(placementIndex + 1);

				extremePointsStack.setMinimumAreaAndVolumeLimit(rotator.get(nextMinStackableAreaIndex).getValue().getArea(), rotator.get(nextMinStackableVolumeIndex).getValue().getVolume());
			} else if(minArea) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);
				extremePointsStack.setMinimumAreaLimit(rotator.get(nextMinStackableAreaIndex).getValue().getArea());

				nextMinStackableVolumeIndex = minStackableVolumeIndex;
			} else if(minVolume) {
				nextMinStackableVolumeIndex = rotator.getMinStackableVolumeIndex(placementIndex + 1);
				extremePointsStack.setMinimumVolumeLimit(rotator.get(nextMinStackableVolumeIndex).getValue().getVolume());

				nextMinStackableAreaIndex = minStackableAreaIndex;
			} else {
				nextMinStackableAreaIndex = minStackableAreaIndex;
				nextMinStackableVolumeIndex = minStackableVolumeIndex;
			}

			List<Point3D<StackPlacement>> points = packStackPlacement(extremePointsStack, placements, rotator, stack, maxLoadWeight, placementIndex + 1, interrupt, constraint,
					nextMinStackableAreaIndex, nextMinStackableVolumeIndex);

			stack.remove(placement);

			if(points != null) {
				if(points.size() >= rotator.length()) {
					best = points;
					break;
				}

				if(best == null || best.size() < points.size()) {
					best = points;
				}
			}
			extremePointsStack.redo();
		}

		extremePointsStack.pop();

		return best;
	}

}
