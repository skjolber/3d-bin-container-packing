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

public abstract class AbstractPackager<P extends PackResult, B extends PackagerResultBuilder<B>> implements Packager<B> {
	
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
	 * @param checkpointsPerDeadlineCheck number of deadline checks to skip, before checking again
	 */

	public AbstractPackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		if(containers.isEmpty()) {
			throw new IllegalArgumentException();
		}
		this.containers = containers.toArray(new Container[containers.size()]);
		if(this.containers.length == 0) {
			throw new RuntimeException();
		}
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
		return pack(boxes, BooleanSupplierBuilder.NOOP);
	}

	/**
	 * Return a container which holds all the boxes in the argument
	 *
	 * @param boxes    list of boxes to fit in a container
	 * @param deadline the system time in millis at which the search should be aborted
	 * @return index of container if match, -1 if not
	 */

	public Container pack(List<StackableItem> boxes, long deadline) {
		return pack(boxes, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
	}

	public Container pack(List<StackableItem> boxes, BooleanSupplier interrupt) {
		return packImpl(boxes, Arrays.asList(containers), interrupt);
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
		return pack(boxes, Arrays.asList(containers), deadline, interrupt);
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
		return packImpl(boxes, containers, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).build());
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
		return packImpl(boxes, containers, BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build());
	}

	protected Container packImpl(List<StackableItem> boxes, List<Container> candidateContainers, BooleanSupplier interrupt) {
		List<Container> containers = filterByVolumeAndWeight(toBoxes(boxes, false), candidateContainers, 1);
				
		if (containers.isEmpty()) {
			return null;
		}

		Adapter<P> pack = adapter(boxes, containers, interrupt);

		if (containers.size() <= 2) {
			for (int i = 0; i < containers.size(); i++) {
				if (interrupt.getAsBoolean()) {
					break;
				}

				P result = pack.attempt(i, null);
				if (result == null) {
					return null; // timeout, no result
				}
				if(result.containsLastStackable()) {
					return pack.accept(result);
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

				P bestResult = null;
				int bestIndex = Integer.MAX_VALUE;
				
				do {
					int next = iterator.next();
					int mid = containerIndexes.get(next);

					P result = pack.attempt(mid, bestResult);
					if (result == null) {
						// timeout 
						// return best result so far, whatever it is
						break search; 
					}
					checked[mid] = true;
					if (result.containsLastStackable()) {
						results[mid] = result;

						iterator.lower();
						
						if(mid < bestIndex) {
							bestIndex = mid;
							bestResult = result;
						}
					} else {
						iterator.higher();
					}
					if (interrupt.getAsBoolean()) {
						break search;
					}
				} while (iterator.hasNext());

				// halt when we have a result, and checked all containers at the lower indexes
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

			// return the best, if any
			for (final PackResult result : results) {
				if (result != null) {
					return pack.accept((P)result);
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

	public List<Container> packList(List<StackableItem> boxes, int limit) {
		return packList(boxes, limit, BooleanSupplierBuilder.NOOP);
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

		Adapter<P> pack = adapter(boxes, containers, interrupt);

		List<Container> containerPackResults = new ArrayList<>();

		// TODO binary search: not as simple as in the single-container use-case; discarding containers would need some kind
		// of criteria which could be trivially calculated, perhaps on volume.
		do {
			P best = null;
			for (int i = 0; i < containers.size(); i++) {

				if (interrupt.getAsBoolean()) {
					return null;
				}

				P result = pack.attempt(i, best);
				if (result == null) {
					return null; // timeout
				}
				if(!result.isEmpty()) {
					if(result.containsLastStackable()) {
						// will not match any better than this
						best = result;
						
						break;
					}
					
					if (best == null || result.isBetterThan(best)) {
						best = result;
					}
				}
			}

			if (best == null) {
				// negative result
				return null;
			}

			boolean end = best.containsLastStackable();

			containerPackResults.add(pack.accept(best));

			if (end) {
				// positive result
				return containerPackResults;
			}

		} while (containerPackResults.size() < limit);

		return null;
	}

	/**
	 * Return a list of containers which can potentially hold the boxes within the provided count
	 *
	 * @param boxes      list of boxes
	 * @param containers list of containers
	 * @param count      maximum number of possible containers
	 * @return list of containers
	 */
	private List<Container> filterByVolumeAndWeight(List<Stackable> boxes, List<Container> containers, int count) {
		long volume = 0;

		long weight = 0;

		for (Stackable box : boxes) {
			// volume
			long boxVolume = box.getVolume();
			volume += boxVolume;

			// weight
			long boxWeight = box.getWeight();
			weight += boxWeight;
		}

		List<Container> list = new ArrayList<>(containers.size());
		
		if (count == 1) {
			containers:
			for (Container container : containers) {
				if(container.getMaxLoadVolume() < volume) {
					continue;
				}
				if(container.getMaxLoadWeight() < weight) {
					continue;
				}
				
				for (Stackable box : boxes) {
					if (!container.canLoad(box)) {
						continue containers;
					}
				}
				list.add(container);
			}
			
		} else {
			long maxContainerLoadVolume = Long.MIN_VALUE;
			long maxContainerLoadWeight = Long.MIN_VALUE;

			for (Container container : containers) {
				// volume
				long boxVolume = container.getVolume();
				if (boxVolume > maxContainerLoadVolume) {
					maxContainerLoadVolume = boxVolume;
				}

				// weight
				long boxWeight = container.getMaxLoadWeight();
				if (boxWeight > maxContainerLoadWeight) {
					maxContainerLoadWeight = boxWeight;
				}
			}

			if (maxContainerLoadVolume * count < volume || maxContainerLoadWeight * count < weight) {
				// no containers will work at current count
				return Collections.emptyList();
			}
			
			long minVolume = Long.MAX_VALUE;
			long minWeight = Long.MAX_VALUE;

			for (Stackable box : boxes) {
				// volume
				long boxVolume = box.getVolume();
				if (boxVolume < minVolume) {
					minVolume = boxVolume;
				}

				// weight
				long boxWeight = box.getWeight();
				if (boxWeight < minWeight) {
					minWeight = boxWeight;
				}
			}
			
			for (Container container : containers) {
				if (container.getMaxLoadVolume() < minVolume || container.getMaxLoadWeight() < minWeight) {
					// this container cannot even fit a single box
					continue;
				}

				if (container.getMaxLoadVolume() + maxContainerLoadVolume * (count - 1) < volume || container.getMaxLoadWeight() + maxContainerLoadWeight * (count - 1) < weight) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}
				
				if (!canLoadAtLeastOne(container, boxes)) {
					continue;
				}
				
				list.add(container);
			}
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

	private boolean canLoadAtLeastOne(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if (containerBox.canLoad(box)) {
				return true;
			}			
		}
		return false;
	}

	protected abstract Adapter<P> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt);

	protected long getMinStackableItemVolume(List<StackableItem> stackables) {
		long minVolume = Integer.MAX_VALUE;
		for(StackableItem stackableItem : stackables) {
			Stackable stackable = stackableItem.getStackable();
			if(stackable.getVolume() < minVolume) {
				minVolume = stackable.getVolume();
			}
		}
		return minVolume;
	}
	
	protected long getMinStackableItemArea(List<StackableItem> stackables) {
		long minArea = Integer.MAX_VALUE;
		for(StackableItem stackableItem : stackables) {
			Stackable stackable = stackableItem.getStackable();
			if(stackable.getMinimumArea() < minArea) {
				minArea = stackable.getMinimumArea();
			}
		}
		return minArea;
	}
	
	protected long getMinStackableVolume(List<Stackable> stackables) {
		long minVolume = Integer.MAX_VALUE;
		for(Stackable stackable : stackables) {
			if(stackable.getVolume() < minVolume) {
				minVolume = stackable.getVolume();
			}
		}
		return minVolume;
	}
	

	protected long getMinStackableArea(List<Stackable> stackables) {
		long minArea = Integer.MAX_VALUE;
		for(Stackable stackable : stackables) {
			if(stackable.getMinimumArea() < minArea) {
				minArea = stackable.getMinimumArea();
			}
		}
		return minArea;
	}
	
}
