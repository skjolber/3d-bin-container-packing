package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BinarySearchIterator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 *
 * Thread-safe implementation.
 */

public abstract class AbstractPackager<P extends IntermediatePackagerResult, B extends PackagerResultBuilder> implements Packager<B> {

	public static final int ARGUMENT_1_IS_BETTER = 1;
	public static final int ARGUMENT_2_IS_BETTER = -1;

	protected final Comparator<P> intermediatePackagerResultComparator;
	
	protected final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(Integer.MAX_VALUE);

	public AbstractPackager(Comparator<P> comparator) {
		this.intermediatePackagerResultComparator = comparator;
	}

	// pack in single container
	public P packSingle(List<Integer> containerItemIndexes, PackagerAdapter<P> adapter, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(containerItemIndexes.size() <= 2) {
			for (int i = 0; i < containerItemIndexes.size(); i++) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}

				Integer containerItemIndex = containerItemIndexes.get(i);
				
				P result = adapter.attempt(containerItemIndex, null, true);
				if(result.isEmpty()) {
					continue;
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
					
					P result = null;
					
					// see whether the current container holds the boxes of the same result at before
					if(bestResult != null) {
						result = adapter.peek(nextContainerItemIndex, bestResult);
					}

					if(result == null) {
						result = adapter.attempt(nextContainerItemIndex, bestResult, true);
					}
					if(!result.isEmpty() && result.getStack().size() == adapter.countRemainingBoxes()) {
						results[nextContainerItemIndex] = result;

						iterator.lower();

						if(mid < bestIndex) {
							bestIndex = mid;
							bestResult = result;
						}
					} else {
						// count as empty
						results[nextContainerItemIndex] = createEmptyIntermediatePackagerResult();
								
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
		return createEmptyIntermediatePackagerResult();
	}

	public List<Container> packAdapter(int limit, PackagerInterruptSupplier interrupt, PackagerAdapter<P> adapter) throws PackagerInterruptedException {
		List<Container> containerPackResults = new ArrayList<>();

		do {
			// is it possible to fit the remaining boxes a single container?
			int maxContainers = limit - containerPackResults.size();
			if(maxContainers > 1) {
				List<Integer> containerItemIndexes = adapter.getContainers(1);
				if(!containerItemIndexes.isEmpty()) {
	
					P result = packSingle(containerItemIndexes, adapter, interrupt);
					if(!result.isEmpty()) {
						containerPackResults.add(adapter.accept(result));
	
						// positive result
						return containerPackResults;
					}
					
					// TODO any way to reuse partial results as the current best result?
				}
			}

			// one or more containers
			List<Integer> containerItemIndexes = adapter.getContainers(maxContainers);
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

					Integer nextContainerItemIndex = containerItemIndexes.get(i);

					// can this container hold more than the previously best result?
					if(best != null) {
						ContainerItem containerItem = adapter.getContainerItem(nextContainerItemIndex);
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

					P result;
					if(best != null && best.getStack().size() == adapter.countRemainingBoxes() ) {
						result = adapter.peek(nextContainerItemIndex, best);
						
						if(result == null) {
							result = adapter.attempt(nextContainerItemIndex, best, maxContainers == 1);
						}
					} else {
						result = adapter.attempt(nextContainerItemIndex, best, maxContainers == 1);
					}
					
					if(result != null && !result.isEmpty()) {
						if(best == null || intermediatePackagerResultComparator.compare(best, result) != ARGUMENT_1_IS_BETTER) {
							best = result;
						}
					}
				} catch(PackagerInterruptedException e) {
					// timeout, unless already have a result ready
					if(best != null && best.getStack().size() == adapter.countRemainingBoxes()) {
						containerPackResults.add(adapter.accept(best));

						return containerPackResults;
					}
					throw e;
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
	
	protected abstract P createEmptyIntermediatePackagerResult();

}
