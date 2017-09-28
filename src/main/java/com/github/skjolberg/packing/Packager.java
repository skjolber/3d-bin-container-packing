package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * 
 * Thread-safe implementation.
 */

public class Packager {

	private final Dimension[] containers;
	
	private final boolean rotate3D; // if false, then 2d
	
	/**
	 * Constructor
	 * 
	 * @param containers Dimensions of supported containers
	 */
	public Packager(List<? extends Dimension> containers) {
		this(containers, true);
	}

	public Packager(List<? extends Dimension> containers, boolean rotate3D) {
		this.containers = containers.toArray(new Dimension[containers.size()]);
		this.rotate3D = rotate3D;
	}
	
	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @return null if no match
	 */
	
	public Container pack(List<Box> boxes) {
		
		int volume = 0;
		for(Box box : boxes) {
			volume += box.getVolume();
		}
		
		boxes:
		for(Dimension containerBox : containers) {
			if(containerBox.getVolume() < volume) {
				continue;
			}

			for(Box box : boxes) {
				if(rotate3D) {
					if(!containerBox.canHold3D(box)) {
						continue boxes;
					}
				} else {
					if(!containerBox.canHold2D(box)) {
						continue boxes;
					}
				}
			}
			
			List<Box> containerProducts = new ArrayList<Box>(boxes);
			
			Container holder = new Container(containerBox);
			
			while(!containerProducts.isEmpty()) {
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
						} else if(currentBox.getFootprint() < box.getFootprint()) {
							currentBox = box;
						} else if(currentBox.getFootprint() == box.getFootprint() && currentBox.getHeight() > box.getHeight() ) {
							currentBox = box;
						}
					}
				}
				
				if(currentBox == null) {
					// no fit in the current container within the remaining space
					// try the next container
					continue boxes;
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
				holder.add(new Placement(levelSpace, currentBox));
				containerProducts.remove(currentBox);
				
				if(containerProducts.isEmpty()) {
					// all boxes fitted with the current container
					break;
				}
								
				fit2D(containerProducts, holder, currentBox, levelSpace);
			}

			return holder;
		}
		
		return null;
	}
	
	private void fit2D(List<Box> containerProducts, Container holder, Box usedSpace, Space freeSpace) {

		List<Space> spaces = getFreespaces(freeSpace, usedSpace);
		if(spaces.isEmpty()) {
			// no boxes
			return;
		}
		
		Placement nextPlacement = bestVolume(containerProducts, spaces);
		if(nextPlacement == null) {
			// no fit
			return;
		}
		
		if(rotate3D) { // maximize use of height
			nextPlacement.getBox().fitRotate3DSmallestFootprint(nextPlacement.getSpace());
		} else {
			nextPlacement.getBox().fitRotate2D(nextPlacement.getSpace());
		}
		
		holder.add(nextPlacement);
		containerProducts.remove(nextPlacement.getBox());

		// stack in the 'sibling' space - other part of free space
		Space remainder = (Space) nextPlacement.getSpace().getRemainder();
		if(!remainder.isEmpty()) {
			fitNext(containerProducts, holder, remainder);
			if(containerProducts.isEmpty()) {
				// finished
				return;
			} 
		}
		
		// determine if the stacking left some space over in the selected space
		List<Space> freeSpaces = getFreespaces(nextPlacement.getSpace(), nextPlacement.getBox());

		if(freeSpaces.isEmpty()) {
			// no more space left
			return;
		}
		
		Space nextFreeSpace = null;
		for(Space free : freeSpaces) {
			if(nextFreeSpace == null || free.getFootprint() > nextFreeSpace.getFootprint()) {
				nextFreeSpace = free;
			}
		}
		
		fitNext(containerProducts, holder, nextFreeSpace);
		if(containerProducts.isEmpty()) {
			// finished
			return;
		} 
		
		Space nextFreeSpaceRemainder = nextFreeSpace.getRemainder();
		if(!nextFreeSpaceRemainder.isEmpty()) {
			fitNext(containerProducts, holder, nextFreeSpaceRemainder);
		}
	}

	private List<Space> getFreespaces(Space freespace, Box used) {

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

		List<Space> freeSpaces = new ArrayList<Space>();
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
				freeSpaces.add(right);
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
				freeSpaces.add(top);
			}
		}
		
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
				freeSpaces.add(right);
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
				freeSpaces.add(top);
			}
		}
		return freeSpaces;
	}

	private void fitNext(List<Box> containerProducts, Container holder, Space freeSpace) {
		if(!freeSpace.isEmpty()) {
			Placement placement = bestVolume(containerProducts, freeSpace);
			
			if(placement != null) {
				holder.add(placement);
				containerProducts.remove(placement.getBox());
				
				if(rotate3D) {
					placement.getBox().fitRotate3DSmallestFootprint(freeSpace);
				}
				fit2D(containerProducts, holder, placement.getBox(), freeSpace);
			}
		}
	}


	private Placement bestVolume(List<Box> containerProducts, Space space) {
		return bestVolume(containerProducts, Arrays.asList(space));
	}
	
	private Placement bestVolume(List<Box> containerProducts, List<Space> spaces) {
		
		Box bestBox = null;
		Space bestSpace = null;
		for(Space space : spaces) {
			for(Box box : containerProducts) {
				
				if(rotate3D) {
					if(box.canFitInside3D(space)) {
						if(bestBox == null) {
							bestBox = box;
							bestSpace = space;
						} else if(bestBox.getVolume() < box.getVolume()) {
							bestBox = box;
							bestSpace = space;
						} else if(bestBox.getVolume() == box.getVolume()) {
							// determine lowest fit
							bestBox.fitRotate3DSmallestFootprint(space);
		
							box.fitRotate3DSmallestFootprint(space);
							
							if(box.getFootprint() < bestBox.getFootprint()) {
								bestBox = box;
								bestSpace = space;
							}
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
