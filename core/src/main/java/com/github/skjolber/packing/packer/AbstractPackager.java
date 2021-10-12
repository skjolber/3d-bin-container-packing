package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.iterator.BinarySearchIterator;


/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 */

public abstract class AbstractPackager<B extends PackagerResultBuilder<B>> implements Packager<B> {
	
	protected static final EmptyPackResult EMPTY_PACK_RESULT = new EmptyPackResult();
	
	protected final Container[] containers;
	
	/** limit the number of calls to get System.currentTimeMillis() */
	protected final int checkpointsPerDeadlineCheck;

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 */

	public AbstractPackager(List<Container> containers) {
		this(containers, 1);
	}

	/**
	 * Constructor
	 *
	 * @param containers   list of containers
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a
	 *                     match, it searches the preceding boxes as well, until the deadline is passed.
	 * @param checkpointsPerDeadlineCheck number of deadline checks to skip, before checking again
	 */

	public AbstractPackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		this.containers = containers.toArray(new Container[0]);
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;

		long maxVolume = Long.MIN_VALUE;
		long maxWeight = Long.MIN_VALUE;

		for (Container container : containers) {
			// volume
			long boxVolume = container.getVolume();
			if (boxVolume > maxVolume) {
				maxVolume = boxVolume;
			}

			// weight
			long boxWeight = container.getWeight();
			if (boxWeight > maxWeight) {
				maxWeight = boxWeight;
			}
		}
	}

	/**
	 * Return a container which holds all the boxes in the argument.
	 *
	 * @param boxes list of boxes to fit in a container
	 * @return null if no match
	 */
	public Container pack(List<StackableItem> boxes) {
		return pack(boxes, Long.MAX_VALUE);
	}

	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes    list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */

	public Container pack(List<StackableItem> boxes, long deadline) {
		return pack(boxes, filterByVolumeAndWeight(toBoxes(boxes, false), Arrays.asList(containers), 1), BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public Container pack(List<StackableItem> boxes, BooleanSupplier interrupt) {
		return pack(boxes, filterByVolumeAndWeight(toBoxes(boxes, false), Arrays.asList(containers), 1), interrupt);
	}

	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes     list of boxes to fit in a container
	 * @param deadline  the system time in millis at which the search should be aborted
	 * @param interrupt When true, the computation is interrupted as soon as possible.
	 * @return index of container if match, -1 if not
	 * 
	 */
	public Container pack(List<StackableItem> boxes, long deadline, BooleanSupplier interrupt) {
		return pack(boxes, filterByVolumeAndWeight(toBoxes(boxes, false), Arrays.asList(containers), 1), deadline, interrupt);
	}

	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes      list of boxes to fit in a container
	 * @param containers list of containers
	 * @param deadline   the system time in milliseconds at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	public Container pack(List<StackableItem> boxes, List<Container> containers, long deadline) {
		return pack(boxes, containers, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes      list of boxes to fit in a container
	 * @param containers list of containers
	 * @param deadline   the system time in milliseconds at which the search should be aborted
	 * @param interrupt  When true, the computation is interrupted as soon as possible.
	 * @return index of container if match, -1 if not
	 */
	public Container pack(List<StackableItem> boxes, List<Container> containers, long deadline, BooleanSupplier interrupt) {
		return pack(boxes, containers, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	public Container pack(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		if (containers.isEmpty()) {
			return null;
		}

		Adapter pack = adapter(boxes, containers, interrupt);

		if (containers.size() <= 2) {
			for (int i = 0; i < containers.size(); i++) {

				if (interrupt.getAsBoolean()) {
					break;
				}

				PackResult result = pack.attempt(i);
				if (result == null) {
					return null; // timeout
				}

				if (!pack.hasMore(result)) {
					return pack.accepted(result);
				}
			}
		} else {
			// perform a binary search among the available containers
			// the list is ranked from most desirable to least.
			PackResult[] results = new PackResult[containers.size()];
			boolean[] checked = new boolean[results.length];

			ArrayList<Integer> containerIndexes = new ArrayList<>(containers.size());
			for (int i = 0; i < containers.size(); i++) {
				containerIndexes.add(i);
			}

			BinarySearchIterator iterator = new BinarySearchIterator();

			search:
			do {
				iterator.reset(containerIndexes.size() - 1, 0);

				do {
					int next = iterator.next();
					int mid = containerIndexes.get(next);

					PackResult result = pack.attempt(mid);
					if (result == null) {
						return null; // timeout
					}
					checked[mid] = true;
					if (!pack.hasMore(result)) {
						results[mid] = result;

						iterator.lower();
					} else {
						iterator.higher();
					}
					if (interrupt.getAsBoolean()) {
						break search;
					}
				} while (iterator.hasNext());

				// halt when have a result, and checked all containers at the lower indexes
				for (int i = 0; i < containerIndexes.size(); i++) {
					Integer integer = containerIndexes.get(i);
					if (results[integer] != null) {
						// remove end items; we already have a better match
						while (containerIndexes.size() > i) {
							containerIndexes.remove(containerIndexes.size() - 1);
						}
						break;
					}

					// remove item
					if (checked[integer]) {
						containerIndexes.remove(i);
						i--;
					}
				}
			} while (!containerIndexes.isEmpty());

			for (final PackResult result : results) {
				if (result != null) {
					return pack.accepted(result);
				}
			}
		}
		return null;
	}

	/**
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes    list of boxes to fit in a container
	 * @param limit    maximum number of containers
	 * @param deadline the system time in milliseconds at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */
	public List<Container> packList(List<StackableItem> boxes, int limit, long deadline) {
		return packList(boxes, limit, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}



	/**
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes     list of boxes to fit in a container
	 * @param limit     maximum number of containers
	 * @param deadline  the system time in milliseconds at which the search should be aborted
	 * @param interrupt When true, the computation is interrupted as soon as possible.
	 * @return index of container if match, -1 if not
	 */
	public List<Container> packList(List<StackableItem> boxes, int limit, long deadline, BooleanSupplier interrupt) {
		return packList(boxes, limit, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	/**
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes     list of boxes to fit in a container
	 * @param limit     maximum number of containers
	 * @param interrupt When true, the computation is interrupted as soon as possible.
	 * @return index of container if match, -1 if not
	 */
	public List<Container> packList(List<StackableItem> boxes, int limit, BooleanSupplier interrupt) {
		List<Container> containers = filterByVolumeAndWeight(toBoxes(boxes, true), Arrays.asList(this.containers), limit);
		if (containers.isEmpty()) {
			return null;
		}

		Adapter pack = adapter(boxes, containers, interrupt);

		List<Container> containerPackResults = new ArrayList<>();

		// binary search: not as simple as in the single-container use-case; discarding containers would need some kind
		// of criteria which could be trivially calculated, perhaps on volume.
		do {
			PackResult best = null;
			for (int i = 0; i < containers.size(); i++) {

				if (interrupt.getAsBoolean()) {
					return null;
				}

				PackResult result = pack.attempt(i);
				if (result == null) {
					return null; // timeout
				}

				if (!result.isEmpty()) {
					if (best == null || result.packsMoreBoxesThan(best)) {
						best = result;

						if (!pack.hasMore(best)) { // will not match any better than this
							break;
						}
					}
				}
			}

			if (best == null) {
				// negative result
				return null;
			}

			boolean end = !pack.hasMore(best);

			containerPackResults.add(pack.accepted(best));

			if (end) {
				// positive result
				return containerPackResults;
			}

		} while (containerPackResults.size() < limit);

		return null;
	}

	/**
	 * Return a list of containers which can potentially hold the boxes.
	 *
	 * @param boxes      list of boxes
	 * @param containers list of containers
	 * @param count      maximum number of possible containers
	 * @return list of containers
	 */
	private List<Container> filterByVolumeAndWeight(List<Stackable> boxes, List<Container> containers, int count) {
		long volume = 0;
		long minVolume = Long.MAX_VALUE;

		long weight = 0;
		long minWeight = Long.MAX_VALUE;

		for (Stackable box : boxes) {
			// volume
			long boxVolume = box.getVolume();
			volume += boxVolume;

			if (boxVolume < minVolume) {
				minVolume = boxVolume;
			}

			// weight
			long boxWeight = box.getWeight();
			weight += boxWeight;

			if (boxWeight < minWeight) {
				minWeight = boxWeight;
			}
		}

		long maxVolume = Long.MIN_VALUE;
		long maxWeight = Long.MIN_VALUE;

		for (Container container : containers) {
			// volume
			long boxVolume = container.getVolume();
			if (boxVolume > maxVolume) {
				maxVolume = boxVolume;
			}

			// weight
			long boxWeight = container.getWeight();
			if (boxWeight > maxWeight) {
				maxWeight = boxWeight;
			}
		}

		if (maxVolume * count < volume || maxWeight * count < weight) {
			// no containers will work at current count
			return Collections.emptyList();
		}

		List<Container> list = new ArrayList<>(containers.size());
		for (Container container : containers) {
			if (container.getVolume() < minVolume || container.getWeight() < minWeight) {
				// this box cannot even fit a single box
				continue;
			}

			if (container.getVolume() + maxVolume * (count - 1) < volume || container.getWeight() + maxWeight * (count - 1) < weight) {
				// this box cannot be used even together with all biggest boxes
				continue;
			}

			if (count == 1) {
				if (!canHoldAll(container, boxes)) {
					continue;
				}
			} else {
				if (!canHoldAtLeastOne(container, boxes)) {
					continue;
				}
			}

			list.add(container);
		}

		return list;
	}


	private static List<Stackable> toBoxes(List<StackableItem> StackableItems, boolean clone) {
		List<Stackable> boxClones = new ArrayList<>(StackableItems.size() * 2);

		for (StackableItem item : StackableItems) {
			Stackable box = item.getStackable();
			boxClones.add(box);
			for (int i = 1; i < item.getCount(); i++) {
				boxClones.add(clone ? box : box.clone());
			}
		}
		return boxClones;

	}


	protected abstract Adapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt);

	private boolean canHoldAll(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if (containerBox.getWeight() < box.getWeight()) {
				continue;
			}
			if (!containerBox.canLoad(box)) {
				return false;
			}
		}
		return true;
	}

	private boolean canHoldAtLeastOne(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if (containerBox.getWeight() < box.getWeight()) {
				continue;
			}
			if (containerBox.canLoad(box)) {
				return true;
			}			
		}
		return false;
	}

	/*
	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			StackPlacement placement = new StackPlacement();
			placement.setSpace(new StackSpace());
			placements.add(placement);
		}
		return placements;
	}
*/

}
