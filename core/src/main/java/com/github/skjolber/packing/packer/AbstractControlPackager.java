package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.control.manifest.DefaultManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.PackagerBoxItems;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation.
 */
public abstract class AbstractControlPackager<I extends Placement, B extends PackagerResultBuilder> extends AbstractPackager<B> {

	public AbstractControlPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	public IntermediatePackagerResult pack(List<RemainingBoxItem> boxItems, ContainerItem controlContainerItem, PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean loadIdenticalBox) throws PackagerInterruptedException {
		Container container = controlContainerItem.getContainer();

		Stack stack = createStack();

		// container priorities: the items of one priority at a time, see getContainerPriorityEnd(..)
		boolean containerPriorities = hasContainerPriorities(boxItems);
		int maxContainerPriority = Integer.MAX_VALUE;
		DefaultBoxItemSource boxItemSource = new DefaultBoxItemSource(sortByRanks(boxItems, order));
		ExtractionOrderSearch extractionOrderSearch = createExtractionOrderSearch(boxItems, order);

		PointCalculator pointCalculator = createPointCalculator(boxItemSource); 
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlContainerItem.hasInitialPoints()) {
			pointCalculator.setPoints(controlContainerItem.getInitialPoints());
			pointCalculator.clear();
		}

		ManifestControls manifestControls = createManifestControls(container, stack, boxItemSource, pointCalculator, null, controlContainerItem.getManifestControlsBuilderFactory());

		PointControlsBuilderFactory pointControlsBuilderFactory = controlContainerItem.getPointControlsBuilderFactory();
		if(pointControlsBuilderFactory == null) {
			pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();
		}

		PointControls pointControls = createPointControls(container, stack, boxItemSource, pointCalculator, pointControlsBuilderFactory, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, loadIdenticalBox);
		
		// remove boxes which do not fit due to volume, weight or dimensions
		List<RemainingBoxItem> removed = new ArrayList<>(boxItemSource.size());
		for(int i = 0; i < boxItemSource.size(); i++) {
			RemainingBoxItem boxItem = boxItemSource.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return createEmptyIntermediatePackagerResult();
				}
				
				if(order != Order.CHRONOLOGICAL) {
					removed.add(boxItemSource.remove(i));
					i--;
				} else {
					// remove all later then the first removed
					while(i < boxItemSource.size()) {
						removed.add(boxItemSource.remove(i));
					}
				}
			}
		}
		
		if(!removed.isEmpty()) {
			manifestControls.declined(removed);
			pointControls.declined(removed);
			maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
			
			removed.clear();
		}
		
		long maxBoxArea = boxItemSource.getMaxArea();
		long maxBoxVolume = boxItemSource.getMaxVolume();
		
		pointCalculator.setMinimumAreaAndVolumeLimit(boxItemSource.getMinArea(), boxItemSource.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		PlacementControls placementControls = createControls(boxItemSource, order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, loadIdenticalBox);

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !boxItemSource.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}

			int end = containerPriorities ? getContainerPriorityEnd(boxItemSource, maxContainerPriority) : boxItemSource.size();
			if(end == 0) {
				break;
			}
			Placement placement = extractionOrderSearch != null ? extractionOrderSearch.getPlacement(placementControls, boxItemSource, end) : placementControls.getPlacement(0, end);
			if(placement == null) {
				break;
			}

			stack.add(placement);
			pointCalculator.add(placement.getPointIndex(), placement);
			placementControls.accepted(placement);
			
			remainingLoadWeight -= placement.getRemainingBoxItem().getBox().getWeight();
			remainingLoadVolume -= placement.getRemainingBoxItem().getBox().getVolume();
			
			if(order == Order.CHRONOLOGICAL_ALLOW_SKIPPING && removeSkippedBoxItems(boxItemSource, placement.getRemainingBoxItem(), removed)) {
				manifestControls.declined(removed);
				pointControls.declined(removed);
				maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);

				removed.clear();
			}
			boxItemSource.decrement(placement.getRemainingBoxItem().getLocalIndex(), 1);

			manifestControls.accepted(placement.getRemainingBoxItem());
			pointControls.accepted(placement.getRemainingBoxItem());
			
			if(!boxItemSource.isEmpty()) {
				
				// remove items are too big according to total volume / weight
				for(int i = 0; i < boxItemSource.size(); i++) {
					RemainingBoxItem boxItem = boxItemSource.get(i);
					Box box = boxItem.getBox();
					if(box.getVolume() > remainingLoadVolume || box.getWeight() > remainingLoadWeight) {
						
						if(abortOnAnyBoxTooBig) {
							return createEmptyIntermediatePackagerResult();
						}
						
						if(order != Order.CHRONOLOGICAL) {
							removed.add(boxItemSource.remove(i));
							i--;
						} else {
							// remove all later then the first removed
							while(i < boxItemSource.size()) {
								removed.add(boxItemSource.remove(i));
							}							
						}
					}
				}
				if(!removed.isEmpty()) {
					manifestControls.declined(removed);
					pointControls.declined(removed);
					maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
					
					removed.clear();
				}
				
				// remove small points
				pointCalculator.setMinimumAreaAndVolumeLimit(boxItemSource.getMinArea(), boxItemSource.getMinVolume());				
				
				// remove boxes which are too big for the available points
				long maxPointArea = pointCalculator.getMaxArea();
				long maxPointVolume = pointCalculator.getMaxVolume();
				
				if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
					for(int i = 0; i < boxItemSource.size(); i++) {
						RemainingBoxItem boxItem = boxItemSource.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
							
							if(abortOnAnyBoxTooBig) {
								return createEmptyIntermediatePackagerResult();
							}
							
							if(order != Order.CHRONOLOGICAL) {
								removed.add(boxItemSource.remove(i));
								i--;
							} else {
								// remove all later then the first removed
								while(i < boxItemSource.size()) {
									removed.add(boxItemSource.remove(i));
								}					
							}
						}
					}
					
					if(!removed.isEmpty()) {
						manifestControls.declined(removed);
						pointControls.declined(removed);
						maxContainerPriority = getMaxContainerPriority(maxContainerPriority, removed);
						
						removed.clear();
					}
				}
			}
		}
		
		// ignore decline for the rest
		
		return createIntermediatePackagerResult(controlContainerItem, stack);
	}

	/**
	 * Without a box item order, sort the box items by ascending container priority (so that the items of the lowest
	 * priority are first, see {@link #getContainerPriorityEnd(BoxItemSource, int)}), then by descending extraction order
	 * (see {@link ExtractionOrderSearch}), keeping their order otherwise. With a box item order, the container priorities
	 * do not decrease (see {@link AbstractPackager#getUnsupportedReason(PackagerInput)}).
	 */
	protected static List<RemainingBoxItem> sortByRanks(List<RemainingBoxItem> boxItems, Order order) {
		if(order != null && order != Order.NONE || !hasContainerPriorities(boxItems) && !hasExtractionOrders(boxItems)) {
			return boxItems;
		}
		List<RemainingBoxItem> sorted = new ArrayList<>(boxItems);
		// stable insertion sort; few items, often sorted
		for (int i = 1; i < sorted.size(); i++) {
			RemainingBoxItem boxItem = sorted.get(i);
			int j = i - 1;
			while(j >= 0 && isRankedAfter(sorted.get(j), boxItem)) {
				sorted.set(j + 1, sorted.get(j));
				j--;
			}
			sorted.set(j + 1, boxItem);
		}
		return sorted;
	}

	private static boolean isRankedAfter(RemainingBoxItem first, RemainingBoxItem second) {
		if(first.getContainerPriority() != second.getContainerPriority()) {
			return first.getContainerPriority() > second.getContainerPriority();
		}
		return first.getExtractionOrder() < second.getExtractionOrder();
	}

	/**
	 * @return an extraction order search, if the box items have different extraction orders and no given order, otherwise null
	 */
	protected static ExtractionOrderSearch createExtractionOrderSearch(List<RemainingBoxItem> boxItems, Order order) {
		if(order != null && order != Order.NONE || !hasExtractionOrders(boxItems)) {
			return null;
		}
		return new ExtractionOrderSearch();
	}

	protected static boolean hasExtractionOrders(List<RemainingBoxItem> boxItems) {
		for (int i = 1; i < boxItems.size(); i++) {
			if(boxItems.get(i).getExtractionOrder() != boxItems.get(0).getExtractionOrder()) {
				return true;
			}
		}
		return false;
	}

	protected static boolean hasContainerPriorities(List<RemainingBoxItem> boxItems) {
		for (int i = 1; i < boxItems.size(); i++) {
			if(boxItems.get(i).getContainerPriority() != boxItems.get(0).getContainerPriority()) {
				return true;
			}
		}
		return false;
	}

	protected static boolean hasGroupContainerPriorities(List<RemainingBoxItemGroup> groups) {
		for (int i = 1; i < groups.size(); i++) {
			if(groups.get(i).getContainerPriority() != groups.get(0).getContainerPriority()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Boxes are placed in a container one container priority at a time (see {@link com.github.skjolber.packing.api.BoxItem#withContainerPriority(int)}):
	 * the items of the first priority, as long as it does not exceed the given maximum.
	 *
	 * @param boxItems box items, sorted by container priority
	 * @param maxContainerPriority the highest priority which may still be placed in the container (lowered when items
	 *        are declined, as the next priority must then wait for the next container)
	 * @return the end index of the items which may be placed, 0 if none
	 */
	/**
	 * With {@link Order#CHRONOLOGICAL_ALLOW_SKIPPING}, the boxes before a placed box in the order were skipped: they are
	 * not placed in this container (later boxes could otherwise make room for them), but wait for the next container,
	 * so that the boxes in a container are in the order.
	 *
	 * @param placed the box item just placed; its local index is its position in the box items
	 * @param removed the skipped box items are added here
	 * @return true if any box items were skipped
	 */
	protected static boolean removeSkippedBoxItems(BoxItemSource boxItemSource, RemainingBoxItem placed, List<RemainingBoxItem> removed) {
		int skipped = placed.getLocalIndex();
		for(int i = 0; i < skipped; i++) {
			removed.add(boxItemSource.remove(0));
		}
		return skipped > 0;
	}

	protected static int getContainerPriorityEnd(BoxItemSource boxItems, int maxContainerPriority) {
		if(boxItems.isEmpty()) {
			return 0;
		}
		int priority = boxItems.get(0).getContainerPriority();
		if(priority > maxContainerPriority) {
			return 0;
		}
		int end = 1;
		while(end < boxItems.size() && boxItems.get(end).getContainerPriority() == priority) {
			end++;
		}
		return end;
	}

	/**
	 * @return the highest container priority which may still be placed in the container after declining the items
	 */
	protected static int getMaxContainerPriority(int maxContainerPriority, List<RemainingBoxItem> declined) {
		for (int i = 0; i < declined.size(); i++) {
			maxContainerPriority = Math.min(maxContainerPriority, declined.get(i).getContainerPriority());
		}
		return maxContainerPriority;
	}

	/**
	 * @return the highest container priority which may still be placed in the container after declining the groups
	 */
	protected static int getMaxGroupContainerPriority(int maxContainerPriority, List<RemainingBoxItemGroup> declined) {
		for (int i = 0; i < declined.size(); i++) {
			maxContainerPriority = Math.min(maxContainerPriority, declined.get(i).getContainerPriority());
		}
		return maxContainerPriority;
	}

	/**
	 * @return the lowest container priority of the groups
	 */
	protected static int getMinGroupContainerPriority(BoxItemGroupSource groups) {
		int min = Integer.MAX_VALUE;
		for (int i = 0; i < groups.size(); i++) {
			min = Math.min(min, groups.get(i).getContainerPriority());
		}
		return min;
	}

	protected Stack createStack() {
		return new Stack();
	}

	protected ManifestControls createManifestControls(Container container, Stack stack, BoxItemSource boxItemSource,
			PointCalculator pointCalculator, BoxItemGroupSource groups, ManifestControlsBuilderFactory manifestControlsBuilderFactory) {
				
		if(manifestControlsBuilderFactory == null) {
			return new DefaultManifestControls(boxItemSource);
		}
		return manifestControlsBuilderFactory.createManifestControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(boxItemSource)
				.withBoxItemGroups(groups)
				.withPoints(pointCalculator)
				.build();
	}
	
	protected PointControls createPointControls(Container container, Stack stack, BoxItemSource boxItemSource, PointSource points, PointControlsBuilderFactory pointControlsBuilderFactory, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean loadIdenticalBox) {
		return pointControlsBuilderFactory.createPointControlsBuilder()
				.withContainer(container)
				.withStack(stack)
				.withBoxItems(boxItemSource)
				.withPoints(points)
				.withMaxLoad(maxLoadWeight, maxLoadPressure, maxLoadBoxCount)
				.withLoadIdenticalBox(loadIdenticalBox)
				.build();
	}
	
	protected PointCalculator createPointCalculator(BoxItemSource boxItemSource) {
		return new DefaultPointCalculator3D(false, boxItemSource);
	}

	public IntermediatePackagerResult packGroup(List<RemainingBoxItemGroup> boxItemGroups, Order order, ContainerItem controlContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig, boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount) throws PackagerInterruptedException {
		ContainerItem containerItem = controlContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = createStack();
		
		// container priorities: the groups of one priority at a time
		boolean containerPriorities = hasGroupContainerPriorities(boxItemGroups);
		int maxContainerPriority = Integer.MAX_VALUE;
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);
		BoxItemSource filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		MarkResetPointCalculator3D pointCalculator = new MarkResetPointCalculator3D(true, filteredBoxItems);
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		if(controlContainerItem.hasInitialPoints()) {
			pointCalculator.setPoints(controlContainerItem.getInitialPoints());
			pointCalculator.clear();
		}


		BoxItemGroupSource filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();

		ManifestControls manifestControls = createManifestControls(container, stack, filteredBoxItems, pointCalculator, filteredBoxItemGroups, controlContainerItem.getManifestControlsBuilderFactory());

		PointControlsBuilderFactory pointControlsBuilderFactory = controlContainerItem.getPointControlsBuilderFactory();
		if(pointControlsBuilderFactory == null) {
			pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();
		}
		
		PointControls pointControls = createPointControls(container, stack, filteredBoxItems, pointCalculator, pointControlsBuilderFactory, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);
						
		List<RemainingBoxItemGroup> removedBoxItemGroups = new ArrayList<>();

		if(order != Order.CHRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				RemainingBoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup.getBoxItemGroup())) {
					if(abortOnAnyBoxTooBig) {
						return createEmptyIntermediatePackagerResult();
					}
					if(order != Order.CHRONOLOGICAL) {
						filteredBoxItemGroups.remove(i);
						i--;
						
						removedBoxItemGroups.add(boxItemGroup);
					} else {
						// remove all later groups than the first removed
						while(i < filteredBoxItemGroups.size()) {
							removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
						}
					}		
				}
			}
			
			if(!removedBoxItemGroups.isEmpty()) {
				if(manifestControls != null) {
					manifestControls.filteredGroups(removedBoxItemGroups);
				}
				if(pointControls != null) {
					pointControls.filteredGroups(removedBoxItemGroups);
				}
				maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
				removedBoxItemGroups.clear();
			}
		}
		
		pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, order, container, pointCalculator);

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		long maxBoxVolume = filteredBoxItems.getMaxVolume();
		long maxBoxArea = filteredBoxItems.getMaxVolume();
		
		PlacementControls placementControls = createControls(filteredBoxItems, order, pointControls, container, pointCalculator, stack, maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalBoxCount);

		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !pointCalculator.isEmpty() && boxItemGroupIterator.hasNext() && !filteredBoxItemGroups.isEmpty()) {
			int groupIndex = boxItemGroupIterator.next();
			
			int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndexForGroup(groupIndex);
			
			RemainingBoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			if(containerPriorities && boxItemGroup.getContainerPriority() > Math.min(maxContainerPriority, getMinGroupContainerPriority(filteredBoxItemGroups))) {
				// a group of a lower container priority is not placed in this container
				break groups;
			}
			boxItemGroup.mark();
			
			pointCalculator.mark();
			int markStackSize = stack.size();
			
			manifestControls.attempt(boxItemGroup, boxItemStartIndex, boxItemGroup.size());
			
			while(!boxItemGroup.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					throw new PackagerInterruptedException();
				}
				
				// groups before this one may have been removed
				boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndex(boxItemGroup);
				if(boxItemStartIndex == -1) {
					break;
				}
				Placement placement = placementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());				
				if(placement == null) {
					break;
				}

				stack.add(placement);
				pointCalculator.add(placement.getPointIndex(), placement);
				
				remainingLoadWeight -= placement.getRemainingBoxItem().getBox().getWeight();
				remainingLoadVolume -= placement.getRemainingBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(placement.getRemainingBoxItem().getLocalIndex());

				manifestControls.accepted(placement.getRemainingBoxItem());
				pointControls.accepted(placement.getRemainingBoxItem());
				placementControls.accepted(placement);

				if(!filteredBoxItemGroups.isEmpty()) {
					// remove groups are too big according to total volume / weight
					
					for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
						RemainingBoxItemGroup g = filteredBoxItemGroups.get(i);
						if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
							
							if(abortOnAnyBoxTooBig) {
								return createEmptyIntermediatePackagerResult();
							}
							
							if(order != Order.CHRONOLOGICAL) {
								filteredBoxItemGroups.remove(i);
								i--;
								
								removedBoxItemGroups.add(g);
							} else {
								// remove all later groups than the first removed
								while(i < filteredBoxItemGroups.size()) {
									removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
								}
								
								
							}
						}
					}

					if(!removedBoxItemGroups.isEmpty()) {
						manifestControls.filteredGroups(removedBoxItemGroups);
						pointControls.filteredGroups(removedBoxItemGroups);
						maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
						removedBoxItemGroups.clear();
					}
					
					// remove / constrain to small points
					pointCalculator.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
					
					// remove groups which have boxes which are too big for the current points
					long maxPointArea = pointCalculator.getMaxArea();
					long maxPointVolume = pointCalculator.getMaxVolume();
										
					if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
	
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							RemainingBoxItemGroup g = filteredBoxItemGroups.get(i);
	
							for(int k = 0; k < g.size(); k++) {
								RemainingBoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
	
									if(abortOnAnyBoxTooBig) {
										return createEmptyIntermediatePackagerResult();
									}

									if(order != Order.CHRONOLOGICAL) {
										filteredBoxItemGroups.remove(i);
										i--;
										
										removedBoxItemGroups.add(g);
									} else {
										// remove all later groups than the first removed
										while(i < filteredBoxItemGroups.size()) {
											removedBoxItemGroups.add(filteredBoxItemGroups.remove(i));
										}
									}

									break;
								}
							}				
						}
						if(!removedBoxItemGroups.isEmpty()) {
							manifestControls.filteredGroups(removedBoxItemGroups);
							pointControls.filteredGroups(removedBoxItemGroups);
							maxContainerPriority = getMaxGroupContainerPriority(maxContainerPriority, removedBoxItemGroups);
							removedBoxItemGroups.clear();
						}
					}
					
				}
				
				if(!packagerBoxItems.contains(boxItemGroup)) {
					// the current group was removed, assume packaging unsuccessful.
					break;
				}
			}

			boolean removed = !packagerBoxItems.contains(boxItemGroup);
			if(!removed) {
				packagerBoxItems.remove(boxItemGroup);
			}
			
			if(removed || !boxItemGroup.isEmpty()) {				
				boxItemGroup.reset();
				
				if(abortOnAnyBoxTooBig) {
					return createEmptyIntermediatePackagerResult();
				}

				// undo any work on this group
				List<Placement> removedBoxPlacements = stack.getPlacements().subList(markStackSize, stack.size());
				if(!removedBoxPlacements.isEmpty()) {
					List<RemainingBoxItem> removedBoxItems = new ArrayList<>();
					for(Placement p : removedBoxPlacements) {
						removedBoxItems.add(p.getRemainingBoxItem());
					}
					manifestControls.undo(removedBoxItems);
					pointControls.undo(removedBoxItems);
					
					removedBoxItems.clear();
					
					placementControls.undo(removedBoxPlacements);
				}
				
				manifestControls.attemptFailure(boxItemGroup);
				pointControls.attemptFailure(boxItemGroup);
				
				stack.setSize(markStackSize);
				// the group waits for the next container, and so do the groups of higher container priorities
				maxContainerPriority = Math.min(maxContainerPriority, boxItemGroup.getContainerPriority());
				
				// unable to stack whole group
				if(order == Order.CHRONOLOGICAL) {
					break groups;
				}
				// try again with another group if possible
				pointCalculator.reset();

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < pointCalculator.calculateUsedWeight()) {
				throw new RuntimeException();
			}
			
			// TODO
			// remove points which are invalid for use with a new group
			// i.e. inner points which cannot be accessed without moving
			// something else.
			
			// successfully stacked group
			boxItemGroup.reset();
			manifestControls.attemptSuccess(boxItemGroup);
			pointControls.attemptSuccess(boxItemGroup);
		}
		
		return createIntermediatePackagerResult(controlContainerItem, stack);
	}

	protected abstract IntermediatePackagerResult createIntermediatePackagerResult(ContainerItem containerItem, Stack stack);

	protected abstract IntermediatePackagerResult createEmptyIntermediatePackagerResult();

	protected abstract BoxItemGroupIterator createBoxItemGroupIterator(
			BoxItemGroupSource groups, 
			Order itemGroupOrder, 
			Container container,
			PointCalculator pointCalculator
		);

	protected abstract PlacementControls createControls(
			BoxItemSource boxItems,
			Order order, PointControls pointControls,
			Container container,
			PointCalculator pointCalculator,
			Stack stack,
			boolean maxLoadWeight, boolean maxLoadPressure, boolean maxLoadBoxCount, boolean maxLoadIdenticalBoxCount
		);
}
