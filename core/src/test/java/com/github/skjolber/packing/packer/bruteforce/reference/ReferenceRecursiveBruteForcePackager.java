package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.packer.bruteforce.PointCalculator3DStack;

/**
 * Ported from master @ e79cf716 (4.2.4) as a reference implementation for differential tests. The recursion and
 * enumeration order are intentionally preserved - do not modernize or optimize.
 * <p>
 * Origin: {@code BruteForcePackager} (the single-container attempt of its box item adapter) and the
 * {@code pack(..)} and {@code packStackPlacement(..)} methods of {@code AbstractBruteForcePackager}.
 * <p>
 * Fit boxes into one container, i.e. perform bin packing to a single container, trying all permutations, rotations
 * and points. Plain box items only: no groups, no controls, no container strategies, no multiple containers and no
 * parallelism. The extreme points are those of the 5.0 {@link PointCalculator3DStack}, so this reference is
 * independent of the 5.0 search and of the 5.0 enumeration, not of the point calculation.
 * <p>
 * Outer loops enumerate the box orders (permutations) and, for each, the rotations of the boxes; for each such state
 * the recursion places the boxes one by one, in order, at every free point which fits, and the longest placed prefix
 * of the order is the result of the state. The best result is that of the most placed boxes within a permutation, and
 * across permutations the one with the most load volume, then load weight, then box count.
 */

public class ReferenceRecursiveBruteForcePackager {

	static List<Placement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<Placement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new Placement(false));
		}
		return placements;
	}

	/**
	 * Pack box items into a container.
	 *
	 * @param container the container
	 * @param boxItems the box items; not changed
	 * @param interrupt interrupt
	 * @return the best result, possibly empty
	 * @throws PackagerInterruptedException if interrupted
	 */
	public ReferencePackResult pack(Container container, List<BoxItem> boxItems, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		ReferencePermutationRotationIterator iterator = ReferencePermutationRotationIterator
				.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withBoxItems(boxItems)
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.build();

		return pack(container, iterator, interrupt);
	}

	/**
	 * Pack the boxes of an iterator into a container. The iterator is reset, and left at its last permutation and rotations.
	 *
	 * @param container the container
	 * @param iterator the iterator, built for the container
	 * @param interrupt interrupt
	 * @return the best result, possibly empty
	 * @throws PackagerInterruptedException if interrupted
	 */
	public ReferencePackResult pack(Container container, ReferencePermutationRotationIterator iterator, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(iterator.length() == 0) {
			// 4.x: the adapter's attempt returned null for an iterator without boxes
			return new ReferencePackResult(iterator);
		}
		PointCalculator3DStack pointCalculator = new PointCalculator3DStack(iterator.length() + 1);
		pointCalculator.reset(1, 1, 1);

		// a previous attempt left the iterator at its last permutation and rotations
		iterator.reset();
		return pack(pointCalculator, getPlacements(iterator.length()), container, iterator, interrupt);
	}

	public ReferencePackResult pack(PointCalculator3DStack pointCalculator, List<Placement> stackPlacements, Container holder,
			ReferencePermutationRotationIterator iterator, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {

		ReferencePackResult bestResult = new ReferencePackResult(iterator);

		// optimization: compare pack results by looking only at count within the same permutation
		ReferencePackResult bestPermutationResult = new ReferencePackResult(iterator);

		// iterator over all permutations
		do {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}
			// iterate over all rotations
			bestPermutationResult.reset();

			do {
				int minStackableAreaIndex = iterator.getMinStackableAreaIndex(0);

				List<Point> points = packStackPlacement(pointCalculator, stackPlacements, iterator, holder, interrupt, minStackableAreaIndex);

				if(points.size() > bestPermutationResult.getSize()) {
					bestPermutationResult.setState(points, iterator.getState());
					if(points.size() == iterator.length()) {
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

				if(bestResult.isEmpty() || compare(bestResult, bestPermutationResult) < 0) {
					// switch the two results for one another
					ReferencePackResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}

			// search for the next permutation which actually
			// has a chance of affecting the result.

			if(permutationIndex == -1) {
				break;
			}
		} while (true);

		return bestResult;
	}

	/**
	 * Compare results with the 4.x comparator for results of one container: load volume, then load weight, then box
	 * count; more is better.
	 *
	 * @return 1 if the first result is better, -1 if the second result is better, otherwise 0
	 */
	protected static int compare(ReferencePackResult r1, ReferencePackResult r2) {
		// load volume - more is better
		if(r1.getLoadVolume() > r2.getLoadVolume()) {
			return 1;
		} else if(r1.getLoadVolume() < r2.getLoadVolume()) {
			return -1;
		}

		// load weight - more is better
		if(r1.getLoadWeight() > r2.getLoadWeight()) {
			return 1;
		} else if(r1.getLoadWeight() < r2.getLoadWeight()) {
			return -1;
		}

		// load count - more is better
		if(r1.getSize() > r2.getSize()) {
			return 1;
		} else if(r1.getSize() < r2.getSize()) {
			return -1;
		}
		return 0;
	}

	public List<Point> packStackPlacement(PointCalculator3DStack pointCalculator, List<Placement> placements, ReferencePermutationRotationIterator iterator,
			Container container,
			PackagerInterruptSupplier interrupt, int minStackableAreaIndex) throws PackagerInterruptedException {
		if(placements.isEmpty()) {
			return Collections.emptyList();
		}

		// pack as many items as possible from placementIndex
		int maxLoadWeight = container.getMaxLoadWeight();

		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		pointCalculator.setMinimumAreaAndVolumeLimit(iterator.getStackValue(minStackableAreaIndex).getArea(), iterator.getMinBoxVolume(0));

		// note: implemented as a recursive algorithm
		return packStackPlacement(pointCalculator, placements, iterator, maxLoadWeight, 0, interrupt, minStackableAreaIndex, Collections.emptyList());
	}

	private List<Point> packStackPlacement(
			PointCalculator3DStack pointCalculatorStack,
			List<Placement> placements,
			ReferencePermutationRotationIterator rotator,
			int maxLoadWeight,
			int placementIndex,
			PackagerInterruptSupplier interrupt,
			int minStackableAreaIndex,
			// optimize: pass best along so that we do not need to get points to known whether extracting the points is necessary
			List<Point> best
		) throws PackagerInterruptedException {
		if(interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		BoxStackValue stackValue = rotator.getStackValue(placementIndex);

		// the boxes placed so far are a result, also when this box exceeds the remaining load weight
		if(pointCalculatorStack.getStackIndex() > best.size()) {
			best = pointCalculatorStack.getPoints();
		}

		if(stackValue.getBox().getWeight() > maxLoadWeight) {
			return best;
		}

		Placement placement = placements.get(placementIndex);

		placement.setStackValue(stackValue);

		maxLoadWeight -= stackValue.getBox().getWeight();

		pointCalculatorStack.push();

		int currentPointsCount = pointCalculatorStack.size();

		for (int k = 0; k < currentPointsCount; k++) {
			Point point3d = pointCalculatorStack.get(k);

			if(!point3d.fits3D(stackValue)) {
				continue;
			}

			placement.setPoint(point3d);

			pointCalculatorStack.add(k, placement);

			if(placementIndex + 1 >= rotator.length()) {
				best = pointCalculatorStack.getPoints();

				break;
			}

			// should minimum area / volume be adjusted?
			int nextMinStackableAreaIndex;

			boolean minArea = placementIndex == minStackableAreaIndex;
			if(minArea) {
				nextMinStackableAreaIndex = rotator.getMinStackableAreaIndex(placementIndex + 1);

				pointCalculatorStack.setMinimumAreaAndVolumeLimit(rotator.getStackValue(nextMinStackableAreaIndex).getArea(), rotator.getMinBoxVolume(placementIndex + 1));
			} else {
				pointCalculatorStack.setMinimumVolumeLimit(rotator.getMinBoxVolume(placementIndex + 1));

				nextMinStackableAreaIndex = minStackableAreaIndex;
			}

			List<Point> points = packStackPlacement(
					pointCalculatorStack,
					placements,
					rotator,
					maxLoadWeight,
					placementIndex + 1,
					interrupt,
					nextMinStackableAreaIndex,
					best);

			if(points != null) {
				if(points.size() >= rotator.length()) {
					best = points;
					break;
				}

				if(best.size() < points.size()) {
					best = points;
				}
			}
			pointCalculatorStack.redo();
		}

		pointCalculatorStack.pop();

		return best;
	}

}
