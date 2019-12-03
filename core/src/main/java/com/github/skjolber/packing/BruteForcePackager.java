package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.impl.*;


/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * This attempts a brute force approach, which is very demanding in terms of resources. For use in scenarios with 'few'
 * boxes, where the complexity of a 'few' can be measured for a specific set of boxes and containers using {@linkplain
 * DefaultPermutationRotationIterator#countPermutations()} * {@linkplain DefaultPermutationRotationIterator#countRotations()}.
 * <br><br>
 * Thread-safe implementation. The input Boxes can be used by multiple threads at a time.
 */

public class BruteForcePackager extends Packager {

	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}
	
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

			Level level = holder.addLevel();

			index = fit2D(rotator, index + 1, placements, holder, placement, interrupt);

			if(index == -1) {
				return index;
			}
			
			// set level space height to the actual used height
			levelSpace.setHeight(level.getHeight());
			
			if (index < rotator.length()) {
				// now that the level height is know, see whether we can use space between level roof and box roof
				
				index = fitWithin(level, levelSpace, rotator, index, placements, holder, interrupt);
				if(index == -1) {
					return index;
				}
			}
			
			// update remaining space
			remainingSpace = holder.getFreeLevelSpace();
		}
		return index;
	}

	protected boolean accept() {
		return true;
	}

	protected static int fit2D(PermutationRotationIterator rotator, int index, List<Placement> placements, Container holder, Placement usedSpace, BooleanSupplier interrupt) {
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

		boolean room = isFreeSpace(usedSpace.getSpace(), usedSpace.getBox(), nextPlacement);
		if (room) {
			index++;
			// note to self: pack in the primary space first, if not then the returned index
			// might be incorrect (if the weight constraint is reached while packaging in the remainder).
			//
			int fromIndex = index;
			// the correct space dimensions is copied into the next placement
			// fit the next box in the selected free space
			// fit in primary
			index = fit2D(rotator, index, placements, holder, nextPlacement, interrupt);
			if(index == -1) {
				return -1;
			} else if(index >= rotator.length()) {
				return index;
			}
			
			// attempt to fit in the remaining (usually smaller) space
			Placement remainderPlacement = placements.get(index);
			
			Box box = rotator.get(index);

			Space remainder = nextPlacement.getSpace().getRemainder();
			if (box.getWeight() <= holder.getFreeWeight()) {
				
				Space nextSpace = null;
				if (box.fitsInside3D(remainder)) {
					nextSpace = remainder;
				} else {
					// is it possible to expand the remainder / secondary
					// with space not used in the primary space?
					Space space = usedSpace.getSpace();
					if(remainder.getX() == space.getX()) {
						remainder.setWidth(space.getWidth());
					} else {
						remainder.setDepth(space.getDepth());
					}
					if (box.fitsInside3D(remainder)) {
						// cut out the area which is already in use, leaving two edges
						Space depthRemainder = remainder; //
						Space widthRemainder = new Space(remainder);
	
						for(int i = fromIndex; i < index; i++) {
							Placement placement = placements.get(i);
							
							if(widthRemainder.intersectsY(placement) && widthRemainder.intersectsX(placement)) {
								// there is overlap, subtract area
								widthRemainder.subtractX(placement);
								
								if (!box.fitsInside3D(widthRemainder)) {
									break;
								}
							}
						}
	
						for(int i = fromIndex; i < index; i++) {
							Placement placement = placements.get(i);
							
							if(depthRemainder.intersectsY(placement) && depthRemainder.intersectsX(placement)) {
								// there is overlap, subtract area
								depthRemainder.subtractY(placement);
								
								if (!box.fitsInside3D(depthRemainder)) {
									break;
								}
							}
						}
	
						// see if the box fits now
						if(box.fitsInside3D(widthRemainder)) {
							nextSpace = widthRemainder;
						}
						
						if(box.fitsInside3D(depthRemainder) && (nextSpace == null || depthRemainder.getVolume() > nextSpace.getVolume())) {
							nextSpace = depthRemainder;
						}
					}
				}					
				if(nextSpace != null) {
					remainderPlacement.setBox(box);

					index++;

					remainderPlacement.getSpace().copyFrom(nextSpace);
					remainderPlacement.getSpace().setParent(nextSpace);
					remainderPlacement.getSpace().getRemainder().setParent(nextSpace);

					index = fit2D(rotator, index, placements, holder, remainderPlacement, interrupt);
					if(index == -1) {
						return -1;
					}
				}
			}
		}  else {
			// no additional boxes can be placed along the level floor
		}

		return index;
	}
	
	protected static int fitWithin(Level level, Space levelSpace, PermutationRotationIterator rotator, int index, List<Placement> placements, Container holder, BooleanSupplier interrupt) {
		// also use the space above the placed box, if any.
		
		// level size increases as this loop runs
		for(int i = 0; i < level.size() && index < rotator.length(); i++) { 
			
			Placement placement = level.get(i);
			Box placedBox = placement.getBox();
			Space placedSpace = placement.getSpace();
			
			int height = (levelSpace.getZ() + levelSpace.getHeight()) - (placedSpace.getZ() + placedBox.getHeight());
			if(height > 0) {
				Box nextBox = rotator.get(index);
				if (nextBox.getWeight() > holder.getFreeWeight()) {
					return index;
				}
				
				Space abovePlacedBox = new Space(
						placedBox.getWidth(), 
						placedBox.getDepth(), 
						height,
						levelSpace.getX(),
						levelSpace.getY(),
						placedSpace.getZ() + placedBox.getHeight()
						);
				
				if (nextBox.fitsInside3D(abovePlacedBox)) {
					Placement nextPlacement = placements.get(index);
					nextPlacement.setBox(nextBox);

					index++;

					nextPlacement.getSpace().copyFrom(abovePlacedBox);
					nextPlacement.getSpace().setParent(levelSpace);
					nextPlacement.getSpace().getRemainder().setParent(levelSpace);

					index = fit2D(rotator, index, placements, holder, nextPlacement, interrupt);
					if(index == -1) {
						return -1;
					}
				}
			}
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
	protected Adapter adapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new BruteForceAdapter(boxes, containers, interrupt);
	}
	
	private class BruteForceAdapter implements Adapter {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
	
		private List<Placement> placements;
		private DefaultPermutationRotationIterator[] iterators;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public BruteForceAdapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			PermutationRotation[] rotations = DefaultPermutationRotationIterator.toRotationMatrix(boxes, rotate3D);
			int count = 0;
			for (PermutationRotation permutationRotation : rotations) {
				count += permutationRotation.getCount();
			}

			placements = getPlacements(count);

			iterators = new DefaultPermutationRotationIterator[containers.size()];
			for (int i = 0; i < containers.size(); i++) {
				iterators[i] = new DefaultPermutationRotationIterator(containers.get(i), rotations);
			}
			this.interrupt = interrupt;
		}

		@Override
		public PackResult attempt(int i) {
			return BruteForcePackager.this.pack(placements, containers.get(i), iterators[i], interrupt);
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
	}

}
