package com.github.skjolber.packing.old;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.impl.*;
import com.github.skjolber.packing.impl.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.impl.points.ExtremePoints;
import com.github.skjolber.packing.impl.points.Point;
import com.github.skjolber.packing.points2d.ExtremePoints2D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public class LargestAreaFitFirstPackager2 extends Packager {

	private static final class Context {
		
		int boxIndex;
		int pointIndex;
		
		int dz = 0;
		int dx = 0;
		int dy = 0;
		
	}
	
	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	private final boolean footprintFirst;

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 */
	public LargestAreaFitFirstPackager2(List<Container> containers) {
		this(containers, true, true, true, 1);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public LargestAreaFitFirstPackager2(List<Container> containers, boolean rotate3D, boolean footprintFirst, boolean binarySearch, int checkpointsPerDeadlineCheck) {
		super(containers, rotate3D, binarySearch, checkpointsPerDeadlineCheck);

		this.footprintFirst = footprintFirst;
	}

	/**
	 *
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param containerProducts list of boxes to fit in a container.
	 * @param targetContainer the container to fit within
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer, long deadline, int checkpointsPerDeadlineCheck, BooleanSupplier interrupt) {
		return pack(containerProducts, targetContainer, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public LAFFResult pack(List<Box> containerProducts, Container targetContainer,  BooleanSupplier interrupt) {
		Container holder = targetContainer.clone();

		ExtremePoints2D<Placement2D> extremePoints = new ExtremePoints2D<>(targetContainer.getWidth(), targetContainer.getDepth());
		
		Context context = new Context();
		
		while(!containerProducts.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			// choose the box with the largest surface area, that fits
			// if the same then the one with minimum height
			findBestFirstBox(containerProducts, holder.getFreeWeight(), holder.getFreeLevelHeight(), extremePoints, context);
			
			if(context.boxIndex == -1) {
				break;
			}

			// current box should have the optimal orientation already
			// create a space which holds the full level
			holder.addLevel();
			
			Box box = containerProducts.get(context.boxIndex);
			context.dx = holder.getWidth();
			context.dy = holder.getDepth();
			context.dz = 0;

			if(!fit2D(containerProducts, holder, box.getHeight(), extremePoints, context, interrupt)) {
				return null;
			}
		}

		return new LAFFResult(containerProducts, holder);
	}

	protected void findBestFirstBox(List<Box> containerProducts, int freeWeight, int freeHeight, ExtremePoints extremePoints, Context context) {
		// use a special case for boxes with full height
		context.boxIndex = -1;

		List<Point> points = extremePoints.getValues();
		
		boolean fullHeight = false;
		for (int i = 0; i < containerProducts.size(); i++) {
			Box box = containerProducts.get(i);

			for (int j = 0; j < points.size(); j++) {
				Point point = points.get(j);
				boolean fits;
				if(rotate3D) {
					fits = box.rotateLargestFootprint3D(point.getDx(), point.getDy(), freeHeight);
				} else {
					fits = box.fitRotate2D(point.getDx(), point.getDy());
				}
				if(fits && box.getWeight() <= freeWeight) {
					if(context.boxIndex == -1) {
						context.boxIndex = i;
						context.pointIndex = j;
						
						fullHeight = box.getHeight() == freeHeight;
					} else {
						if(fullHeight) {
							if(box.getHeight() == freeHeight) {
								if(containerProducts.get(context.boxIndex).getFootprint() < box.getFootprint()) {
									context.boxIndex = i;
									context.pointIndex = j;
								}
							}
						} else {
							if(box.getHeight() == freeHeight) {
								fullHeight = true;
	
								context.boxIndex = i;
								context.pointIndex = j;
							} else if(footprintFirst) {
								if(containerProducts.get(context.boxIndex).getFootprint() < box.getFootprint()) {
									context.boxIndex = i;
									context.pointIndex = j;
								} else if(containerProducts.get(context.boxIndex).getFootprint() == box.getFootprint() && containerProducts.get(context.boxIndex).getHeight() < box.getHeight()) {
									context.boxIndex = i;
									context.pointIndex = j;
								}
							} else {
								if(containerProducts.get(context.boxIndex).getHeight() < box.getHeight()) {
									context.boxIndex = i;
									context.pointIndex = j;
								} else if(containerProducts.get(context.boxIndex).getHeight() == box.getHeight() && containerProducts.get(context.boxIndex).getFootprint() < box.getFootprint()) {
									context.boxIndex = i;
									context.pointIndex = j;
								}
							}
						}
					}
				}
			}
		}
		
	}

	/**
	 * Remove from list, more explicit implementation than {@linkplain List#remove} with no equals.
	 * @param containerProducts list of products
	 * @param currentBox item to remove
	 */
	private void removeIdentical(List<Box> containerProducts, Box currentBox) {
		for(int i = 0; i < containerProducts.size(); i++) {
			if(containerProducts.get(i) == currentBox) {
				containerProducts.remove(i);

				return;
			}
		}
		throw new IllegalArgumentException();
	}

	/**
	 * Fit in two dimensions
	 * 
	 * @param containerProducts products to fit
	 * @param holder target container
	 * @param usedSpace space to subtract
	 * @param freeSpace available space
	 * @param interrupt interrupt
	 * @return false if interrupted
	 */

	protected boolean fit2D(List<Box> containerProducts, Container holder, int freeHeight, ExtremePoints extremePoints, Context context, BooleanSupplier interrupt) {

		Box box = containerProducts.remove(context.boxIndex);
		Point point = extremePoints.getPoint(context.pointIndex);
		
		Space space = new Space(box.getWidth(), box.getDepth(), box.getHeight(), context.dx, context.dy, context.dz);
		holder.add(new Placement(space, box));

		if(containerProducts.isEmpty()) {
			// no additional boxes
			return true;
		}

		if(interrupt.getAsBoolean()) {
			return false;
		}

		Space[] spaces = getFreespaces(freeSpace, usedSpace);

		Placement primaryPlacement = getBestBoxAndSpace(containerProducts, spaces, holder.getFreeWeight());
		if(primaryPlacement == null) {
			// no additional boxes along the level floor (x,y)
			// just make sure the used space fits in the free space
			usedSpace.fitRotate2D(freeSpace);
		} else {
			// check whether the selected free space requires the used space box to be rotated
			if(primaryPlacement.getSpace() == spaces[2] || primaryPlacement.getSpace() == spaces[3]) {
				// the desired space implies that we rotate the used space box
				usedSpace.rotate2D();
			}

			// holder.validateCurrentLevel(); // uncomment for debugging

			removeIdentical(containerProducts, primaryPlacement.getBox());

			// unused dual / remaining space
			Level currentLevel = holder.currentLevel();
			int count = currentLevel.size();

			if(!fit2D(containerProducts, holder, primaryPlacement.getBox(), primaryPlacement.getSpace(), interrupt)) {
				return false; // time is up
			}

			// stack in the 'sibling' space - the space left over between the used box and the selected free space
			Space remainder = primaryPlacement.getSpace().getRemainder();
			if(remainder.nonEmpty()) {
				Box remainderBox = getBestBoxForSpace(containerProducts, remainder, holder.getFreeWeight());
				if(remainderBox != null) {
					removeIdentical(containerProducts, remainderBox);

					// fit in remainder
					if(!fit2D(containerProducts, holder, remainderBox, remainder, interrupt)) {
						return false; // time is up
					}
				} else {
					// is it possible to expand the remainder / secondary
					// with space not used in the primary space?
					//
					// No rotation:
					// ........................  ........................                .............
					// .                      .  .                      .                .           .
					// .                      .  .                      .                .           .
					// .          A           .  .          A           .                .           .
					// .                      .  .                      .                .           .
					// .                B     .  .                      .                .    B      .
					// ............           .  ........................                .           .
					// .          .           .                                          .           .
					// .          .           .                                          .           .
					// ........................                                          .............
					//
					// With remainders shown as 'r' and the expansion area as double quoted: 
					//
					//                           ........................    .........................
					//                           .          .           .    .           .           .
					//                           .          .           .    .           .           .
					//                           .     A'   .    A''    .    .    r      .   B''     .
					//                           .          .           .    .           .           .
					//    depth                  .          .           .    .           .           .
					//      ^                    ........................    .........................
					//      |                               .           .                .           .
					//      |                               .    r      .                .    B'     .
					//       ---> width                     .............                .............
					//
					// Rotation (placed box is rotated 90 degrees):
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
					//                           .  C'  .      C''       .  .  r    .     D''        .
					//                           .      .                .  .       .                .
					//                           .........................  ..........................
					//      depth                       .                .          .                .
					//       ^                          .                .          .                .
					//       |                          .       r        .          .      D'        .
					//       |                          .                .          .                .
					//        ----> width               ..................          ..................
					//
					// So if all remainders are expanded with the double quoted area,  
					// the maximum available space for the remainder is the 'sibling' free space
					// which is known to be non-empty since there is a remainder.
					// 
					// Cutting out the area actually in use will be done in a bounding box way,
					// separately for width and depth.
					// 
					// Cutting stop conditions (most easily seen in C and D):
					// 
					// C:
					//  - depth cuts: depth equals to remainder depth
					//  - width cuts: width equal to zero
					//
					// D:
					//  - depth cuts: depth equals to zero
					//  - width cuts: width equal to remainder width
					//

					int siblingIndex = getSiblingIndex(spaces, primaryPlacement);
					Space sibling = spaces[siblingIndex];

					// cut out the area which is already in use, leaving two edges
					Space depthRemainder = sibling; //
					Space widthRemainder = new Space(sibling); // TODO reuse remainder space object for improved performance

					int widthConstraint;
					if(siblingIndex % 2 == 0) { // A and C
						widthConstraint = 0;
					} else {
						widthConstraint = remainder.getWidth();
					}
					for(int i = count; i < currentLevel.size(); i++) {
						Placement placement = currentLevel.get(i);

						if(widthRemainder.intersectsY(placement) && widthRemainder.intersectsX(placement)) {
							// there is overlap, subtract area
							widthRemainder.subtractX(placement);

							if (widthRemainder.getWidth() <= widthConstraint) {
								break;
							}							
						}
					}

					int depthConstraint;
					if(siblingIndex % 2 == 0) { // A and C
						depthConstraint = remainder.getDepth();
					} else {
						depthConstraint = 0;
					}					
					for(int i = count; i < currentLevel.size(); i++) {
						Placement placement = currentLevel.get(i);

						if(depthRemainder.intersectsY(placement) && depthRemainder.intersectsX(placement)) {
							// there is overlap, subtract area
							depthRemainder.subtractY(placement);

							if (depthRemainder.getDepth() <= depthConstraint) {
								break;
							}							
						}
					}					

					Box nextBox = null;
					
					Box widthRemainderBox = null;
					Box depthRemainderBox = null;
					
					Space nextSpace = null;

					if(widthRemainder.nonEmpty()) {
						widthRemainderBox = getBestBoxForSpace(containerProducts, widthRemainder, holder.getFreeWeight());
					} 
					if(depthRemainder.nonEmpty()) {
						depthRemainderBox = getBestBoxForSpace(containerProducts, depthRemainder, holder.getFreeWeight());
					}
					
					if(depthRemainderBox != null && (widthRemainderBox == null || depthRemainder.getVolume() > widthRemainder.getVolume())) {
						nextSpace = depthRemainder;
						nextBox = depthRemainderBox;
						
						// subtract primary space size
						Space primary = primaryPlacement.getSpace();
						primary.setWidth(primary.getWidth() - (depthRemainder.getWidth() - remainder.getWidth()));
					} else if(widthRemainderBox != null) {
						nextSpace = widthRemainder;
						nextBox = widthRemainderBox;
						
						// subtract primary space size
						Space primary = primaryPlacement.getSpace();
						primary.setDepth(primary.getDepth() - (widthRemainder.getDepth() - remainder.getDepth()));
					}

					if(nextBox != null) {
						removeIdentical(containerProducts, nextBox);

						// fit in (potentially expanded) remainder
						if(!fit2D(containerProducts, holder, nextBox, nextSpace, interrupt)) {
							return false; // time is up
						}
					}
				}
			}
		} 

		// also use the space above the placed box, if any.
		if(freeSpace.getHeight() > usedSpace.getHeight()) {
			// so there is some free room; between the used space and the level height

			// the level by level approach is somewhat crude, but at least some of the inefficiency
			// can be avoided this way
			Space above;
			if(primaryPlacement == null) {
				// full width / depth
				above = new Space(
						freeSpace.getWidth(), 
						freeSpace.getDepth(), 
						freeSpace.getHeight() - usedSpace.getHeight(),
						freeSpace.getX(),
						freeSpace.getY(),
						freeSpace.getZ() + usedSpace.getHeight()
						);
			} else {
				// just directly above the used space

				// TODO possible include the sibling space if no box was fitted there
				above = new Space(
						usedSpace.getWidth(), 
						usedSpace.getDepth(), 
						freeSpace.getHeight() - usedSpace.getHeight(),
						freeSpace.getX(),
						freeSpace.getY(),
						freeSpace.getZ() + usedSpace.getHeight()
						);
			}
			int currentIndex = getBestBox(holder, above, containerProducts);

			if(currentIndex != -1) {
				// should be within weight already
				Box currentBox = containerProducts.get(currentIndex); 

				containerProducts.remove(currentIndex);

				if(!fit2D(containerProducts, holder, currentBox, above, interrupt)) {
					return false;
				}
			}
		}

		return true;
	}

	private int getSiblingIndex(Space[] spaces, Placement nextPlacement) {
		Space nextPlacementSpace = nextPlacement.getSpace();
		if(nextPlacementSpace == spaces[0]) {
			return 1;
		} else if(nextPlacementSpace == spaces[1]) {
			return 0;
		} else if(nextPlacementSpace == spaces[2]) {
			return 3;
		} else { // if(nextPlacementSpace == spaces[3]) {
			return 2;
		}
	}	

	protected int findBestBoxForPoint(List<Box> containerProducts, Point point, int height, int freeWeight) {

		int index = -1;
		for(int i = 0; i < containerProducts.size(); i++) {
			Box box = containerProducts.get(i);
			
			if(box.getWeight() > freeWeight) {
				continue;
			}
			
			if(rotate3D) {
				if(box.canFitInside3D(point.getDx(), point.getDy(), height)) {
					if(index == -1 || isBetter3D(containerProducts.get(index), box, point, height) < 0) {
						index = i;
					}
				}
			} else {
				if(box.canFitInside2D(point.getDx(), point.getDy(), height)) {
					if(index == -1 || isBetter2D(containerProducts.get(index), box) < 0) {
						index = i;
					}
				}
			}
		}
		return index;
	}

	/**
	 * Is box b better than a?
	 * 
	 * @param a box
	 * @param b box
	 * @return -1 if b is better, 0 if equal, 1 if b is better
	 */

	protected int isBetter2D(Box a, Box b) {
		int compare = Long.compare(a.getVolume(), b.getVolume());
		if(compare != 0) {
			return compare;
		}
		return Long.compare(b.getFootprint(), a.getFootprint()); // i.e. smaller i better

	}

	/**
	 * Is box b strictly better than a?
	 * 
	 * @param a box
	 * @param b box
	 * @param space free space
	 * @return -1 if b is better, 0 if equal, 1 if b is better
	 */

	protected int isBetter3D(Box a, Box b, Point point, int dz) {
		int compare = Long.compare(a.getVolume(), b.getVolume());
		if(compare != 0) {
			return compare;
		}
		// determine lowest fit
		a.fitRotate3DSmallestFootprint(point.getDx(), point.getDy(), dz);
		b.fitRotate3DSmallestFootprint(point.getDx(), point.getDy(), dz);

		return Long.compare(b.getFootprint(), a.getFootprint()); // i.e. smaller i better
	}

	protected Placement getBestBoxAndSpace(List<Box> containerProducts, int freeWeight, ExtremePoints extremePoints, Context context) {

		// this method could have many implementation
		// it focuses on getting the biggest box possible fitted.
		// 
		// an alternative implementation would be one that 
		// measure the amount of wasted space and/or maximizes largest leftover area

		List<Point> points = extremePoints.getValues();
		
		boolean fullHeight = false;
		for (int i = 0; i < containerProducts.size(); i++) {
			Box box = containerProducts.get(i);

			for (int j = 0; j < points.size(); j++) {
				Point point = points.get(j);
				
				
		Box bestBox = null;
		Space bestSpace = null;
		for(Space space : spaces) {
			if(space == null) {
				continue;
			}

			Box box = getBestBoxForSpace(containerProducts, space, freeWeight);
			if(box == null) {
				continue;
			}
			boolean best;
			if(bestBox == null) {
				best = true;
			} else {
				int compare;
				if(rotate3D) {
					compare = isBetter3D(bestBox, box, space);
				} else {
					compare = isBetter2D(bestBox, box);
				}
				if(compare < 0) {
					best = true;
				} else if(compare == 0) {
					// if all is equal, prefer the box that came closest to one of its edges
					// in the available space
					
					// TODO this is really a complicated decision, which may not have a definitive right answer (all alternatives must explored)
					best = Math.min(space.getWidth() - box.getWidth(), space.getDepth() - box.getDepth()) < Math.min(bestSpace.getWidth() - bestBox.getWidth(), bestSpace.getDepth() - bestBox.getDepth());
				} else {
					best = false;
				}
			}

			if(best) {
				bestBox = box;
				bestSpace = space;
			}
		}
		if(bestBox != null) {
			if(rotate3D) {
				bestBox.fitRotate3DSmallestFootprint(bestSpace);
			}

			return new Placement(bestSpace, bestBox);
		}
		return null;
	}

	private class LAFFAdapter implements Adapter {

		private List<Box> boxes;
		private LAFFResult previous;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public LAFFAdapter(List<BoxItem> boxItems, List<Container> container, BooleanSupplier interrupt) {
			this.containers = container;

			List<Box> boxClones = new ArrayList<>(boxItems.size() * 2);

			for(BoxItem item : boxItems) {
				Box box = item.getBox();
				boxClones.add(box);
				for(int i = 1; i < item.getCount(); i++) {
					boxClones.add(box.clone());
				}
			}

			this.boxes = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public PackResult attempt(int index) {
			LAFFResult result = LargestAreaFitFirstPackager2.this.pack(new ArrayList<>(boxes), containers.get(index), interrupt);

			return previous = result;
		}

		@Override
		public Container accepted(PackResult result) {
			LAFFResult laffResult = (LAFFResult)result;

			this.boxes = laffResult.getRemainingBoxes();

			if(previous == result) {
				return laffResult.getContainer();
			}

			// calculate again
			Container container = laffResult.getContainer();
			List<Box> boxes = new ArrayList<>(this.boxes.size());
			for(Level level : container.getLevels()) {
				for(Placement placement : level) {
					boxes.add(placement.getBox());
				}
			}

			container.clear();

			LAFFResult pack = LargestAreaFitFirstPackager2.this.pack(boxes, container, Long.MAX_VALUE, Integer.MAX_VALUE);

			return pack.getContainer();
		}

		@Override
		public boolean hasMore(PackResult result) {
			LAFFResult laffResult = (LAFFResult)result;
			return !laffResult.getRemainingBoxes().isEmpty();
		}
	}

	@Override
	protected Adapter adapter(List<BoxItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new LAFFAdapter(boxes, containers, interrupt);
	}
}
