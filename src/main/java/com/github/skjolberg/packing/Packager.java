package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * 
 * Thread-safe implementation.
 */

public class Packager {

	private final Dimension[] containers;
	
	/**
	 * Constructor
	 * 
	 * @param containers Dimensions of supported containers
	 */

	public Packager(List<? extends Dimension> containers) {
		this.containers = containers.toArray(new Dimension[containers.size()]);
	}
	
	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
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
				if(!containerBox.canHold(box)) {
					continue boxes;
				}
			}
			
			List<Box> containerProducts = new ArrayList<Box>(boxes);
			
			Container holder = new Container(containerBox);
			
			while(!containerProducts.isEmpty()) {
				// choose the box with the largest surface area, that fits
				// if the same then the one with minimum height
				Box space = holder.getRemainigFreeSpace(containerBox);
				Box currentBox = null; // i
				for(Box box : containerProducts) {
					
					if(box.rotateLargestFootprint(space)) {
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
				
				holder.addLevel();
				holder.add(currentBox);
				containerProducts.remove(currentBox);
				
				if(containerProducts.isEmpty()) {
					// all boxes fitted with the current container
					break;
				}
				
				// current box should have the optimal orientation already
				Dimension freeSpace = new Dimension(containerBox.getWidth(), containerBox.getDepth(), currentBox.getHeight()); // ak, bk, ck
				
				fit(containerProducts, holder, currentBox, freeSpace);
			}

			return holder;
		}
		
		return null;
	}
	
	private void fit(List<Box> containerProducts, Container holder, Box usedSpace, Dimension freespace) {

		List<Dimension> spaces = getFreespaces(freespace, usedSpace);
		if(spaces.isEmpty()) {
			// no boxes
			return;
		} 
		Box nextBox = bestVolume(containerProducts, spaces); // sets space as parent

		if(nextBox == null) {
			// no fit
			return;
		}
		Dimension space = nextBox.getParent();
		
		nextBox.rotateSmallestFootprint(space);
		
		holder.add(nextBox);
		containerProducts.remove(nextBox);

		// stack in leftover from original free space
		Dimension remainder = space.getParent();
		if(!remainder.isEmpty()) {
			fitNext(containerProducts, holder, remainder);
			if(containerProducts.isEmpty()) {
				// finished
				return;
			} 
		}
		
		// determine stacking within free space to maximize leftover area
		List<Dimension> freeSpaces = getFreespaces(space, nextBox);

		if(freeSpaces.isEmpty()) {
			// no more space left
			return;
		} 

		Dimension nextFreeSpace = null;
		for(Dimension free : freeSpaces) {
			if(nextFreeSpace == null || free.getFootprint() > nextFreeSpace.getFootprint()) {
				nextFreeSpace = free;
			}
		}
		
		Dimension nextFreeSpaceRemainder = nextFreeSpace.getParent();
		
		fitNext(containerProducts, holder, nextFreeSpace);
		if(containerProducts.isEmpty()) {
			// finished
			return;
		} 
		
		if(!nextFreeSpaceRemainder.isEmpty()) {
			fitNext(containerProducts, holder, nextFreeSpaceRemainder);
		}
	}

	private List<Dimension> getFreespaces(Dimension freespace, Box used) {

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
		// So there is always a big and a small leftover area (the small is not shown).

		List<Dimension> freeSpaces = new ArrayList<Dimension>();
		if(freespace.getWidth() >= used.getWidth() && freespace.getDepth() >= used.getDepth()) {
			// B
			Dimension right = new Dimension(freespace.getWidth() - used.getWidth(), freespace.getDepth(), freespace.getHeight());
			if(!right.isEmpty()) {
				Dimension rightRemainder = new Dimension(used.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight());
				right.setParent(rightRemainder);
				freeSpaces.add(right);
			}
			
			// A
			Dimension top = new Dimension(freespace.getWidth(), freespace.getDepth() - used.getDepth(), freespace.getHeight());
			if(!top.isEmpty()) {
				Dimension topRemainder = new Dimension(freespace.getWidth() - used.getWidth(), used.getDepth(), freespace.getHeight());
				top.setParent(topRemainder);
				freeSpaces.add(top);
			}
		}
		
		if(freespace.getWidth() >= used.getDepth() && freespace.getDepth() >= used.getWidth()) {	
			// D
			Dimension right = new Dimension(freespace.getWidth() - used.getDepth(), freespace.getDepth(), freespace.getHeight());
			if(!right.isEmpty()) {
				Dimension rightRemainder = new Dimension(used.getDepth(), freespace.getDepth() - used.getWidth(), freespace.getHeight());
				right.setParent(rightRemainder);
				freeSpaces.add(right);
			}
			
			// C
			Dimension top = new Dimension(freespace.getWidth(), freespace.getDepth() - used.getWidth(), freespace.getHeight());
			if(!top.isEmpty()) {
				Dimension topRemainder = new Dimension(freespace.getWidth() - used.getDepth(), used.getWidth(), freespace.getHeight());
				top.setParent(topRemainder);
				freeSpaces.add(top);
			}
		}
		return freeSpaces;
	}

	private void fitNext(List<Box> containerProducts, Container holder, Dimension nextFreeSpace) {
		if(!nextFreeSpace.isEmpty()) {
			Box nextUsedSpace = bestVolume(containerProducts, nextFreeSpace);
			
			if(nextUsedSpace != null) {
				holder.add(nextUsedSpace);
				containerProducts.remove(nextUsedSpace);
				
				nextUsedSpace.rotateSmallestFootprint(nextFreeSpace);
				
				fit(containerProducts, holder, nextUsedSpace, nextFreeSpace);
			}
		}
	}


	private Box bestVolume(List<Box> containerProducts, Dimension space) {
		
		Box best = null;
		for(Box box : containerProducts) {
			
			if(box.canFitInside(space)) {
				if(best == null) {
					best = box;
				} else if(best.getVolume() < box.getVolume()) {
					best = box;
				} else if(best.getVolume() == box.getVolume()) {
					// determine lowest fit
					best.rotateSmallestFootprint(space);

					box.rotateSmallestFootprint(space);
					
					if(box.getFootprint() < best.getFootprint()) {
						best = box;
					}
				}
			}
		}
		
		return best;
	}
	
	private Box bestVolume(List<Box> containerProducts, List<Dimension> spaces) {
		
		Box best = null;
		for(Dimension space : spaces) {
			for(Box box : containerProducts) {
				
				if(box.canFitInside(space)) {
					if(best == null) {
						best = box;
						
						box.setParent(space);
					} else if(best.getVolume() < box.getVolume()) {
						best = box;
						
						box.setParent(space);
					} else if(best.getVolume() == box.getVolume()) {
						// determine lowest fit
						best.rotateSmallestFootprint(space);
	
						box.rotateSmallestFootprint(space);
						
						if(box.getFootprint() < best.getFootprint()) {
							best = box;
							
							box.setParent(space);
						}
					}
				}
			}
		}
		
		return best;
	}

	private Dimension getBest(Dimension freespace, Box box) {
		
		Dimension alignment1 = null;
		if(box.fitsInside(freespace.getWidth(), freespace.getDepth(), freespace.getHeight())) {
			if( (freespace.getDepth() - box.getDepth()) * freespace.getWidth() >  (freespace.getWidth() - box.getWidth()) * freespace.getDepth()) {
				alignment1 = new Dimension(freespace, freespace.getWidth(), freespace.getDepth() - box.getDepth(), freespace.getHeight());
			} else {
				alignment1 = new Dimension(freespace, freespace.getWidth() - box.getWidth(), freespace.getDepth(), freespace.getHeight());
			}
		}
		
		Dimension alignment2 = null;
		if(box.fitsInside(freespace.getDepth(), freespace.getWidth(), freespace.getHeight())) {
			if( (freespace.getDepth() - box.getWidth()) * freespace.getWidth() >  (freespace.getWidth() - box.getDepth()) * freespace.getDepth()) {
				alignment2 = new Dimension(freespace, freespace.getWidth(), freespace.getDepth() - box.getWidth(), freespace.getHeight());
			} else {
				alignment2 = new Dimension(freespace, freespace.getWidth() - box.getDepth(), freespace.getDepth(), freespace.getHeight());
			}
			
		}
		
		if(alignment1 != null && alignment2 != null) {
			if(alignment1.getFootprint() > alignment2.getFootprint()) {
				return alignment1;
			} else {
				return alignment2;
			}
		} else if(alignment1 != null) {
			return alignment1;
		} else if(alignment2 != null) {
			return alignment2;
		}
		
		return null;
	}

}
