package com.github.skjolberg.packing;

import java.util.ArrayList;
import java.util.Arrays;
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
		void initialize(List<BoxItem> boxes, List<Container> container);
		Container accepted(PackResult result);
		PackResult attempt(int containerIndex, long deadline);
		boolean hasMore(PackResult result);
	}

	public interface PackResult {
		/**
		 * Compare two results
		 *
		 * @param result to compare against
		 * @return true if this box is better than the argument
		 */
		boolean packsMoreBoxesThan(PackResult result);
		boolean isEmpty();
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
	 * @param containers list of containers
	 * @param count maximum number of possible containers
	 * @return list of containers
	 */

	public List<Container> filterByVolumeAndWeight(List<Box> boxes, List<Container> containers, int count) {
		long volume = 0;
		long minVolume = Long.MAX_VALUE;

		long weight = 0;
		long minWeight = Long.MAX_VALUE;

		for(Box box : boxes) {
			// volume
			long boxVolume = box.getVolume();
			volume += boxVolume;

			if(boxVolume < minVolume) {
				minVolume = boxVolume;
			}

			// weight
			long boxWeight = box.getWeight();
			weight += boxWeight;

			if(boxWeight < minWeight) {
				minWeight = boxWeight;
			}
		}

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

		if(maxVolume * count < volume || maxWeight * count < weight) {
			// no containers will work at current count
			return Collections.emptyList();
		}

		List<Container> list = new ArrayList<>(containers.size());
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

	public List<Container> filterByVolume(List<Box> boxes) {
		long volume = 0;
		for(Box box : boxes) {
			volume += box.getVolume();
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
	 * @param containers list of containers
	 */

	public void filterBySize(List<Box> boxes, List<Container> containers) {
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
		return pack(boxes, filterByVolumeAndWeight(toBoxes(boxes, false), Arrays.asList(containers), 1), deadline);
	}

	protected static List<Box> toBoxes(List<BoxItem> boxItems, boolean clone) {
		List<Box> boxClones = new ArrayList<>(boxItems.size() * 2);

		for(BoxItem item : boxItems) {
			Box box = item.getBox();
			boxClones.add(box);
			for(int i = 1; i < item.getCount(); i++) {
				boxClones.add(clone ? box : box.clone());
			}
		}
		return boxClones;

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
		return pack(boxes, filterByVolumeAndWeight(toBoxes(boxes, false), Arrays.asList(containers), limit), deadline);
	}

	/**
	 *
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes list of boxes to fit in a container
	 * @param containers list of containers
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */

	public Container pack(List<BoxItem> boxes, List<Container> containers, long deadline) {
		if(containers.isEmpty()) {
			return null;
		}

		Adapter pack = adapter();
		pack.initialize(boxes, containers);

		if(!binarySearch || containers.size() <= 2 || deadline == Long.MAX_VALUE) {
			for (int i = 0; i < containers.size(); i++) {

				if(System.currentTimeMillis() > deadline) {
					break;
				}

				PackResult result = pack.attempt(i, deadline);
				if(result == null) {
					return null; // timeout
				}

				if(!pack.hasMore(result)) {
					return pack.accepted(result);
				}
			}
		} else {
			// perform a binary search among the available containers
			// the list is ranked from most desirable to least.
			PackResult[] results = new PackResult[containers.size()];
			boolean[] checked = new boolean[results.length];

			ArrayList<Integer> containerIndexes = new ArrayList<>(containers.size());
			for(int i = 0; i < containers.size(); i++) {
				containerIndexes.add(i);
			}

			BinarySearchIterator iterator = new BinarySearchIterator();

			search:
			do {
				iterator.reset(containerIndexes.size() - 1, 0);

				do {
					int next = iterator.next();
					int mid = containerIndexes.get(next);

					PackResult result = pack.attempt(mid, deadline);
					if(result == null) {
						return null; // timeout
					}
					checked[mid] = true;
					if(!pack.hasMore(result)) {
						results[mid] = result;

						iterator.lower();
					} else {
						iterator.higher();
					}
					if(System.currentTimeMillis() > deadline) {
						break search;
					}
				} while(iterator.hasNext());

		        // halt when have a result, and checked all containers at the lower indexes
		        for (int i = 0; i < containerIndexes.size(); i++) {
		        	Integer integer = containerIndexes.get(i);
					if(results[integer] != null) {
						// remove end items; we already have a better match
						while(containerIndexes.size() > i) {
							containerIndexes.remove(containerIndexes.size() - 1);
						}
						break;
					}

					// remove item
					if(checked[integer]) {
						containerIndexes.remove(i);
						i--;
					}
		        }
	        } while(!containerIndexes.isEmpty());

			for(int i = 0; i < results.length; i++) {
				if(results[i] != null) {
					return pack.accepted(results[i]);
				}
			}
		}
		return null;
	}

	protected abstract Adapter adapter();

	protected boolean canHoldAll(Container containerBox, List<Box> boxes) {
		for(Box box : boxes) {
			if(containerBox.getWeight() < box.getWeight()) {
				continue;
			}
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


	protected boolean canHoldAtLeastOne(Container containerBox, List<Box> boxes) {
		for(Box box : boxes) {
			if(containerBox.getWeight() < box.getWeight()) {
				continue;
			}
			if(rotate3D) {
				if(containerBox.canHold3D(box)) {
					return true;
				}
			} else {
				if(containerBox.canHold2D(box)) {
					return true;
				}
			}
		}
		return false;
	}


	public static List<Placement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<Placement> placements = new ArrayList<>(size);

		for(int i = 0; i < size; i++) {
			Space a = new Space();
			Space b = new Space();
			a.setRemainder(b);
			b.setRemainder(a);

			placements.add(new Placement(a));
		}
		return placements;
	}

	public List<Container> packList(List<BoxItem> boxes, int limit) {
		return packList(boxes, limit, Long.MAX_VALUE);
	}

	/**
	 *
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes list of boxes to fit in a container
	 * @param limit maximum number of containers
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */

	public List<Container> packList(List<BoxItem> boxes, int limit, long deadline) {

		List<Container> containers = filterByVolumeAndWeight(toBoxes(boxes, true), Arrays.asList(this.containers), limit);
		if(containers.isEmpty()) {
			return null;
		}

		Adapter pack = adapter();
		pack.initialize(boxes, containers);

		List<Container> containerPackResults = new ArrayList<>();

		// binary search: not as simple as in the single-container use-case; discarding containers would need some kind
		// of criteria which could be trivially calculated, perhaps on volume.
		do {
			PackResult best = null;
			for (int i = 0; i < containers.size(); i++) {

				if(System.currentTimeMillis() > deadline) {
					return null;
				}

				PackResult result = pack.attempt(i, deadline);
				if(result == null) {
					return null; // timeout
				}

				if(!result.isEmpty()) {
					if(best == null || result.packsMoreBoxesThan(best)) {
						best = result;

						if(!pack.hasMore(best)) { // will not match any better than this
							break;
						}
					}
				}
			}

			if(best == null) {
				// negative result
				return null;
			}

			boolean end = !pack.hasMore(best);

			containerPackResults.add(pack.accepted(best));

			if(end) {
				// positive result
				return containerPackResults;
			}

		} while(containerPackResults.size() < limit);

		return null;
	}


}
