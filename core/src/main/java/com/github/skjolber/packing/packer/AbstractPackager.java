package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.PackResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.iterator.BinarySearchIterator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 */

public abstract class AbstractPackager<P extends IntermediatePackagerResult, B extends PackagerResultBuilder<B>> implements Packager<B> {

	protected static final EmptyPackagerResultAdapter EMPTY_PACK_RESULT = EmptyPackagerResultAdapter.EMPTY;

	protected final IntermediatePackagerResultComparator intermediatePackagerResultComparator;
	
	protected final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE);

	/**
	 * Constructor
	 *
	 * @param packResultComparator result comparator
	 */

	public AbstractPackager(IntermediatePackagerResultComparator packResultComparator) {
		this.intermediatePackagerResultComparator = packResultComparator;
	}

	// pack in single container
	protected P packSingle(List<Integer> containerItemIndexes, PackagerAdapter<P> adapter, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(containerItemIndexes.size() <= 2) {
			for (int i = 0; i < containerItemIndexes.size(); i++) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}

				Integer containerItemIndex = containerItemIndexes.get(i);

				P result = adapter.attempt(containerItemIndex, null);
				if(result == null) {
					return null; // timeout, no result
				}
				if(result.getStack().size() == adapter.countRemainingBoxes()) {
					return result;
				}
			}
		} else {
			// perform a binary search among the available containers
			// the list is ranked from most desirable to least.
			// while the search finds a baseline, we really need to check all the containers
			// at a lower index before the optional container is located.
			
			IntermediatePackagerResult[] results = new IntermediatePackagerResult[containerItemIndexes.get(containerItemIndexes.size() - 1) + 1];

			BinarySearchIterator iterator = new BinarySearchIterator();

			search: do {
				iterator.reset(containerItemIndexes.size() - 1, 0);

				P bestResult = null;
				int bestIndex = Integer.MAX_VALUE;

				do {
					int mid = iterator.next();
					int nextContainerItemIndex = containerItemIndexes.get(mid);

					P result = adapter.attempt(nextContainerItemIndex, bestResult);
					if(result == null) {
						// timeout 
						// return best result so far, whatever it is
						break search;
					}
					if(result.getStack().size() == adapter.countRemainingBoxes()) {
						results[nextContainerItemIndex] = result;

						iterator.lower();

						if(mid < bestIndex) {
							bestIndex = mid;
							bestResult = result;
						}
					} else {
						// count as empty
						results[nextContainerItemIndex] = EMPTY_PACK_RESULT;
								
						iterator.higher();
					}
					if(interrupt.getAsBoolean()) {
						break search;
					}
				} while (iterator.hasNext());
				
				for (int i = 0; i < containerItemIndexes.size(); i++) {
					Integer nextContainerItemIndex = containerItemIndexes.get(i);
					if(results[nextContainerItemIndex] != null) {
						if(!results[nextContainerItemIndex].isEmpty()) {
							// remove containers at lower indexes; we already have a better match
							while (containerItemIndexes.size() > i) {
								containerItemIndexes.remove(containerItemIndexes.size() - 1);
							}
							break;
						}
						
						// remove container which could not fit all the items
						containerItemIndexes.remove(i);
						i--;
					}
				}
				// halt when not more containers to check
			} while (!containerItemIndexes.isEmpty());

			// return the best, if any
			for (final IntermediatePackagerResult result : results) {
				if(result != null && !result.isEmpty()) {
					return (P)result;
				}
			}
		}
		return (P) EMPTY_PACK_RESULT;
	}

	public List<Container> packList(List<BoxItemGroup> boxes, Order itemGroupOrder, List<CompositeContainerItem> containers, int limit) throws PackagerInterruptedException {
		return pack(boxes, itemGroupOrder, containers, limit, PackagerInterruptSupplierBuilder.NEGATIVE);
	}

	public List<Container> pack(List<BoxItem> boxes, List<CompositeContainerItem> containerItems, int limit, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		PackagerAdapter<P> adapter = adapter(boxes, containerItems, interrupt);

		if(adapter == null) {
			return Collections.emptyList();
		}
		
		return packAdapter(limit, interrupt, adapter);
	}

	/**
	 * Return a list of containers which holds all the boxes in the argument
	 *
	 * @param boxes     list of boxes to fit in a container
	 * @param containerItems list of containers available for use in this operation
	 * @param limit     maximum number of containers
	 * @param interrupt When true, the computation is interrupted as soon as possible.
	 * @return list of containers, or null if the deadline was reached, or empty list if the packages could not be packaged within the available containers and/or limit.
	 * @throws PackagerInterruptedException 
	 */

	public List<Container> pack(List<BoxItemGroup> boxes, Order itemGroupOrder, List<CompositeContainerItem> containerItems, int limit, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		PackagerAdapter<P> adapter = adapter(boxes, containerItems, itemGroupOrder, interrupt);

		if(adapter == null) {
			return Collections.emptyList();
		}
		
		return packAdapter(limit, interrupt, adapter);
	}

	private List<Container> packAdapter(int limit, PackagerInterruptSupplier interrupt, PackagerAdapter<P> adapter) throws PackagerInterruptedException {
		List<Container> containerPackResults = new ArrayList<>();

		do {
			// is it possible to fit the remaining boxes a single container?
			int maxContainers = limit - containerPackResults.size();
			List<Integer> containerItemIndexes = adapter.getContainers(1);
			if(!containerItemIndexes.isEmpty()) {

				P result = packSingle(containerItemIndexes, adapter, interrupt);
				if(result == null) {
					// timeout
					return null;
				}
				if(!result.isEmpty()) {
					containerPackResults.add(adapter.accept(result));

					// positive result
					return containerPackResults;
				}
				
				// TODO any way to reuse partial results as the current best result?
			}

			// one or more containers
			containerItemIndexes = adapter.getContainers(maxContainers);
			if(containerItemIndexes.isEmpty()) {
				return Collections.emptyList();
			}

			// the best container is the one which can hold the most box groups
			// assume larger boxes is at the end of list, so start there
			P best = null;
			for (int i = containerItemIndexes.size() - 1; i >= 0; i--) {
				try {

					if(interrupt.getAsBoolean()) {
						throw new PackagerInterruptedException();
					}

					Integer containerItemIndex = containerItemIndexes.get(i);

					// can this container hold more than the previously best result?
					if(best != null) {
						ContainerItem containerItem = adapter.getContainerItem(containerItemIndex);
						Container container = containerItem.getContainer();
	
						long loadVolume = container.getLoadVolume();
						if(loadVolume > container.getMaxLoadVolume()) {
							continue;
						}
						int loadWeight = container.getLoadWeight();
						if(loadWeight > container.getMaxLoadWeight()) {
							continue;
						}
					}

					P result = adapter.attempt(containerItemIndex, best);
	
					if(!result.isEmpty()) {
						if(best == null || intermediatePackagerResultComparator.compare(best, result) != PackResultComparator.ARGUMENT_1_IS_BETTER) {
							best = result;
						}
					}
				} catch(PackagerInterruptedException e) {
					// timeout, unless already have a result ready
					if(best != null && best.getStack().size() == adapter.countRemainingBoxes()) {
						containerPackResults.add(adapter.accept(best));

						return containerPackResults;
					}
					return null;
				}
			}

			if(best == null) {
				// negative result
				return Collections.emptyList();
			}

			containerPackResults.add(adapter.accept(best));
			
			if(adapter.countRemainingBoxes() == 0) {
				return containerPackResults;
			}
			
		} while (containerPackResults.size() < limit);

		return null;
	}

	protected abstract PackagerAdapter<P> adapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Order itemGroupOrder, PackagerInterruptSupplier interrupt);

	protected abstract PackagerAdapter<P> adapter(List<BoxItem> boxItems, List<CompositeContainerItem> containers, PackagerInterruptSupplier interrupt);

	public void close() {
		scheduledThreadPoolExecutor.shutdownNow();
	}
	
	protected List<BoxItemGroup> getFitsInside(List<BoxItemGroup> inputs, Container container) {
		List<BoxItemGroup> result = new ArrayList<>(inputs.size());
		for (BoxItemGroup boxItemGroup : inputs) {
			if(container.fitsInside(boxItemGroup)) {
				result.add(boxItemGroup);
			}
		}
		return result;
	}
	
	protected List<BoxItem> getBoxItemsFitsInside(List<BoxItem> inputs, Container container) {
		List<BoxItem> result = new ArrayList<>(inputs.size());
		for (BoxItem boxItem : inputs) {
			if(container.fitsInside(boxItem)) {
				result.add(boxItem);
			}
		}
		return result;
	}
	
	public List<BoxItem> removeEmpty(List<BoxItem> values) {
		List<BoxItem> result = new ArrayList<>(values.size());
		for(int i = 0; i < values.size(); i++) {
			if(!values.get(i).isEmpty()) {
				result.add(values.get(i));
			}
		}
		return result;
	}
	
	public ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}
}
