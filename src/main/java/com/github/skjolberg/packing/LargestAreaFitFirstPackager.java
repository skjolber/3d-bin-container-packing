package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * 
 * Thread-safe implementation.
 */

public class LargestAreaFitFirstPackager extends Packager {

	protected final boolean footprintFirst;
	
	/**
	 * Constructor
	 * 
	 * @param containers Dimensions of supported containers
	 */
	public LargestAreaFitFirstPackager(List<? extends Dimension> containers) {
		this(containers, true, true);
	}

	public LargestAreaFitFirstPackager(List<? extends Dimension> containers, boolean rotate3D, boolean footprintFirst) {
		super(containers, rotate3D);
		
		this.footprintFirst = footprintFirst;
	}
	
	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */
	
	protected Container pack(List<Box> boxes, Dimension containerBox, long deadline) {
		List<Box> containerProducts = new ArrayList<Box>(boxes);
		
		Container holder = new Container(containerBox);
		
		while(!containerProducts.isEmpty()) {
			if(System.currentTimeMillis() > deadline) {
				// fit2d below might have returned due to deadline

				break;
			}

			// choose the box with the largest surface area, that fits
			// if the same then the one with minimum height
			Dimension space = holder.getRemainigFreeSpace();
			Box currentBox = null;
			for(Box box : containerProducts) {
				
				boolean fits;
				if(rotate3D) {
					fits = box.rotateLargestFootprint3D(space);
				} else {
					fits = box.fitRotate2D(space);
				}
				if(fits) {
					if(currentBox == null) {
						currentBox = box;
					} else {
						if(footprintFirst) {
							if(currentBox.getFootprint() < box.getFootprint()) {
								currentBox = box;
							} else if(currentBox.getFootprint() == box.getFootprint() && currentBox.getHeight() < box.getHeight()) {
								currentBox = box;
							}
						} else {
							if(currentBox.getHeight() < box.getHeight()) {
								currentBox = box;
							} else if(currentBox.getHeight() == box.getHeight() && currentBox.getFootprint() < box.getFootprint()) {
								currentBox = box;
							}
						}							
					}
				} else {
					// no fit in the current container within the remaining space
					// try the next container

					return null;
				}
			}

			if(currentBox.getHeight() < space.getHeight()) {
				// see whether we can get a fit for full height
				Box idealBox = null;
				for(Box box : containerProducts) {
					if(box.getHeight() == space.getHeight()) {
						if(idealBox == null) {
							idealBox = box;
						} else if(idealBox.getFootprint() < box.getFootprint()) {
							idealBox = box;
						}
					}
				}
				if(idealBox != null) {
					currentBox = idealBox;
				}
			}				
			
			// current box should have the optimal orientation already
			// create a space which holds the full level
			Space levelSpace = new Space(
						containerBox.getWidth(), 
						containerBox.getDepth(), 
						currentBox.getHeight(), 
						0, 
						0, 
						holder.getStackHeight()
						);
			
			holder.addLevel();
			removeIdentical(containerProducts, currentBox);

			fit2D(containerProducts, holder, currentBox, levelSpace, deadline);
		}
		
		return holder;
	}

	/**
	 * Remove from list, more explicit implementation than {@linkplain List#remove} with no equals.
	 * @param containerProducts list of products
	 * @param currentBox item to remove
	 */
	
	protected void removeIdentical(List<Box> containerProducts, Box currentBox) {
		for(int i = 0; i < containerProducts.size(); i++) {
			if(containerProducts.get(i) == currentBox) {
				containerProducts.remove(i);
				
				return;
			}
		}
		throw new IllegalArgumentException();
	}
	
	protected void fit2D(List<Box> containerProducts, Container holder, Box usedSpace, Space freeSpace, long deadline) {

		if(rotate3D) {
			// minimize footprint
			usedSpace.fitRotate3DSmallestFootprint(freeSpace);
		}

		// add used space box now, but possibly rotate later - this depends on the actual remaining free space selected further down
		// there is up to possible 4 free spaces, 2 in which the used space box is rotated
		holder.add(new Placement(freeSpace, usedSpace));

		if(containerProducts.isEmpty()) {
			// no additional boxes
			// just make sure the used space fits in the free space
			usedSpace.fitRotate2D(freeSpace);
			
			return;
		}
		
		if(System.currentTimeMillis() > deadline) {
			return;
		}
		
		Space[] spaces = getFreespaces(freeSpace, usedSpace, true);

		Placement nextPlacement = bestVolumePlacement(containerProducts, spaces);
		if(nextPlacement == null) {
			// no additional boxes
			// just make sure the used space fits in the free space
			usedSpace.fitRotate2D(freeSpace);
			
			return;
		}
		
		// check whether the selected free space requires the used space box to be rotated
		if(nextPlacement.getSpace() == spaces[2] || nextPlacement.getSpace() == spaces[3]) {
			// the desired space implies that we rotate the used space box
			usedSpace.rotate2D();
		}
		
		// holder.validateCurrentLevel(); // uncomment for debugging
		
		removeIdentical(containerProducts, nextPlacement.getBox());

		// attempt to fit in the remaining (usually smaller) space first
		
		// stack in the 'sibling' space - the space left over between the used box and the selected free space
		Space remainder = nextPlacement.getSpace().getRemainder();
		if(!remainder.isEmpty()) {
			Box box = bestVolume(containerProducts, remainder);
			if(box != null) {
				removeIdentical(containerProducts, box);
				
				fit2D(containerProducts, holder, box, remainder, deadline);
			}
		}

		// fit the next box in the selected free space
		fit2D(containerProducts, holder, nextPlacement.getBox(), nextPlacement.getSpace(), deadline);
		
		// TODO use free spaces between box and level, if any
	}

	protected Space[] getFreespaces(Space freespace, Box used, boolean rotate2D) {

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
		// Second:
		//
		// ........................   ........................  ..................
		// .                      .   .                      .  .                .
		// .          C           .   .         C            .  .                .
		// .                      .   .                      .  .                .
		// .......                .   ........................  .                .
		// .     .       D        .                             .        D       .
		// .     .                .                             .                .
		// .     .                .                             .                .
		// .     .                .                             .                .
		// ........................                             ..................
		//
		// So there is always a 'big' and a 'small' leftover area (the small is not shown).

		Space[] freeSpaces = new Space[4];
		if(freespace.getWidth() >= used.getWidth() && freespace.getDepth() >= used.getDepth()) {
			
			// if B is empty, then it is sufficient to work with A and the other way around
			
			// B
			if(freespace.getWidth() > used.getWidth()) {
				Space right = new Space(
						freespace.getWidth() - used.getWidth(), freespace.getDepth(), freespace.getHeight(),
						freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
						);
				
				Space rightRemainder = new Space(
						used.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
						freespace.getX(), freespace.getY()+ used.getDepth(), freespace.getZ()
						);
				right.setRemainder(rightRemainder);
				rightRemainder.setRemainder(right);
				freeSpaces[0] = right;
			}
			
			// A
			if(freespace.getDepth() > used.getDepth()) {
				Space top = new Space(
							freespace.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight(),
							freespace.getX(), freespace.getY() + used.depth, freespace.getHeight()
						);
				Space topRemainder = new Space(
							freespace.getWidth() - used.getWidth(), used.getDepth(), freespace.getHeight(),
							freespace.getX() + used.getWidth(), freespace.getY(), freespace.getZ()
						);
				top.setRemainder(topRemainder);
				topRemainder.setRemainder(top);
				freeSpaces[1] = top;
			}
		}
		
		if(rotate2D) {
			if(freespace.getWidth() >= used.getDepth() && freespace.getDepth() >= used.getWidth()) {	
				// if D is empty, then it is sufficient to work with C and the other way around
				
				// D
				if(freespace.getWidth() > used.getDepth()) {
					Space right = new Space(
							freespace.getWidth() - used.getDepth(), freespace.getDepth(), freespace.getHeight(),
							freespace.getX() + used.getDepth(), freespace.getY(), freespace.getHeight()
							);
					Space rightRemainder = new Space(
							used.getDepth(), freespace.getDepth() - used.getWidth(), freespace.getHeight(),
							freespace.getX(), freespace.getY() + used.getWidth(), freespace.getZ()
							);
					right.setRemainder(rightRemainder);
					rightRemainder.setRemainder(right);
					freeSpaces[2] = right;
				}
				
				// C
				if(freespace.getDepth() > used.getWidth()) {
					Space top = new Space(
							freespace.getWidth(), freespace.getDepth() - used.getWidth(), freespace.getHeight(),
							freespace.getX(), freespace.getY() + used.getWidth(), freespace.getHeight()
							);
					Space topRemainder = new Space(
							freespace.getWidth() - used.getDepth(), used.getWidth(), freespace.getHeight(),
							freespace.getX() + used.getDepth(), freespace.getY(), freespace.getZ()
							);
					top.setRemainder(topRemainder);
					topRemainder.setRemainder(top);
					freeSpaces[3] = top;
				}
			}
		}
		return freeSpaces;
	}

	protected Box bestVolume(List<Box> containerProducts, Space space) {
		
		Box bestBox = null;
		for(Box box : containerProducts) {
			
			if(rotate3D) {
				if(box.canFitInside3D(space)) {
					if(bestBox == null) {
						bestBox = box;
						
						bestBox.fitRotate3DSmallestFootprint(space);
					} else if(bestBox.getVolume() < box.getVolume()) {
						bestBox = box;
						
						bestBox.fitRotate3DSmallestFootprint(space);
					} else if(bestBox.getVolume() == box.getVolume()) {
						// determine lowest fit	
						box.fitRotate3DSmallestFootprint(space);
						
						if(box.getFootprint() < bestBox.getFootprint()) {
							bestBox = box;
						}
					}
				}
			} else {
				if(box.canFitInside2D(space)) {
					if(bestBox == null) {
						bestBox = box;
					} else if(bestBox.getVolume() < box.getVolume()) {
						bestBox = box;
					} else if(bestBox.getVolume() < box.getVolume()) {
						// TODO use the aspect ratio in some meaningful way
					}
				}
			}
		}
		return bestBox;
	}
	
	protected Placement bestVolumePlacement(List<Box> containerProducts, Space[] spaces) {
		
		Box bestBox = null;
		Space bestSpace = null;
		for(Space space : spaces) {
			if(space == null) {
				continue;
			}
			for(Box box : containerProducts) {
				
				if(rotate3D) {
					if(box.canFitInside3D(space)) {
						if(bestBox == null) {
							bestBox = box;
							bestSpace = space;
							
							bestBox.fitRotate3DSmallestFootprint(bestSpace);
						} else if(bestBox.getVolume() < box.getVolume()) {
							bestBox = box;
							bestSpace = space;
							
							bestBox.fitRotate3DSmallestFootprint(bestSpace);
						} else if(bestBox.getVolume() == box.getVolume()) {
							// determine lowest fit
							box.fitRotate3DSmallestFootprint(space);
							
							if(box.getFootprint() < bestBox.getFootprint()) {
								bestBox = box;
								bestSpace = space;
							}
							
							// TODO if all else is equal, which free space is preferred?
						}
					}
				} else {
					if(box.canFitInside2D(space)) {
						if(bestBox == null) {
							bestBox = box;
							bestSpace = space;
						} else if(bestBox.getVolume() < box.getVolume()) {
							bestBox = box;
							bestSpace = space;
						} else if(bestBox.getVolume() < box.getVolume()) {
							// TODO use the aspect ratio in some meaningful way
						}
						
						// TODO if all else is equal, which free space is preferred?
					}
				}
			}
		}
		if(bestBox != null) {
			return new Placement(bestSpace, bestBox);
		}
		return null;
	}


}
