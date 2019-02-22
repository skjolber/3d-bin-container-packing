package com.github.skjolberg.packing;

import com.github.skjolberg.packing.impl.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;


/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * This attempts a brute force approach, which is very demanding in terms of resources. For use in scenarios with 'few'
 * boxes, where the complexity of a 'few' can be measured for a specific set of boxes and containers using {@linkplain
 * PermutationRotationIterator#countPermutations()} * {@linkplain PermutationRotationIterator#countRotations()}.
 * <br><br>
 * Thread-safe implementation. The input Boxes can be used by multiple threads at a time.
 */

public class BruteForcePackager extends Packager {

	/**
	 * Constructor
	 *
	 * @param containers   list of containers
	 * @param rotate3D     whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a
	 *                     container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public BruteForcePackager(List<Container> containers, boolean rotate3D, boolean binarySearch) {
		super(containers, rotate3D, binarySearch);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 */

	public BruteForcePackager(List<Container> containers) {
		this(containers, true, true);
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, long deadline) {
		return pack(placements, container, rotator, deadLinePredicate(deadline));
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, long deadline, AtomicBoolean interrupt) {
		return pack(placements, container, rotator, () -> deadlineReached(deadline) || interrupt.get());
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, BooleanSupplier interrupt) {

		Container holder = new Container(container);

		BruteForceResult result = new BruteForceResult(rotator, placements, holder);

		// iterator over all permutations
		do {
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations

			do {
				int count = pack(placements, holder, rotator, holder, 0, interrupt);
				if (count == Integer.MIN_VALUE) {
					return null; // timeout
				} else {
					holder.clear();

					if (count == placements.size()) {
						if (accept()) {
							result.setCount(count);
							result.setState(rotator.getState());
							return result;
						}
					} else if (count > 0) {
						// continue search, but see if this is the best fit so far
						if (count > result.getCount()) {
							result.setCount(count);
							result.setState(rotator.getState());
						}
					}
				}
			} while (rotator.nextRotation());
		} while (rotator.nextPermutation());

		return result;
	}

	public static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, long deadline, Container holder, int index) {
		return pack(placements, container, rotator, holder, index, deadLinePredicate(deadline));
	}

	public static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, long deadline, Container holder, int index, AtomicBoolean interrupt) {
		return pack(placements, container, rotator, holder, index, () -> deadlineReached(deadline) || interrupt.get());
	}

	public static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, Container holder, int index, BooleanSupplier interrupt) {
		if (placements.isEmpty()) {
			return -1;
		}
		Dimension remainingSpace = container;

		while (index < rotator.length()) {
			if (interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				return Integer.MIN_VALUE;
			}

			Box box = rotator.get(index);
			if (box.getWeight() > holder.getFreeWeight()) {
				return index;
			}

			if (!remainingSpace.canHold2D(box)) {
				return index;
			}

			Placement placement = placements.get(index);
			Space levelSpace = placement.getSpace();
			levelSpace.width = remainingSpace.getWidth();
			levelSpace.depth = remainingSpace.getDepth();

			// the LAFF allocates a height per level equal to the given the current box,
			// but here allow for use of all of the remaining space; the selection of boxes is fixed
			// the result will be constrained to actual use.
			levelSpace.height = remainingSpace.getHeight();

			placement.setBox(box);

			levelSpace.setX(0);
			levelSpace.setY(0);
			levelSpace.setZ(holder.getStackHeight());

			levelSpace.setParent(null);
			levelSpace.getRemainder().setParent(null);

			holder.addLevel();

			index = fit2D(rotator, index + 1, placements, holder, placement, interrupt);

			// update remaining space
			remainingSpace = holder.getFreeSpace();
		}
		return index;
	}

	protected boolean accept() {
		return true;
	}

	private static int fit2D(PermutationRotationIterator rotator, int index, List<Placement> placements, Container holder, Placement usedSpace, BooleanSupplier interrupt) {
		// add used space box now
		// there is up to 2 possible free spaces
		holder.add(usedSpace);

		if (index >= rotator.length()) {
			return index;
		}

		if (interrupt.getAsBoolean()) {
			return -1;
		}

		Box nextBox = rotator.get(index);
		if (nextBox.getWeight() > holder.getFreeWeight()) {
			return index;
		}

		Placement nextPlacement = placements.get(index);
		nextPlacement.setBox(nextBox);

		if (!isFreeSpace(usedSpace.getSpace(), usedSpace.getBox(), nextPlacement)) {
			// no additional boxes
			// just make sure the used space fits in the free space
			return index;
		}

		index++;
		// the correct space dimensions is copied into the next placement

		// attempt to fit in the remaining (usually smaller) space first

		// stack in the 'sibling' space - the space left over between the used box and the selected free space
		if (index < rotator.length()) {
			Space remainder = nextPlacement.getSpace().getRemainder();
			if (remainder.nonEmpty()) {
				Box box = rotator.get(index);

				if (box.getWeight() <= holder.getFreeWeight()) {
					if (box.fitsInside3D(remainder)) {
						Placement placement = placements.get(index);
						placement.setBox(box);

						index++;

						placement.getSpace().copyFrom(remainder);
						placement.getSpace().setParent(remainder);
						placement.getSpace().getRemainder().setParent(remainder);

						index = fit2D(rotator, index, placements, holder, placement, interrupt);
					}
				}
			}
		}

		// fit the next box in the selected free space
		// double check that there is still free weight
		if (holder.getFreeWeight() >= nextBox.getWeight()) {
			return fit2D(rotator, index, placements, holder, nextPlacement, interrupt);
		}
		return index;
	}

	private static boolean isFreeSpace(Space freeSpace, Box used, Placement target) {

		// Two free spaces, on each rotation of the used space.
		// Height is always the same, used box is assumed within free space height.
		// First:
		// ........................  ........................  .............
		// .                      .  .                      .  .           .
		// .                      .  .                      .  .           .
		// .          A           .  .          A           .  .           .
		// .                      .  .                      .  .           .
		// .                B     .  .                      .  .    B      .
		// ............           .  ........................  .           .
		// .          .           .                            .           .
		// .          .           .                            .           .
		// ........................                            .............
		//
		// So there is always a 'big' and a 'small' leftover area (the small is not shown).
		if (freeSpace.getWidth() >= used.getWidth() && freeSpace.getDepth() >= used.getDepth()) {

			// if B is empty, then it is sufficient to work with A and the other way around
			int b = (freeSpace.getWidth() - used.getWidth()) * freeSpace.getDepth();
			int a = freeSpace.getWidth() * (freeSpace.getDepth() - used.getDepth());

			// pick the one with largest footprint.
			if (b >= a) {
				if (b > 0 && b(freeSpace, used, target)) {
					return true;
				}

				return a > 0 && a(freeSpace, used, target);
			} else {
				if (a > 0 && a(freeSpace, used, target)) {
					return true;
				}

				return b > 0 && b(freeSpace, used, target);
			}
		}
		return false;
	}

	private static boolean a(Space freeSpace, Box used, Placement target) {
		if (target.getBox().fitsInside3D(freeSpace.getWidth(), freeSpace.getDepth() - used.getDepth(), freeSpace.getHeight())) {
			target.getSpace().copyFrom(
					freeSpace.getWidth(), freeSpace.getDepth() - used.getDepth(), freeSpace.getHeight(),
					freeSpace.getX(), freeSpace.getY() + used.depth, freeSpace.getZ()
			);
			target.getSpace().getRemainder().copyFrom(
					freeSpace.getWidth() - used.getWidth(), used.getDepth(), freeSpace.getHeight(),
					freeSpace.getX() + used.getWidth(), freeSpace.getY(), freeSpace.getZ()
			);
			target.getSpace().setParent(freeSpace);
			target.getSpace().getRemainder().setParent(freeSpace);

			return true;
		}
		return false;
	}

	private static boolean b(Space freeSpace, Box used, Placement target) {
		if (target.getBox().fitsInside3D(freeSpace.getWidth() - used.getWidth(), freeSpace.getDepth(), freeSpace.getHeight())) {
			// we have a winner
			target.getSpace().copyFrom(
					freeSpace.getWidth() - used.getWidth(), freeSpace.getDepth(), freeSpace.getHeight(),
					freeSpace.getX() + used.getWidth(), freeSpace.getY(), freeSpace.getZ()
			);

			target.getSpace().getRemainder().copyFrom(
					used.getWidth(), freeSpace.getDepth() - used.getDepth(), freeSpace.getHeight(),
					freeSpace.getX(), freeSpace.getY() + used.getDepth(), freeSpace.getZ()
			);

			target.getSpace().setParent(freeSpace);
			target.getSpace().getRemainder().setParent(freeSpace);
			return true;
		}
		return false;
	}

	@Override
	protected Adapter adapter() {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach

		return new Adapter() {

			private List<Placement> placements;
			private PermutationRotationIterator[] iterators;
			private List<Container> containers;

			@Override
			public PackResult attempt(int i, BooleanSupplier interrupt) {
				return BruteForcePackager.this.pack(placements, containers.get(i), iterators[i], interrupt);
			}

			@Override
			public void initialize(List<BoxItem> boxes, List<Container> containers) {
				this.containers = containers;
				PermutationRotation[] rotations = PermutationRotationIterator.toRotationMatrix(boxes, rotate3D);
				int count = 0;
				for (PermutationRotation permutationRotation : rotations) {
					count += permutationRotation.getCount();
				}

				placements = getPlacements(count);

				iterators = new PermutationRotationIterator[containers.size()];
				for (int i = 0; i < containers.size(); i++) {
					iterators[i] = new PermutationRotationIterator(containers.get(i), rotations);
				}
			}

			@Override
			public Container accepted(PackResult result) {
				BruteForceResult bruteForceResult = (BruteForceResult) result;

				Container container = bruteForceResult.getContainer();

				if (bruteForceResult.isRemainder()) {
					int[] permutations = bruteForceResult.getRotator().getPermutations();
					List<Integer> p = new ArrayList<>(bruteForceResult.getCount());
					for (int i = 0; i < bruteForceResult.getCount(); i++) {
						p.add(permutations[i]);
					}
					for (PermutationRotationIterator it : iterators) {
						if (it == bruteForceResult.getRotator()) {
							it.removePermutations(bruteForceResult.getCount());
						} else {
							it.removePermutations(p);
						}
					}
					placements = placements.subList(bruteForceResult.getCount(), this.placements.size());
				} else {
					placements = Collections.emptyList();
				}

				return container;
			}

			@Override
			public boolean hasMore(PackResult result) {
				BruteForceResult bruteForceResult = (BruteForceResult) result;
				return placements.size() > bruteForceResult.getCount();
			}

		};
	}

}
