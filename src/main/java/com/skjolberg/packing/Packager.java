package com.skjolberg.packing;

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
				
				// currentbox should have the optimal orientation already
				if(containerProducts.isEmpty()) {
					// all boxes fitted with the current container
					break;
				}
				
				Dimension freeSpace = new Dimension(containerBox.getWidth(), containerBox.getDepth(), currentBox.getHeight()); // ak, bk, ck
				
				fit(containerProducts, holder, currentBox, freeSpace);
			}

			return holder;
		}
		
		return null;
	}
	
	private void fit(List<Box> containerProducts, Container holder, Box usedSpace, Dimension freespace) {
		
		Dimension r1 = new Dimension(freespace.getWidth() - usedSpace.getWidth(), freespace.getDepth(), freespace.getHeight()); // right
		Dimension t1 = new Dimension(freespace.getWidth(), freespace.getDepth() - usedSpace.getDepth(), freespace.getHeight()); // top

		Dimension r2 = new Dimension(freespace.getWidth() - usedSpace.getDepth(), freespace.getDepth(), freespace.getHeight()); // right
		Dimension t2 = new Dimension(freespace.getWidth(), freespace.getDepth() - usedSpace.getWidth(), freespace.getHeight()); // top

		List<Dimension> spaces = new ArrayList<Dimension>();
		if(!r1.isEmpty()) {
			spaces.add(r1);
		}
		if(!t1.isEmpty()) {
			spaces.add(t1);
		}
		if(!r2.isEmpty()) {
			spaces.add(r2);
		}
		if(!t2.isEmpty()) {
			spaces.add(t2);
		}
		
		if(spaces.isEmpty()) {
			// no boxes
			return;
		}
		Box nextBox = bestVolume(containerProducts, spaces);

		if(nextBox == null) {
			// no fit
			return;
		}
		
		nextBox.rotateSmallestFootprint(freespace);
		
		holder.add(nextBox);
		containerProducts.remove(nextBox);
		
		if(nextBox.getFootprint() == freespace.getFootprint()) {
			// no more space left
			return;
		}
		
		// maximize free space
		Dimension nextFreeSpace = null;
		
		// determine stacking within free space to maximize leftover area
		for(Dimension space : spaces) {
			Dimension free = getBest(space, nextBox);
			if(free != null) {
				if(nextFreeSpace == null || free.getFootprint() > nextFreeSpace.getFootprint()) {
					nextFreeSpace = free;
				}
			}
		}
		
		if(nextFreeSpace == null) {
			// nothing left
			return;
		}
		
		// so what about the other remaining space?
		Dimension parent = nextFreeSpace.getParent();
		
		Dimension leftOverFreespace = null;
		if(parent == r1) {
			// free space in depth the used space
			leftOverFreespace = new Dimension(usedSpace.getWidth(), freespace.getDepth() - usedSpace.getDepth(), freespace.getHeight());
		} else if(parent == t1) {
			// free space in width of the used space
			leftOverFreespace = new Dimension(freespace.getWidth() - usedSpace.getWidth(), usedSpace.getDepth(), freespace.getHeight());
		} else if(parent == r2) {
			// free space in depth the used space (rotated used space)
			leftOverFreespace = new Dimension(usedSpace.getDepth(), freespace.getDepth() - usedSpace.getWidth(), freespace.getHeight());
		} else if(parent == t2) {
			// free space in width of the used space (rotated used space)
			leftOverFreespace = new Dimension(freespace.getWidth() - usedSpace.getDepth(), usedSpace.getWidth(), freespace.getHeight());
		} else {
			throw new IllegalArgumentException();
		}
		fitNext(containerProducts, holder, nextFreeSpace);
		
		fitNext(containerProducts, holder, leftOverFreespace);
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
		}
		
		return best;
	}

	private Dimension getBest(Dimension freespace, Box box) {
		
		Dimension alignment1 = null;
		if(box.fitsInside(freespace)) {
			if( (freespace.getDepth() - box.getDepth()) * freespace.getWidth() >  (freespace.getWidth() - box.getWidth()) * freespace.getDepth()) {
				alignment1 = new Dimension(freespace, freespace.getWidth(), freespace.getDepth() - box.getDepth(), freespace.getHeight());
			} else {
				alignment1 = new Dimension(freespace, freespace.getWidth() - box.getWidth(), freespace.getDepth(), freespace.getHeight());
			}
		}
		
		Dimension alignment2 = null;
		box.rotate2D();
		if(box.fitsInside(freespace)) {
			if( (freespace.getDepth() - box.getDepth()) * freespace.getWidth() >  (freespace.getWidth() - box.getWidth()) * freespace.getDepth()) {
				alignment2 = new Dimension(freespace, freespace.getWidth(), freespace.getDepth() - box.getDepth(), freespace.getHeight());
			} else {
				alignment2 = new Dimension(freespace, freespace.getWidth() - box.getWidth(), freespace.getDepth(), freespace.getHeight());
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
