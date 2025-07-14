package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.PackResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.DefaultPackagerResultBuilder;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

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

public abstract class AbstractBruteForcePackager extends AbstractPackager<BruteForceIntermediatePackagerResult, BruteForcePackagerResultBuilder> {

	private static final Logger LOGGER = Logger.getLogger(AbstractBruteForcePackager.class.getName());
	
	public AbstractBruteForcePackager(IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withPackager(this);
	}

	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new StackPlacement());
		}
		return placements;
	}

	public BruteForceIntermediatePackagerResult pack(ExtremePoints3DStack extremePoints, List<StackPlacement> stackPlacements, ContainerItem containerItem, int index,
			PermutationRotationIterator iterator, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {

		Container holder = containerItem.getContainer().clone();
		
		Stack stack = holder.getStack();
		
		BruteForceIntermediatePackagerResult bestResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), index, iterator);
		
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForceIntermediatePackagerResult bestPermutationResult = new BruteForceIntermediatePackagerResult(containerItem, new Stack(), index, iterator);

		// iterator over all permutations
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// iterate over all rotations
			bestPermutationResult.reset();

			do {
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

				List<Point> points = packStackPlacement(extremePoints, stackPlacements, iterator, stack, holder, interrupt, minStackableAreaIndex);
				if(points == null) {
					return null; // stack overflow
				}
				
				stack.clear();
				
				if(points.size() > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(points, iterator.getState(), stackPlacements);
					if(points.size() == iterator.length() || acceptAsFull(bestPermutationResult, holder)) {
						// best possible result for this container
						return bestPermutationResult;
					}
				}

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
			} while (true);

			int permutationIndex = iterator.nextPermutation(bestPermutationResult.getSize());

			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result
				
				if(bestResult.isEmpty() || intermediatePackagerResultComparator.compare(bestResult, bestPermutationResult) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
					// switch the two results for one another
					BruteForceIntermediatePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}
			
			// search for the next permutation which actually 
			// has a chance of affecting the result.

			// TODO can bounds on weight or volume be used to determine whether it is even possible to beat the best result?

			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		return bestResult;
	}

	protected abstract boolean acceptAsFull(BruteForceIntermediatePackagerResult bestPermutationResult, Container holder);

	public List<Point> packStackPlacement(ExtremePoints3DStack extremePoints, List<StackPlacement> placements, PermutationRotationIterator iterator, Stack stack,
			Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex) throws PackagerInterruptedException {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		int maxLoadWeight = container.getMaxLoadWeight();

		extremePoints.reset(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints.setMinimumAreaAndVolumeLimit(iterator.get(minStackableAreaIndex).getBoxStackValue().getArea(), iterator.getMinStackableVolume(0));

		try {
			// note: currently implemented as a recursive algorithm
			return packStackPlacement(extremePoints, placements, iterator, stack, maxLoadWeight, 0, interrupt, minStackableAreaIndex, Collections.emptyList());
		} catch (StackOverflowError e) {
			// TODO throw packager exception
			
			LOGGER.warning("Stack overflow occoured for " + placements.size() + " boxes. Limit number of boxes or increase thread stack");
			return null;
		}
	}

	private List<Point> packStackPlacement(
			ExtremePoints3DStack extremePointsStack, 
			List<StackPlacement> placements, 
			PermutationRotationIterator rotator, 
			Stack stack,
			int maxLoadWeight, 
			int placementIndex, 
			PackagerInterruptSupplier interrupt, 
			int minStackableAreaIndex,
			// optimize: pass best along so that we do not need to get points to known whether extracting the points is necessary
			List<Point> best
		) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			// fit2d below might have returned due to deadline
			throw new PackagerInterruptedException();
		}
		PermutationRotation permutationRotation = rotator.get(placementIndex);

		BoxItem stackable = permutationRotation.getBoxItem();
		if(stackable.getWeight() > maxLoadWeight) {
			return null;
		}

		StackPlacement placement = placements.get(placementIndex);
		BoxStackValue stackValue = permutationRotation.getBoxStackValue();

		placement.setBoxItem(stackable);
		placement.setStackValue(stackValue);

		maxLoadWeight -= stackable.getWeight();

		if(extremePointsStack.getStackIndex() > best.size()) {
			best = extremePointsStack.getPoints();
		}

		extremePointsStack.push();

		int currentPointsCount = extremePointsStack.size();

		for (int k = 0; k < currentPointsCount; k++) {
			Point point3d = extremePointsStack.get(k);

			if(!point3d.fits3D(stackValue)) {
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

			boolean minArea = placementIndex == minStackableAreaIndex;
			if(minArea) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);

				extremePointsStack.setMinimumAreaAndVolumeLimit(rotator.get(nextMinStackableAreaIndex).getBoxStackValue().getArea(), rotator.getMinStackableVolume(placementIndex + 1));
			} else {
				extremePointsStack.setMinimumVolumeLimit(rotator.getMinStackableVolume(placementIndex + 1));

				nextMinStackableAreaIndex = minStackableAreaIndex;
			}

			List<Point> points = packStackPlacement(extremePointsStack, placements, rotator, stack, maxLoadWeight, placementIndex + 1, interrupt, 
					nextMinStackableAreaIndex, best);

			stack.remove(placement);

			if(points != null) {
				if(points.size() >= rotator.length()) {
					best = points;
					break;
				}

				if(best.size() < points.size()) {
					best = points;
				}
			}
			extremePointsStack.redo();
		}

		extremePointsStack.pop();

		return best;
	}

}
