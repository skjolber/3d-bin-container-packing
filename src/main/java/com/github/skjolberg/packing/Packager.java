package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.Collections;
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
	
	public interface Adapter {
		PackResult pack(List<BoxItem> boxes, Container dimension, long deadline);
	}
	
	public interface PackResult {
		boolean isFull();
		Container getContainer();
		boolean packsMoreBoxesThan(PackResult result);
		// TODO better in weight and also volume
	}
	
	protected final Container[] containers;

	protected final long maxVolume;
	protected final long maxWeight;

	protected final boolean rotate3D; // if false, then 2d
	protected final boolean binarySearch;
	
	/**
	 * Constructor
	 * 
	 * @param containers list of containers
	 */
	public Packager(List<Container> containers) {
		this(containers, true, true);
	}
	
	/**
	 * Constructor
	 * 
	 * @param containers list of containers
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a match, it searches the preceding boxes as well, until the deadline is passed. 
	 */

	public Packager(List<Container> containers, boolean rotate3D, boolean binarySearch) {
		this.containers = containers.toArray(new Container[containers.size()]);
		this.rotate3D = rotate3D;
		this.binarySearch = binarySearch;

		long maxVolume = Long.MIN_VALUE;
		long maxWeight = Long.MIN_VALUE;

		for(Container container : containers) {
			// volume
			long boxVolume = container.getVolume();
			if(boxVolume > maxVolume) {
				maxVolume = boxVolume;
			}
			
			// weight
			long boxWeight = container.getWeight();
			if(boxWeight > maxWeight) {
				maxWeight = boxWeight;
			}
		}

		this.maxVolume = maxVolume;
		this.maxWeight = maxWeight;
	}
	
	/**
	 * 
	 * Return a container which holds all the boxes in the argument.
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @return null if no match
	 */
	
	public Container pack(List<BoxItem> boxes) {
		return pack(boxes, Long.MAX_VALUE);
	}

	/**
	 * Return a list of containers which can potentially hold the boxes.
	 * 
	 * @param boxes list of boxes
	 * @return list of containers
	 */
	
	public List<Container> filterByVolumeAndWeight(List<BoxItem> boxes, int count) {
		long volume = 0;
		long minVolume = Long.MAX_VALUE;
		
		long weight = 0;
		long minWeight = Long.MAX_VALUE;

		for(BoxItem box : boxes) {
			// volume
			long boxVolume = box.getBox().getVolume();
			volume += boxVolume * box.getCount();
			
			if(boxVolume < minVolume) {
				minVolume = boxVolume;
			}
			
			// weight
			long boxWeight = box.getBox().getWeight();
			weight += boxWeight;
					
			if(boxWeight < minWeight) {
				minWeight = boxWeight;
			}
		}

		if(maxVolume * count < volume || maxWeight * count < weight) { // XXX FEILER
			// no containers will work at current count
			return Collections.emptyList();
		}
		
		List<Container> list = new ArrayList<>(containers.length);
		for(Container container : containers) {
			if(container.getVolume() < minVolume || container.getWeight() < minWeight) {
				// this box cannot even fit a single box
				continue;
			}

			if(container.getVolume() + maxVolume * (count - 1) < volume || container.getWeight() + maxWeight * (count - 1) < weight) {
				// this box cannot be used even together with all biggest boxes
				continue;
			}

			if(count == 1) {
				if(!canHoldAll(container, boxes)) {
					continue;
				}
			} else {
				if(!canHoldAtLeastOne(container, boxes)) {
					continue;
				}
			}

			list.add(container);
		}
		
		return list;
	}

	/**
	 * Return a list of containers which can potentially hold the boxes.
	 * 
	 * @param boxes list of boxes
	 * @return list of containers
	 */
	
	public List<Container> filterByVolume(List<BoxItem> boxes) {
		long volume = 0;
		for(BoxItem box : boxes) {
			volume += box.getBox().getVolume() * box.getCount();
		}
		
		List<Container> list = new ArrayList<>();
		for(Container container : containers) {
			if(container.getVolume() < volume || !canHoldAll(container, boxes)) {
				// discard this container
				continue;
			}

			list.add(container);
		}
		
		return list;
	}

	/**
	 * Return a list of containers which can at least hold one of the boxes
	 * 
	 * @param boxes list of boxes
	 * @return list of containers
	 */
	
	public void filterBySize(List<BoxItem> boxes, List<Container> containers) {
		for (int i = 0; i < containers.size(); i++) {
			Container dimension = containers.get(i);
			if(!canHoldAll(dimension, boxes)) {
				// discard this container
				containers.remove(i);
				i--;
			}
		}
	}
	

	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	
	public Container pack(List<BoxItem> boxes, long deadline) {
		return pack(boxes, filterByVolumeAndWeight(boxes, 1), deadline);
	}

	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param limit maximum number of containers
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	
	public Container pack(List<BoxItem> boxes, int limit, long deadline) {
		return pack(boxes, filterByVolumeAndWeight(boxes, limit), deadline);
	}

	/**
	 * 
	 * Return a container which holds all the boxes in the argument
	 * 
	 * @param boxes list of boxes to fit in a container
	 * @param container list of containers
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	
	public Container pack(List<BoxItem> boxes, List<Container> container, long deadline) {
		if(container.isEmpty()) {
			return null;
		}
		
		Adapter pack = adapter(boxes);

		if(!binarySearch || container.size() <= 2 || deadline == Long.MAX_VALUE) {
			for (int i = 0; i < container.size(); i++) {
					
				if(System.currentTimeMillis() > deadline) {
					break;
				}
				
				PackResult result = pack.pack(boxes, container.get(i), deadline);
				if(result == null) {
					return null; // timeout
				}

				if(result.isFull()) {
					return result.getContainer();
				}
			}
		} else {
			// perform a binary search among the available containers
			// the list is ranked from most desirable to least.
			Container[] results = new Container[container.size()];
			boolean[] checked = new boolean[results.length]; 

			ArrayList<Integer> current = new ArrayList<>(container.size());
			for(int i = 0; i < container.size(); i++) {
				current.add(i);
			}

			BinarySearchIterator iterator = new BinarySearchIterator();

			search:
			do {
				iterator.reset(current.size() - 1, 0);
				
				do {
					int next = iterator.next();
					int mid = current.get(next);

					PackResult result = pack.pack(boxes, container.get(mid), deadline);
					if(result == null) {
						return null; // timeout
					}
					checked[mid] = true;
					if(result.isFull()) {
						results[mid] = result.getContainer();
						
						iterator.lower();
					} else {
						iterator.higher();
					}
					if(System.currentTimeMillis() > deadline) {
						break search;
					}
				} while(iterator.hasNext()); 
				
		        // halt when have a result, and checked all containers at the lower indexes
		        for (int i = 0; i < current.size(); i++) {
		        	Integer integer = current.get(i);
					if(results[integer] != null) {
						// remove end items; we already have a better match
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
	
	protected abstract Adapter adapter(List<BoxItem> boxes);

	protected boolean canHoldAll(Container containerBox, List<BoxItem> boxes) {
		for(BoxItem box : boxes) {
			if(containerBox.getWeight() < box.getBox().getWeight()) {
				continue;
			}
			if(rotate3D) {
				if(!containerBox.canHold3D(box.getBox())) {
					return false;
				}
			} else {
				if(!containerBox.canHold2D(box.getBox())) {
					return false;
				}
			}
		}
		return true;
	}
	

	protected boolean canHoldAtLeastOne(Container containerBox, List<BoxItem> boxes) {
		for(BoxItem box : boxes) {
			if(containerBox.getWeight() < box.getBox().getWeight()) {
				continue;
			}
			if(rotate3D) {
				if(containerBox.canHold3D(box.getBox())) {
					return true;
				}
			} else {
				if(containerBox.canHold2D(box.getBox())) {
					return true;
				}
			}
		}
		return false;
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
