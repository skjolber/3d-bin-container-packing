package com.github.skjolber.packing.packer.strategy.ordered;

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
import com.github.skjolber.packing.packer.strategy.ContainerResult;
import com.github.skjolber.packing.packer.strategy.ContainerStrategy;
import com.github.skjolber.packing.packer.strategy.allocation.ContainerAllocationPlanner;

/** Packs containers in their preference order. */
public class OrderedContainerPackingStrategy implements ContainerStrategy {
	@FunctionalInterface
	public interface SingleContainerPacker {
		IntermediatePackagerResult packSingle(List<ContainerItem> containerItems, PackagerAdapter adapter, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;
	}

	private final Comparator<IntermediatePackagerResult> intermediatePackagerResultComparator;
	private final Supplier<IntermediatePackagerResult> emptyResultSupplier;
	private final SingleContainerPacker singleContainerPacker;
	private final boolean allocationFeasibilityCheck;

	public OrderedContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator,
			Supplier<IntermediatePackagerResult> emptyResultSupplier) {
		this(comparator, emptyResultSupplier, null, true);
	}

	public OrderedContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier, SingleContainerPacker singleContainerPacker) {
		this(comparator, emptyResultSupplier, singleContainerPacker, true);
	}

	private OrderedContainerPackingStrategy(Comparator<IntermediatePackagerResult> comparator, Supplier<IntermediatePackagerResult> emptyResultSupplier, SingleContainerPacker singleContainerPacker, boolean allocationFeasibilityCheck) {
		this.intermediatePackagerResultComparator = comparator;
		this.emptyResultSupplier = emptyResultSupplier;
		this.singleContainerPacker = singleContainerPacker == null ? this::packSingle : singleContainerPacker;
		this.allocationFeasibilityCheck = allocationFeasibilityCheck;
	}

	/**
	 * Create a variant for callers which have already proven that remaining
	 * inventory can allocate every remaining unit.
	 */
	public OrderedContainerPackingStrategy withoutAllocationFeasibilityCheck() {
		return new OrderedContainerPackingStrategy(intermediatePackagerResultComparator, emptyResultSupplier, singleContainerPacker, false);
	}

	// pack in single container
	public IntermediatePackagerResult packSingle(List<ContainerItem> containerItems, PackagerAdapter adapter, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(containerItems.size() <= 2) {
			for (int i = 0; i < containerItems.size(); i++) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}

				int containerItemIndex = containerItems.get(i).getIndex();
				
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
			
			IntermediatePackagerResult[] results = new IntermediatePackagerResult[containerItems.get(containerItems.size() - 1).getIndex() + 1];

			BinarySearchIterator iterator = new BinarySearchIterator();

			search: do {
				iterator.reset(containerItems.size() - 1, 0);

				IntermediatePackagerResult bestResult = null;
				int bestIndex = Integer.MAX_VALUE;

				do {
					int mid = iterator.next();
					int nextContainerItemIndex = containerItems.get(mid).getIndex();
					
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
				
				for (int i = 0; i < containerItems.size(); i++) {
					int nextContainerItemIndex = containerItems.get(i).getIndex();
					if(results[nextContainerItemIndex] != null) {
						if(!results[nextContainerItemIndex].isEmpty()) {
							// remove containers at lower indexes; we already have a better match
							while (containerItems.size() > i) {
								containerItems.remove(containerItems.size() - 1);
							}
							break;
						}
						
						// remove container which could not fit all the items
						containerItems.remove(i);
						i--;
					}
				}
				// halt when not more containers to check
			} while (!containerItems.isEmpty());

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
			// Avoid trying candidate containers when the remaining items cannot be
			// assigned to the remaining inventory within the container limit.
			if(allocationFeasibilityCheck && !ContainerAllocationPlanner.canAllocate(adapter, interrupt)) {
				return null;
			}

			// is it possible to fit the remaining boxes a single container?
			int maxContainers = limit - containerPackResults.size();
			if(maxContainers > 1) {
				List<BoxItemGroup> groups = adapter.getRemainingBoxItemGroups();
				List<ContainerItem> containerItems;
				if(groups != null) {
					containerItems = adapter.getContainerItemsCalculator().getGroupContainers(groups, 1);
				} else {
					containerItems = adapter.getContainerItemsCalculator().getContainers(adapter.getRemainingBoxItems(), 1);
				}
				if(!containerItems.isEmpty()) {
	
					IntermediatePackagerResult result = singleContainerPacker.packSingle(containerItems, adapter, interrupt);
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
