package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.iterators.PermutationIterator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. 
 * 
 * This attempts a brute force approach, which is very demanding in terms of resources. Only use for scenarios with
 * few boxes.
 * 
 * Thread-safe implementation.
 */

public class BruteForcePackager extends Packager {

	public BruteForcePackager(List<? extends Dimension> containers, boolean rotate3d) {
		super(containers, rotate3d);
	}

	public BruteForcePackager(List<? extends Dimension> containers) {
		super(containers);
	}
	
	
	public Container pack(List<Box> boxes, long deadline) {

		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
		
		// each box will at most have a single placement with a space (and its remainder). 
		List<Placement> placements = new ArrayList<Placement>(boxes.size());

		long volume = 0;
		for(Box box : boxes) {
			volume += box.getVolume();
			
			Space a = new Space();
			Space b = new Space();
			a.setRemainder(b);
			b.setRemainder(a);
			
			placements.add(new Placement(a, box));
		}
		
		for(Dimension container : containers) {
			if(System.currentTimeMillis() > deadline) {
				break;
			}
			
			if(container.getVolume() < volume || !canHold(container, boxes)) {
				// discard this container
				continue;
			}

			Container result = pack(placements, container, deadline);
			if(result != null) {
				return result;
			}
		}
		
		return null;
	}		

	protected Container pack(List<Placement> placements, Dimension container, long deadline) {
		
		// setup reusable objects
		int[] reset = new int[placements.size()];

		Container holder = new Container(container);

		List<Placement> containerPlacements = new ArrayList<Placement>(placements.size());

		int[] rotations = new int[placements.size()];

		Dimension remainingSpace = container;
		
		// iterator over all permutations
		PermutationIterator<Placement> iterator = new PermutationIterator<Placement>(placements);
		while(iterator.hasNext()) {
			if(System.currentTimeMillis() > deadline) {
				break;
			}

			List<Placement> permutation = iterator.next();

			// for each permutation, try all combinations of rotation
			permutation:
				while(true) {
					fit: {
						// check sanity of current rotation
						for(Placement placement : permutation) {
							if(!placement.getBox().fitsInside3D(remainingSpace)) {
								break fit;
							}
						}
	
						containerPlacements.addAll(permutation);
						
						while(!containerPlacements.isEmpty()) {
							if(System.currentTimeMillis() > deadline) {
								// fit2d below might have returned due to deadline
								return null;
							}
							
							Placement placement = containerPlacements.remove(0);
							
							if(!placement.getBox().fitsInside3D(remainingSpace)) {
								
								// clean up
								holder.clear();
								containerPlacements.clear();
	
								break fit;
							}
							
							Space levelSpace = placement.getSpace();
							levelSpace.width = container.getWidth();
							levelSpace.depth = container.getDepth();
							levelSpace.height = placement.getBox().getHeight();
							levelSpace.x = 0;
							levelSpace.y = 0;
							levelSpace.z = holder.getStackHeight();
							
							levelSpace.setParent(null);
							levelSpace.getRemainder().setParent(null);
							
							holder.addLevel();

							// update remaining space
							remainingSpace = holder.getRemainigFreeSpace();

							fit2D(containerPlacements, holder, placement, deadline);
						}
						
						return holder; // uncomment this line to run all combinations
					}
	
					// find the next rotation
					do {
						// next rotation
						int index = rotate(rotations);
						if(index == -1) {
							// done
							break permutation;
						}
						
						Box box = permutation.get(index).getBox();
						if(rotations[index] % 2 == 0) {
							if(box.isSquare2D()) {
								// skip 2d rotation
								rotations[index]++;
								
								continue;
							}
							box.rotate2D();
						} else {
							if(box.isSquare3D()) {
								// skip 2d and 3d rotations
								rotations[index] = 6; 
								
								continue;
							}
							if(box.isSquare2D()) {
								box.rotate3D();
							} else {
								box.rotate2D3D();
							}
						}

						
						
						break;
					} while(true);
				}
			
			// clean up
			// reset rotation array
			System.arraycopy(reset, 0, rotations, 0, rotations.length);
		}
		return null;
	}	

	protected int rotate(int[] rotations) {
		// next rotation
		for(int i = 0; i < rotations.length; i++) {
			if(rotations[i] < (rotate3D ? 5 : 1)) { // 6 - 1 and 2 - 1 
				rotations[i]++;
				
				// reset all previous counter to zero
				for(int k = 0; k < i; k++) {
					rotations[k] = 0;
				}
				
				return i;
			}
		}		
		
		return -1;
	}
	
	protected void fit2D(List<Placement> containerProducts, Container holder, Placement usedSpace, long deadline) {
		// add used space box now
		// there is up to possible 2 free spaces
		holder.add(usedSpace);

		if(containerProducts.isEmpty()) {
			return;
		}

		if(System.currentTimeMillis() > deadline) {
			return;
		}

		Placement nextPlacement = containerProducts.get(0);

		if(!isFreespace(usedSpace.getSpace(), usedSpace.getBox(), nextPlacement)) {
			// no additional boxes
			// just make sure the used space fits in the free space
			return;
		} 
		// the correct space dimensions is copied into the next placement
		
		// holder.validateCurrentLevel(); // uncomment for debugging
		
		containerProducts.remove(0);

		// attempt to fit in the remaining (usually smaller) space first
		
		// stack in the 'sibling' space - the space left over between the used box and the selected free space
		if(!containerProducts.isEmpty()) {
			Space remainder = nextPlacement.getSpace().getRemainder();
			if(!remainder.isEmpty()) {
				Placement placement = containerProducts.get(0);
				
				if(placement.getBox().fitsInside3D(remainder)) {
					containerProducts.remove(0);
					
					placement.getSpace().copyFrom(remainder);
					placement.getSpace().setParent(remainder);
					placement.getSpace().getRemainder().setParent(remainder);
					
					fit2D(containerProducts, holder, placement, deadline);
				}
			}
		}
	
		// fit the next box in the selected free space
		fit2D(containerProducts, holder, nextPlacement, deadline);
	}	

	protected boolean isFreespace(Space freespace, Box used, Placement target) {

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
			
			// B
			if(freespace.getWidth() > used.getWidth()) {
				
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
				
			}
			
			// A
			if(freespace.getDepth() > used.getDepth()) {
				
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
				
			}
		}
		return false;
	}

}
