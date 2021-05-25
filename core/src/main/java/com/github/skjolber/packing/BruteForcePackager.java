package com.github.skjolber.packing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.impl.*;
import com.github.skjolber.packing.impl.deadline.BooleanSupplierBuilder;


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

	public BruteForcePackager(List<Container> containers, boolean rotate3D, boolean binarySearch, int checkpointsPerDeadlineCheck) {
		super(containers, rotate3D, binarySearch, checkpointsPerDeadlineCheck);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 */

	public BruteForcePackager(List<Container> containers) {
		this(containers, true, true, 1);
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(placements, container, rotator, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(placements, container, rotator, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public BruteForceResult pack(List<Placement> placements, Container container, PermutationRotationIterator rotator, BooleanSupplier interrupt) {

		Container holder = container.clone();

		BruteForceResult result = new BruteForceResult(rotator, placements, holder);

		// iterator over all permutations
		do {
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			int index = 0;
			do {
				int count = pack(placements, holder.getFreeLevelSpace(), rotator, holder, index, interrupt);
				if (count == Integer.MIN_VALUE) {
					return null; // timeout
				} 
				if (count == placements.size()) {
					if (accept(count)) {
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

				int diff = rotator.nextRotation();
				if(diff == -1) {
					// no more rotations, continue to next permutation
					holder.clear();

					break;
				}
				
				if(count >= 2 && diff >= 2) { // see whether we can reuse some previous calculations
					index = holder.clearLevelsForBoxes(Math.min(diff, count));
				} else {
					index = 0;
					holder.clear();
				}
				
			} while (true);
		} while (rotator.nextPermutation() != -1);

		return result;
	}

	public static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, long deadline, int checkpointsPerDeadlineCheck, Container holder, int index) {
		return pack(placements, container, rotator, holder, index, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public static int pack(List<Placement> placements, Dimension container, PermutationRotationIterator rotator, long deadline, int checkpointsPerDeadlineCheck, Container holder, int index, BooleanSupplier interrupt) {
		return pack(placements, container, rotator, holder, index, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public static int pack(List<Placement> placements, Dimension remainingSpace, PermutationRotationIterator rotator, Container holder, int index, BooleanSupplier interrupt) {
		if (placements.isEmpty()) {
			return -1;
		}

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

			if(index == -1) { // interrupted
				return index;
			}
			
			// set level space height to the actual used height
			levelSpace.setHeight(level.getHeight());
			
			if (index < rotator.length()) {
				// now that the level height is know, see whether we can use space between level roof and box roof
				
				index = fitWithin(level, levelSpace, rotator, index, placements, holder, interrupt);
				if(index == -1) {  // interrupted
					return index;
				}
			}
			
			// update remaining space
			remainingSpace = holder.getFreeLevelSpace();
		}
		return index;
	}

	protected boolean accept(int count) {
		return count > 0;
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

		Box primaryBox = rotator.get(index);
		if (primaryBox.getWeight() > holder.getFreeWeight()) {
			return index;
		}

		Placement primaryPlacement = placements.get(index);
		primaryPlacement.setBox(primaryBox);

		Space parentSpace = usedSpace.getSpace();

		boolean room = isFreeSpace(parentSpace, usedSpace.getBox(), primaryPlacement);
		if (room) {
			int fromIndex = index;
			
			index++;
			// note to self: pack in the primary space first, if not then the returned index
			// might be incorrect (if the weight constraint is reached while packaging in the remainder).
			//
			// the correct space dimensions is copied into the next placement
			// fit the next box in the selected free space
			// fit in primary
			index = fit2D(rotator, index, placements, holder, primaryPlacement, interrupt);
			
			if(index == -1) { // interrupted
				return -1;
			} else if(index >= rotator.length()) {
				return index;
			}
			
			// attempt to fit in the remaining (usually smaller) space
			Placement remainderPlacement = placements.get(index);
			
			Box remainderBox = rotator.get(index);

			Space remainder = primaryPlacement.getSpace().getRemainder();
			if (remainderBox.getWeight() <= holder.getFreeWeight()) {
				
				Space nextSpace = null;
				if (remainderBox.fitsInside3D(remainder)) {
					nextSpace = remainder;
				} else {
					// is it possible to expand the remainder / secondary
					// with space not used in the primary space?
					//
					// ........................   ........................          ..................
					// .                      .   .                      .          .                .
					// .          C           .   .         C            .          .                .
					// .                      .   .                      .          .                .
					// .......                .   ........................          .                .
					// .     .       D        .                                     .        D       .
					// .     .                .                                     .                .
					// .     .                .                                     .                .
					// .     .                .                                     .                .
					// ........................                                     ..................
					//
					// With remainders shown as 'r' and the expansion area as double quoted:
					//
					//                           .........................  ..........................
					//                           .      .                .  .       .                .
					//                           .  C'  .      C''       .  .  r1   .     D''        .
					//                           .      .                .  .       .                .
					//                           .........................  ..........................
					//      depth                       .                .          .                .
					//       ^                          .                .          .                .
					//       |                          .       r2       .          .      D'        .
					//       |                          .                .          .                .
					//        ----> width               ..................          ..................
					//
					// So if the remainder is expanded with the double quoted area,  
					// the maximum available space for the remainder is the 'sibling' free space
					// which is known to be non-empty since there is a remainder.
					// 
					// Cutting out the area actually in use will be done in a bounding box way,
					// separately for width and depth.
					// 
					// Cutting stop conditions:
					// 
					// C:
					//  - depth cuts: depth equals to remainder depth
					//  - width cuts: width equal to zero
					//
					// D:
					//  - depth cuts: depth equals to zero
					//  - width cuts: width equal to remainder width
					//
					
					Space expandedRemainder = new Space(remainder); 
					if(expandedRemainder.getX() == parentSpace.getX()) { // r1
						
						expandedRemainder.setWidth(parentSpace.getWidth()); // expand r1 with D''
					} else { // r2
						expandedRemainder.setDepth(parentSpace.getDepth()); // expand r2 with C''
					}
					if (remainderBox.fitsInside3D(expandedRemainder)) {
						// cut out the area which is already in use, leaving two edges
						Space depthRemainder = expandedRemainder; //
						Space widthRemainder = new Space(expandedRemainder);
	
						// subtract spaced used in primary (fromIndex to index(exclusive))
						// 
						// Width:
						//
						// ......... ..     ...........
						// .         .      .////|    .
						// .         .      .////|    .
						// .         .  ->  .////|    .
						// .         .      .////|    .
						// .         .      .////|    .
						// ...........      ...........
						
						for(int i = fromIndex; i < index; i++) {
							Placement placement = placements.get(i);
							if(widthRemainder.intersectsY(placement) && widthRemainder.intersectsX(placement)) {
								// there is overlap, subtract area
								widthRemainder.subtractX(placement);

								if (!remainderBox.fitsInside3D(widthRemainder)) {
									break;
								}
							}
						}

						// 
						// Depth:
						//
						// ...........      ...........
						// .         .      ./////////.
						// .         .      ./////////.
						// .         .  ->  .---------.
						// .         .      .         .
						// .         .      .         .
						// ...........      ...........

						for(int i = fromIndex; i < index; i++) {
							Placement placement = placements.get(i);
							if(depthRemainder.intersectsY(placement) && depthRemainder.intersectsX(placement)) {
								// there is overlap, subtract area
								depthRemainder.subtractY(placement);
								
								if (!remainderBox.fitsInside3D(depthRemainder)) {
									break;
								}
							}
						}
	
						// see if the box fits now
						boolean fitsInWidthRemainder = remainderBox.fitsInside3D(widthRemainder);
						boolean fitsInDepthRemainder = remainderBox.fitsInside3D(depthRemainder);
							
						if(fitsInDepthRemainder && (!fitsInWidthRemainder || depthRemainder.getVolume() > widthRemainder.getVolume())) {
							nextSpace = depthRemainder;
							
							// subtract primary space size
							Space primary = primaryPlacement.getSpace();
							primary.setWidth(primary.getWidth() - (depthRemainder.getWidth() - remainder.getWidth()));
						} else if(fitsInWidthRemainder) {
							nextSpace = widthRemainder;
							
							// subtract primary space size
							Space primary = primaryPlacement.getSpace();
							primary.setDepth(primary.getDepth() - (widthRemainder.getDepth() - remainder.getDepth()));
						}
								
					}
				}					
				if(nextSpace != null) {
					remainderPlacement.setBox(remainderBox);

					index++;

					remainderPlacement.getSpace().copyFrom(nextSpace);
					remainderPlacement.getSpace().setParent(nextSpace);
					remainderPlacement.getSpace().getRemainder().setParent(nextSpace);

					index = fit2D(rotator, index, placements, holder, remainderPlacement, interrupt);
					if(index == -1) { // interrupted
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
						placedSpace.getX(),
						placedSpace.getY(),
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
			
			if(iterators[i].length() == 0) {
				return EMPTY_PACK_RESULT;
			}
			
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
