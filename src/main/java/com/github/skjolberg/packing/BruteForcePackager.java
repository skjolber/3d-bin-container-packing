package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * This attempts a brute force approach, which is very demanding in terms of resources.
 * For use in scenarios with 'few' boxes, where the complexity of a 'few' can be measured
 * for a specific set of boxes and containers using
 * {@linkplain PermutationRotationIterator#countPermutations()} * {@linkplain PermutationRotationIterator#countRotations()}.
 * <br><br>
 * Thread-safe implementation. The input Boxes can be used by multiple threads at a time.
 */

public class BruteForcePackager extends Packager {

	private static class BruteForceResult implements PackResult {

		private PermutationRotationIterator rotator;
		private List<Placement> items;
		private Container container;

		private int count;
		private PermutationRotationState state;

		public BruteForceResult(PermutationRotationIterator rotator, List<Placement> items, Container container) {
			super();
			this.rotator = rotator;
			this.container = container;
			this.items = items;
		}

		public boolean isRemainder() {
			return count < items.size();
		}

		public Container getContainer() {
			container.clear();
			if(state == null ) {
				throw new RuntimeException();
			}
			rotator.setState(state);

			int result = pack(items, container, rotator, Long.MAX_VALUE, container, 0);
			if(result == count) {
				return container;
			}
			throw new IllegalArgumentException("Unexpected count " + result + ", expected " + count);
		}

		public void setState(PermutationRotationState state) {
			this.state = state;
		}

		public void setCount(int count) {
			this.count = count;
		}

		public int getCount() {
			return count;
		}

		@Override
		public boolean packsMoreBoxesThan(PackResult result) {
			// return true if 'this' is better:
			// - higher number of boxes
			// - lower volume
			// - lower max weight

			BruteForceResult bruteForceResult = (BruteForceResult)result;
			if(bruteForceResult.count < count) {
				return true;
			} else if(bruteForceResult.count == count) {
				// check volume (of container)
				if(bruteForceResult.container.getVolume() > container.getVolume()) {
					return true;
				} else if(bruteForceResult.container.getVolume() == container.getVolume()) {
					// check weight (max weight of container, suboptimal but quick)
					if(bruteForceResult.container.getWeight() > container.getWeight()) {
						return true;
					}
				}
			}

			return false;
		}

		public PermutationRotationIterator getRotator() {
			return rotator;
		}

		@Override
		public boolean isEmpty() {
			return count == 0;
		}

	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
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

		Container holder = new Container(container);

		BruteForceResult result = new BruteForceResult(rotator, placements, holder);

		// iterator over all permutations
		do {
			if(System.currentTimeMillis() > deadline) {
				return null;
			}
			// iterator over all rotations

			do {
				int count = pack(placements, holder, rotator, deadline, holder, 0);
				if(count == Integer.MIN_VALUE) {
					return null; // timeout
				} else {
					holder.clear();

					if(count == placements.size()) {
						if(accept()) {
							result.setCount(count);
							result.setState(rotator.getState());
							return result;
						}
					} else if(count > 0) {
						// continue search, but see if this is the best fit so far
						if(count > result.getCount()) {
							result.setCount(count);
							result.setState(rotator.getState());
						}
					}
				}
			} while(rotator.nextRotation());
		} while(rotator.nextPermutation());

		return result;
	}

	protected static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, long deadline, Container holder, int index) {
	    if(placements.isEmpty()) {
	    	return -1;
	    }
		Dimension remainingSpace = container;

		while(index < rotator.length()) {
			if(System.currentTimeMillis() > deadline) {
				// fit2d below might have returned due to deadline
				return Integer.MIN_VALUE;
			}

			Box box = rotator.get(index);
			if(box.getWeight() > holder.getFreeWeight()) {
				return index;
			}

			if(!remainingSpace.canHold2D(box)) {
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

			levelSpace.x = 0;
			levelSpace.y = 0;
			levelSpace.z = holder.getStackHeight();

			levelSpace.setParent(null);
			levelSpace.getRemainder().setParent(null);

			holder.addLevel();

			index = fit2D(rotator, index + 1, placements, holder, placement, deadline);

			// update remaining space
			remainingSpace = holder.getFreeSpace();
		}
		return index;
	}

	protected boolean accept() {
		return true;
	}

	protected static int fit2D(PermutationRotationIterator rotator, int index, List<Placement> placements, Container holder, Placement usedSpace, long deadline) {
		// add used space box now
		// there is up to 2 possible free spaces
		holder.add(usedSpace);

		if(index >= rotator.length()) {
			return index;
		}

		if(System.currentTimeMillis() > deadline) {
			return -1;
		}

		Box nextBox = rotator.get(index);
		if(nextBox.getWeight() > holder.getFreeWeight()) {
			return index;
		}

		Placement nextPlacement = placements.get(index);
		nextPlacement.setBox(nextBox);

		if(!isFreespace(usedSpace.getSpace(), usedSpace.getBox(), nextPlacement)) {
			// no additional boxes
			// just make sure the used space fits in the free space
			return index;
		}

		index++;
		// the correct space dimensions is copied into the next placement

		// attempt to fit in the remaining (usually smaller) space first

		// stack in the 'sibling' space - the space left over between the used box and the selected free space
		if(index < rotator.length()) {
			Space remainder = nextPlacement.getSpace().getRemainder();
			if(remainder.nonEmpty()) {
				Box box = rotator.get(index);

				if(box.getWeight() <= holder.getFreeWeight()) {
					if(box.fitsInside3D(remainder)) {
						Placement placement = placements.get(index);
						placement.setBox(box);

						index++;

						placement.getSpace().copyFrom(remainder);
						placement.getSpace().setParent(remainder);
						placement.getSpace().getRemainder().setParent(remainder);

						index = fit2D(rotator, index, placements, holder, placement, deadline);
					}
				}
			}
		}

		// fit the next box in the selected free space
		return fit2D(rotator, index, placements, holder, nextPlacement, deadline);
	}

	protected static boolean isFreespace(Space freespace, Box used, Placement target) {

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
		if(freespace.getWidth() >= used.getWidth() && freespace.getDepth() >= used.getDepth()) {

			// if B is empty, then it is sufficient to work with A and the other way around
			int b = (freespace.getWidth() - used.getWidth()) * freespace.getDepth();
			int a = freespace.getWidth() * (freespace.getDepth() - used.getDepth());

			// pick the one with largest footprint.
			if(b >= a) {
				if(b > 0 && b(freespace, used, target)) {
					return true;
				}

				return a > 0 && a(freespace, used, target);
			} else {
				if(a > 0 && a(freespace, used, target)) {
					return true;
				}

				return b > 0 && b(freespace, used, target);
			}
		}
		return false;
	}

	private static boolean a(Space freespace, Box used, Placement target) {
		if(target.getBox().fitsInside3D(freespace.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight())) {
			target.getSpace().copyFrom(
					freespace.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
					freespace.getX(), freespace.getY() + used.depth, freespace.getHeight()
				);
			target.getSpace().getRemainder().copyFrom(
					freespace.getWidth() - used.getWidth(), used.getDepth(), freespace.getHeight(),
					freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
				);
			target.getSpace().setParent(freespace);
			target.getSpace().getRemainder().setParent(freespace);

			return true;
		}
		return false;
	}

	private static boolean b(Space freespace, Box used, Placement target) {
		if(target.getBox().fitsInside3D(freespace.getWidth() - used.getWidth(), freespace.getDepth(), freespace.getHeight())) {
			// we have a winner
			target.getSpace().copyFrom(
					freespace.getWidth() - used.getWidth(), freespace.getDepth(), freespace.getHeight(),
					freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
					);

			target.getSpace().getRemainder().copyFrom(
					used.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
					freespace.getX(), freespace.getY()+ used.getDepth(), freespace.getZ()
					);

			target.getSpace().setParent(freespace);
			target.getSpace().getRemainder().setParent(freespace);
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
			public PackResult attempt(int i, long deadline) {
				return BruteForcePackager.this.pack(placements, containers.get(i), iterators[i], deadline);
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
				BruteForceResult bruteForceResult = (BruteForceResult)result;

				Container container = bruteForceResult.getContainer();

				if(bruteForceResult.isRemainder()) {
					int[] permutations = bruteForceResult.getRotator().getPermutations();
					List<Integer> p = new ArrayList<>(bruteForceResult.getCount());
					for(int i = 0; i < bruteForceResult.getCount(); i++) {
						p.add(permutations[i]);
					}
					for(PermutationRotationIterator it : iterators) {
						if(it == bruteForceResult.getRotator()) {
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
				BruteForceResult bruteForceResult = (BruteForceResult)result;
				return placements.size() > bruteForceResult.getCount();
			}

		};
	}

}
