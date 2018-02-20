package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * 
 * Thread-safe implementation.
 */

public abstract class Packager {

	/**
	 * Logical packager for wrapping preprocessing / optimizations.
	 *
	 */
	
	protected interface PackagerImpl {
		Container fit(List<Box> boxes, Dimension dimension, long deadline);
	}
	
	protected final Dimension[] containers;
	
	protected final boolean rotate3D; // if false, then 2d
	protected final boolean binarySearch;
	
	/**
	 * Constructor
	 * 
	 * @param containers Dimensions of supported containers
	 */
	public Packager(List<? extends Dimension> containers) {
		this(containers, true, true);
	}

	public Packager(List<? extends Dimension> containers, boolean rotate3D, boolean binarySearch) {
		this.containers = containers.toArray(new Dimension[containers.size()]);
		this.rotate3D = rotate3D;
		this.binarySearch = binarySearch;
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
	 * Return a list of containers which can potentially hold the boxes.
	 * 
	 * @param boxes list of boxes
	 * @return list of containers
	 */
	
	public List<Dimension> filter(List<Box> boxes) {
		long volume = 0;
		for(Box box : boxes) {
			volume += box.getVolume();
		}
		
		List<Dimension> list = new ArrayList<>();
		for(Dimension container : containers) {
			if(container.getVolume() < volume || !canHold(container, boxes)) {
				// discard this container
				continue;
			}

			list.add(container);
		}
		
		return list;
	}
	
	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	
	public Container pack(List<Box> boxes, long deadline) {
		return pack(boxes, filter(boxes), deadline);
	}

	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param dimensions list of containers
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	
	public Container pack(List<Box> boxes, List<Dimension> dimensions, long deadline) {
		if(dimensions.isEmpty()) {
			return null;
		}
		
		PackagerImpl pack = impl(boxes);

		if(!binarySearch || dimensions.size() <= 2) {
			for (int i = 0; i < dimensions.size(); i++) {
					
				if(System.currentTimeMillis() > deadline) {
					break;
				}
				
				Container result = pack.fit(boxes, dimensions.get(i), deadline);
				if(result != null) {
					return result;
				}
			}
		} else {
			Container[] results = new Container[dimensions.size()];
			boolean[] checked = new boolean[results.length]; 

			ArrayList<Integer> current = new ArrayList<>();
			for(int i = 0; i < dimensions.size(); i++) {
				current.add(i);
			}

			BinarySearchIterator iterator = new BinarySearchIterator();

			search:
			do {
				iterator.reset(current.size() - 1, 0);
				
				do {
					
					int mid = current.get(iterator.next());

					Container result = pack.fit(boxes, dimensions.get(mid), deadline);
					
					checked[mid] = true;
					if(result != null) {
						results[mid] = result;
						
						iterator.lower();
					} else {
						iterator.higher();
					}
					if(System.currentTimeMillis() > deadline) {
						break search;
					}
				} while(iterator.hasNext()); 
				
		        // halt when has a result, and checked all containers at the lower indexes
		        for (int i = 0; i < current.size(); i++) {
		        	Integer integer = current.get(i);
					if(results[integer] != null) {
						// remove end items
						while(current.size() > i) {
							current.remove(current.size() - 1);
						}
						break;
					}
					
					// remove item
					if(checked[integer]) {
						current.remove(i);
						i--;
					}
		        }
	        } while(!current.isEmpty());
		        
			for(int i = 0; i < results.length; i++) {
				if(results[i] != null) {
					return results[i];
				}
			}
		}
		return null;
	}	
	
	protected abstract PackagerImpl impl(List<Box> boxes);

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

	public static List<Placement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder). 
		List<Placement> placements = new ArrayList<Placement>(size);

		for(int i = 0; i < size; i++) {
			Space a = new Space();
			Space b = new Space();
			a.setRemainder(b);
			b.setRemainder(a);
			
			placements.add(new Placement(a));
		}
		return placements;
	}		


}
