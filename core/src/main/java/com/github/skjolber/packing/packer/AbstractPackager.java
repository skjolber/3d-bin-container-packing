package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.PackResultComparator;
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

public abstract class AbstractPackager<P extends PackResult, B extends PackagerResultBuilder<B>> implements Packager<B> {

	protected static final EmptyPackResult EMPTY_PACK_RESULT = EmptyPackResult.EMPTY;

	protected final PackResultComparator packResultComparator;

	/** limit the number of calls to get System.currentTimeMillis() */
	protected final int checkpointsPerDeadlineCheck;

	/**
	 * Constructor
	 *
	 * @param containers                  list of containers
	 * @param checkpointsPerDeadlineCheck number of deadline checks to skip, before checking again
	 */

	public AbstractPackager(int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		this.checkpointsPerDeadlineCheck = checkpointsPerDeadlineCheck;
		this.packResultComparator = packResultComparator;
	}

	public Container pack(List<StackableItem> products, List<ContainerItem> containers) {
		return pack(products, containers, BooleanSupplierBuilder.NOOP);
	}
	
	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes      list of boxes to fit in a container
	 * @param containers list of containers
	 * @param interrupt  When true, the computation is interrupted as soon as possible.
	 * @return list of containers, or null if the deadline was reached / the packages could not be packaged within the available containers and/or limit
	 */

	public Container pack(List<StackableItem> boxes, List<ContainerItem> containersItems, BooleanSupplier interrupt) {
		
		Adapter<P> pack = adapter(boxes, containersItems, interrupt);
		
		List<Integer> containerIndexes = pack.getContainers(1);
		if(containerIndexes.isEmpty()) {
			return null;
		}
		
		if(containerIndexes.size() <= 2) {
			for (int i = 0; i < containerIndexes.size(); i++) {
				if(interrupt.getAsBoolean()) {
					break;
				}

				Integer index = containerIndexes.get(i);
				
				P result = pack.attempt(index, null);
				if(result == null) {
					return null; // timeout, no result
				}
				if(result.containsLastStackable()) {
					return pack.accept(result);
				}
			}
		} else {
			// perform a binary search among the available containers
			// the list is ranked from most desirable to least.
			PackResult[] results = new PackResult[containerIndexes.size()];
			boolean[] checked = new boolean[results.length];

			BinarySearchIterator iterator = new BinarySearchIterator();

			search: do {
				iterator.reset(containerIndexes.size() - 1, 0);

				P bestResult = null;
				int bestIndex = Integer.MAX_VALUE;

				do {
					int next = iterator.next();
					int mid = containerIndexes.get(next);

					P result = pack.attempt(mid, bestResult);
					if(result == null) {
						// timeout 
						// return best result so far, whatever it is
						break search;
					}
					checked[mid] = true;
					if(result.containsLastStackable()) {
						results[mid] = result;

						iterator.lower();

						if(mid < bestIndex) {
							bestIndex = mid;
							bestResult = result;
						}
					} else {
						iterator.higher();
					}
					if(interrupt.getAsBoolean()) {
						break search;
					}
				} while (iterator.hasNext());

				// halt when we have a result, and checked all containers at the lower indexes
				for (int i = 0; i < containerIndexes.size(); i++) {
					Integer integer = containerIndexes.get(i);
					if(results[integer] != null) {
						// remove end items; we already have a better match
						while (containerIndexes.size() > i) {
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
			} while (!containerIndexes.isEmpty());

			// return the best, if any
			for (final PackResult result : results) {
				if(result != null) {
					return pack.accept((P)result);
				}
			}
		}
		return null;
	}

	/**
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes     list of boxes to fit in a container
	 * @param limit     maximum number of containers
	 * @param interrupt When true, the computation is interrupted as soon as possible.
	 * @return list of containers, or null if the deadline was reached / the packages could not be packaged within the available containers and/or limit
	 */
	public List<Container> packList(List<StackableItem> boxes, List<ContainerItem> containerItems, int limit, BooleanSupplier interrupt) {
		Adapter<P> pack = adapter(boxes, containerItems, interrupt);

		List<Container> containerPackResults = new ArrayList<>();

		// TODO binary search: not as simple as in the single-container use-case; discarding containers would need some kind
		// of criteria which could be trivially calculated, perhaps on volume.
		do {
			
			List<Integer> containerIndexes = pack.getContainers(limit - containerPackResults.size());
			if(containerIndexes.isEmpty()) {
				return null;
			}
			
			P best = null;
			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = containerItems.get(i);
				if(!item.isAvailable()) {
					continue;
				}
				
				if(interrupt.getAsBoolean()) {
					return null;
				}

				P result = pack.attempt(i, best);
				if(result == null) {
					return null; // timeout
				}
				if(!result.isEmpty()) {
					if(result.containsLastStackable()) {
						// will not match any better than this
						best = result;

						break;
					}

					if(best == null || packResultComparator.compare(best, result) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
						best = result;
					}
				}
			}

			if(best == null) {
				// negative result
				return null;
			}

			boolean end = best.containsLastStackable();

			containerPackResults.add(pack.accept(best));
			
			if(end) {
				// positive result
				return containerPackResults;
			}

			// TODO recalculate the in-scope containers
		} while (containerPackResults.size() < limit);

		return null;
	}

	protected abstract Adapter<P> adapter(List<StackableItem> boxes, List<ContainerItem> containers, BooleanSupplier interrupt);

	protected long getMinStackableItemVolume(List<StackableItem> stackables) {
		long minVolume = Integer.MAX_VALUE;
		for (StackableItem stackableItem : stackables) {
			Stackable stackable = stackableItem.getStackable();
			if(stackable.getVolume() < minVolume) {
				minVolume = stackable.getVolume();
			}
		}
		return minVolume;
	}

	protected long getMinStackableItemArea(List<StackableItem> stackables) {
		long minArea = Integer.MAX_VALUE;
		for (StackableItem stackableItem : stackables) {
			Stackable stackable = stackableItem.getStackable();
			if(stackable.getMinimumArea() < minArea) {
				minArea = stackable.getMinimumArea();
			}
		}
		return minArea;
	}

	protected long getMinStackableVolume(List<Stackable> stackables) {
		long minVolume = Integer.MAX_VALUE;
		for (Stackable stackable : stackables) {
			if(stackable.getVolume() < minVolume) {
				minVolume = stackable.getVolume();
			}
		}
		return minVolume;
	}

	protected long getMinStackableArea(List<Stackable> stackables) {
		long minArea = Integer.MAX_VALUE;
		for (Stackable stackable : stackables) {
			if(stackable.getMinimumArea() < minArea) {
				minArea = stackable.getMinimumArea();
			}
		}
		return minArea;
	}

}
