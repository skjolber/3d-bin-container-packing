package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BinarySearchIterator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/** Packs containers in their preference order. */
public class OrderedContainerPackingStrategy implements ContainerStrategy {
	@FunctionalInterface
	public interface SingleContainerPacker {
		IntermediatePackagerResult packSingle(List<Integer> indexes, PackagerAdapter adapter,
				PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;
	}

	private final Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	private final Supplier<IntermediatePackagerResult> emptyResultSupplier;
	private final SingleContainerPacker singleContainerPacker;

	public OrderedContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator,
			Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		this(comparator, emptyResultSupplier, null);
	}

	public OrderedContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator,
			Supplier<IntermediatePackagerResult> emptyResultSupplier, SingleContainerPacker singleContainerPacker) {
		this.intermediatePackagerResultComparator = comparator;
		this.emptyResultSupplier = emptyResultSupplier;
		this.singleContainerPacker = singleContainerPacker == null ? this::packSingle : singleContainerPacker;
	}

	// pack in single container
	public IntermediatePackagerResult packSingle(List<Integer> containerItemIndexes, PackagerAdapter adapter, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(containerItemIndexes.size() <= 2) {
			for (int i = 0; i < containerItemIndexes.size(); i++) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}

				Integer containerItemIndex = containerItemIndexes.get(i);
				
				IntermediatePackagerResult result = adapter.attempt(containerItemIndex, null, true);
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

				IntermediatePackagerResult bestResult = null;
				int bestIndex = Integer.MAX_VALUE;

			do {
					int mid = iterator.next();
					int nextContainerItemIndex = containerItemIndexes.get(mid);
					
					IntermediatePackagerResult result = null;
					
					// see whether the current container holds the boxes of the same result as before
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
						results[nextContainerItemIndex] = emptyResultSupplier.get();
								
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
					return result;
				}
			}
		}
		return emptyResultSupplier.get();
	}

	@Override
	public ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException {
		int limit = adapter.getMaxContainerCount();

		List<Container> containerPackResults = new ArrayList<>();

		do {
			// is it possible to fit the remaining boxes a single container?
			int maxContainers = limit - containerPackResults.size();
			if(maxContainers > 1) {
				List<BoxItemGroup> groups = adapter.getRemainingBoxItemGroups();
				List<Integer> containerItemIndexes;
				if(groups != null) {
					containerItemIndexes = adapter.getContainerItemsCalculator().getGroupContainers(groups, 1);
				} else {
					containerItemIndexes = adapter.getContainerItemsCalculator().getContainers(adapter.getRemainingBoxItems(), 1);
				}
				if(!containerItemIndexes.isEmpty()) {
	
					IntermediatePackagerResult result = singleContainerPacker.packSingle(containerItemIndexes, adapter, interrupt);
					if(!result.isEmpty()) {
						containerPackResults.add(adapter.accept(result));
	
						// positive result
						return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
					}
					
					// TODO any way to reuse partial results as the current best result?
				}
			}

			// one or more containers
			List<Integer> containerItemIndexes = adapter.getContainers();
			if(containerItemIndexes.isEmpty()) {
				return null;
			}

			// the best container is the one which can hold the most box groups
			// assume larger boxes is at the end of list, so start there
			IntermediatePackagerResult best = null;
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
						long loadWeight = container.getLoadWeight();
						if(loadWeight > container.getMaxLoadWeight()) {
							continue;
						}
					}

					IntermediatePackagerResult result;
					if(best != null && best.getStack().size() == adapter.countRemainingBoxes() ) {
						result = adapter.peek(nextContainerItemIndex, best);
						
						if(result == null) {
							result = adapter.attempt(nextContainerItemIndex, best, maxContainers == 1);
						}
					} else {
						result = adapter.attempt(nextContainerItemIndex, best, maxContainers == 1);
					}
					
					if(result != null && !result.isEmpty()) {
						if(best == null || intermediatePackagerResultComparator.compare(best, result) <= 0) { // we are going backwards so if equal, the candidate is the new best result
							best = result;
						}
					}
				} catch(PackagerInterruptedException e) {
					// timeout, unless already have a result ready
					if(best != null && best.getStack().size() == adapter.countRemainingBoxes()) {
						containerPackResults.add(adapter.accept(best));

						return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
					}
					throw e;
				}
			}

			if(best == null) {
				// negative result
				return null;
			}

			containerPackResults.add(adapter.accept(best));
			
			if(adapter.countRemainingBoxes() == 0) {
				return new ContainerResult(adapter.getContainerItemsCalculator().getCost(), containerPackResults);
			}
			
		} while (containerPackResults.size() < limit);

		return null;
	}

}
