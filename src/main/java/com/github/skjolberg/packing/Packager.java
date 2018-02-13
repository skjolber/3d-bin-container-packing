package com.github.skjolberg.packing;

import java.util.List;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * 
 * Thread-safe implementation.
 */

public abstract class Packager {

	protected final Dimension[] containers;
	
	protected final boolean rotate3D; // if false, then 2d
	
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
		return pack(boxes, Long.MAX_VALUE);
	}

	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return null if no match, or deadline reached
	 */
	
	public Container pack(List<Box> boxes, long deadline) {
		
		long volume = 0;
		for(Box box : boxes) {
			volume += box.getVolume();
		}
		
		for(Dimension containerBox : containers) {
			if(System.currentTimeMillis() > deadline) {
				break;
			}
			
			if(containerBox.getVolume() < volume || !canHold(containerBox, boxes)) {
				// discard this container
				continue;
			}

			Container result = pack(boxes, containerBox, deadline);
			if(result != null) {
				return result;
			}
		}
		
		return null;
	}	

	protected boolean canHold(Dimension containerBox, List<Box> boxes) {
		for(Box box : boxes) {
			if(rotate3D) {
				if(!containerBox.canHold3D(box)) {
					return false;
				}
			} else {
				if(!containerBox.canHold2D(box)) {
					return false;
				}
			}
		}
		return true;
	}
	
	protected abstract Container pack(List<Box> boxes, Dimension containerBox, long deadline);

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

}
