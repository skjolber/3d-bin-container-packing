package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.AbstractControlsPackagerResultBuilder;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Priority;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemGroupControls;
import com.github.skjolber.packing.api.packager.ControlContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.PackagerBoxItems;
import com.github.skjolber.packing.api.packager.PointControls;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation.
 */
public abstract class AbstractControlPackager extends AbstractPackager<IntermediatePackagerResult, AbstractControlPackager.DefaultControlsPackagerResultBuilder> {

	public AbstractControlPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	protected class DefaultBoxItemAdapter extends AbstractBoxItemAdapter<ControlContainerItem> {

		public DefaultBoxItemAdapter(List<BoxItem> boxItems, Priority priority,
				AbstractContainerItemsCalculator<ControlContainerItem> packagerContainerItems,
				PackagerInterruptSupplier interrupt) {
			super(boxItems, priority, packagerContainerItems, interrupt);
		}

		@Override
		protected IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, ControlContainerItem containerItem,
				PackagerInterruptSupplier interrupt, Priority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
			return AbstractControlPackager.this.pack(remainingBoxItems, containerItem, interrupt, priority, abortOnAnyBoxTooBig);
		}

	}
	
	protected class DefaultBoxItemGroupAdapter extends AbstractBoxItemGroupAdapter<ControlContainerItem> {

		public DefaultBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups,
				Priority priority,
				AbstractContainerItemsCalculator<ControlContainerItem> packagerContainerItems, 
				PackagerInterruptSupplier interrupt) {
			super(boxItemGroups, packagerContainerItems, priority, interrupt);
		}

		@Override
		protected IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Priority priority,
				ControlContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
			return AbstractControlPackager.this.packGroup(remainingBoxItemGroups, priority, containerItem, interrupt, abortOnAnyBoxTooBig);
		}

	}
	
	public class DefaultControlsPackagerResultBuilder extends AbstractControlsPackagerResultBuilder<DefaultControlsPackagerResultBuilder> {

		public PackagerResult build() {
			validate();
			
			if( (items == null || items.isEmpty()) && (itemGroups == null || itemGroups.isEmpty())) {
				throw new IllegalStateException();
			}
			long start = System.currentTimeMillis();

			PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
			if(deadline != -1L) {
				booleanSupplierBuilder.withDeadline(deadline);
			}
			if(interrupt != null) {
				booleanSupplierBuilder.withInterrupt(interrupt);
			}

			booleanSupplierBuilder.withScheduledThreadPoolExecutor(getScheduledThreadPoolExecutor());

			PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
			try {
				PackagerAdapter adapter;
				if(items != null && !items.isEmpty()) {
					adapter = new DefaultBoxItemAdapter(items, priority, new ControlContainerItemsCalculator(containers), interrupt);
				} else {
					adapter = new DefaultBoxItemGroupAdapter(itemGroups, priority, new ControlContainerItemsCalculator(containers), interrupt);
				}
				List<Container> packList = packAdapter(maxContainerCount, interrupt, adapter);
				
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(packList, duration, false);
			} catch (PackagerInterruptedException e) {
				long duration = System.currentTimeMillis() - start;
				return new PackagerResult(Collections.emptyList(), duration, true);
			} finally {
				interrupt.close();
			}
		}

	}

	@Override
	public DefaultControlsPackagerResultBuilder newResultBuilder() {
		return new DefaultControlsPackagerResultBuilder();
	}

	public IntermediatePackagerResult pack(List<BoxItem> boxItems, ControlContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, Priority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		Container container = compositeContainerItem.getContainer();

		Stack stack = new Stack();

		ExtremePoints extremePoints = new ExtremePoints3D();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(boxItems);
		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
		
		// remove boxes which do not fit due to volume, weight or dimensions
		List<BoxItem> removed = new ArrayList<>();
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return EmptyPackagerResultAdapter.EMPTY;
				}
				
				if(priority != Priority.CRONOLOGICAL) {
					removed.add(filteredBoxItems.remove(i));
					i--;
				} else {
					// remove all later then the first removed
					while(i < filteredBoxItems.size()) {
						removed.add(filteredBoxItems.remove(i));
					}
				}
			}
		}
		
		if(!removed.isEmpty()) {
			boxItemControls.declined(removed);
			pointControls.declined(removed);
			
			removed.clear();
		}
		
		long maxBoxArea = filteredBoxItems.getMaxArea();
		long maxBoxVolume = filteredBoxItems.getMaxVolume();
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}

			IntermediatePlacementResult result = findBestPoint(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);
			if(result == null) {
				break;
			}
			
			StackPlacement stackPlacement = new StackPlacement(result.getStackValue(), 
				result.getPoint().getMinX(), 
				result.getPoint().getMinY(), 
				result.getPoint().getMinZ()
			);

			stack.add(stackPlacement);
			extremePoints.add(result.getPoint(), stackPlacement);
			
			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
			remainingLoadVolume -= result.getBoxItem().getBox().getVolume();
			
			filteredBoxItems.decrement(result.getIndex(), 1);

			boxItemControls.accepted(result.getBoxItem());
			pointControls.accepted(result.getBoxItem());
			
			if(!filteredBoxItems.isEmpty()) {
				
				// remove items are too big according to total volume / weight
				for(int i = 0; i < filteredBoxItems.size(); i++) {
					BoxItem boxItem = filteredBoxItems.get(i);
					Box box = boxItem.getBox();
					if(box.getVolume() > remainingLoadVolume || box.getWeight() > remainingLoadWeight) {
						
						if(abortOnAnyBoxTooBig) {
							return EmptyPackagerResultAdapter.EMPTY;
						}
						
						if(priority != Priority.CRONOLOGICAL) {
							removed.add(filteredBoxItems.remove(i));
							i--;
						} else {
							// remove all later then the first removed
							while(i < filteredBoxItems.size()) {
								removed.add(filteredBoxItems.remove(i));
							}							
						}
					}
				}
				if(!removed.isEmpty()) {
					boxItemControls.declined(removed);
					pointControls.declined(removed);
					
					removed.clear();
				}
				
				// remove small points
				extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());				
				
				// remove boxes which are too big for the available points
				long maxPointArea = extremePoints.getMaxArea();
				long maxPointVolume = extremePoints.getMaxVolume();
				
				if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
					for(int i = 0; i < filteredBoxItems.size(); i++) {
						BoxItem boxItem = filteredBoxItems.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
							
							if(abortOnAnyBoxTooBig) {
								return EmptyPackagerResultAdapter.EMPTY;
							}
							
							if(priority != Priority.CRONOLOGICAL) {
								removed.add(filteredBoxItems.remove(i));
								i--;
							} else {
								// remove all later then the first removed
								while(i < filteredBoxItems.size()) {
									removed.add(filteredBoxItems.remove(i));
								}					
							}
						}
					}
					
					if(!removed.isEmpty()) {
						boxItemControls.declined(removed);
						pointControls.declined(removed);
						
						removed.clear();
					}
				}
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem, stack);
	}


	public IntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, Priority priority, ControlContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
		ContainerItem containerItem = compositeContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D(true);
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);

		FilteredBoxItems filteredBoxItems = packagerBoxItems.getFilteredBoxItems();
		
		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);

		BoxItemGroupControls boxItemGroupControls = boxItemControls instanceof BoxItemGroupControls ? (BoxItemGroupControls)boxItemControls : null;

		FilteredBoxItemGroups filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();
						
		List<BoxItem> removedBoxItems = new ArrayList<>();

		if(priority != Priority.CRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup)) {
					if(abortOnAnyBoxTooBig) {
						return EmptyPackagerResultAdapter.EMPTY;
					}
					if(priority != Priority.CRONOLOGICAL) {
						filteredBoxItemGroups.remove(i);
						i--;
						
						removedBoxItems.addAll(boxItemGroup.getItems());
						
						if(boxItemGroupControls != null) {
							boxItemGroupControls.declined(boxItemGroup);
						}

					} else {
						// remove all later then the first removed

						while(i < filteredBoxItems.size()) {
							filteredBoxItemGroups.remove(i);
							i--;
							
							removedBoxItems.addAll(boxItemGroup.getItems());

							if(boxItemGroupControls != null) {
								boxItemGroupControls.declined(boxItemGroup);
							}
						}
					}		
					
	
				}
			}
			
			if(!removedBoxItems.isEmpty()) {
				boxItemControls.declined(removedBoxItems);
				pointControls.declined(removedBoxItems);
				
				removedBoxItems.clear();
			}
		}
			
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, priority, container, extremePoints);

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		long maxBoxVolume = filteredBoxItems.getMaxVolume();
		long maxBoxArea = filteredBoxItems.getMaxVolume();

		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !extremePoints.isEmpty() && boxItemGroupIterator.hasNext()) {
			int groupIndex = boxItemGroupIterator.next();
			
			int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndexForGroup(groupIndex);
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			boxItemGroup.mark();
			
			extremePoints.mark();
			int markStackSize = stack.size();
			
			if(boxItemGroupControls != null) {
				boxItemGroupControls.attempt(boxItemGroup, boxItemStartIndex, boxItemGroup.size());
			}
			
			while(!boxItemGroup.isEmpty()) {
				
				IntermediatePlacementResult bestPoint = findBestPoint(filteredBoxItems, boxItemStartIndex, boxItemGroup.size(), priority, pointControls, container, extremePoints, stack);				
				if(bestPoint == null) {
					break;
				}
				
				StackPlacement stackPlacement = new StackPlacement(bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints.add(bestPoint.getPoint(), stackPlacement);
				
				remainingLoadWeight -= bestPoint.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= bestPoint.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(bestPoint.getIndex());
				
				boxItemControls.accepted(bestPoint.getBoxItem());
				pointControls.accepted(bestPoint.getBoxItem());

				if(!filteredBoxItems.isEmpty()) {
					// remove groups are too big according to total volume / weight
					
					for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
						BoxItemGroup g = filteredBoxItemGroups.get(i);
						if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
							
							if(abortOnAnyBoxTooBig) {
								return EmptyPackagerResultAdapter.EMPTY;
							}
							
							filteredBoxItemGroups.remove(i);
							i--;
							
							removedBoxItems.addAll(boxItemGroup.getItems());
							
							if(boxItemGroupControls != null) {
								boxItemGroupControls.declined(boxItemGroup);
							}
						}
					}
					
					if(!removedBoxItems.isEmpty()) {
						boxItemControls.declined(removedBoxItems);
						pointControls.declined(removedBoxItems);
						
						removedBoxItems.clear();
					}
					
					// remove / constrain to small points
					extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
					
					// remove groups which have boxes which are too big for the current points
					long maxPointArea = extremePoints.getMaxArea();
					long maxPointVolume = extremePoints.getMaxVolume();
										
					if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
	
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);
	
							for(int k = 0; k < g.size(); k++) {
								BoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
	
									if(abortOnAnyBoxTooBig) {
										return EmptyPackagerResultAdapter.EMPTY;
									}
									
									filteredBoxItemGroups.remove(i);
									i--;
									
									removedBoxItems.addAll(boxItemGroup.getItems());
									
									if(boxItemGroupControls != null) {
										boxItemGroupControls.declined(boxItemGroup);
									}
									break;
								}
							}				
						}
						
						if(!removedBoxItems.isEmpty()) {
							boxItemControls.declined(removedBoxItems);
							pointControls.declined(removedBoxItems);
							
							removedBoxItems.clear();
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
					return EmptyPackagerResultAdapter.EMPTY;
				}

				
				// undo any work on this group
				for(int i = markStackSize; i < stack.size(); i++) {
					removedBoxItems.add(stack.getPlacements().get(i).getStackValue().getBox().getBoxItem());
				}
				if(!removedBoxItems.isEmpty()) {
					boxItemControls.undo(removedBoxItems);
					
					removedBoxItems.clear();
				}
				
				removedBoxItems.addAll(boxItemGroup.getItems());
				boxItemControls.declined(removedBoxItems);
				pointControls.declined(removedBoxItems);
				removedBoxItems.clear();
				
				if(boxItemGroupControls != null) {
					boxItemGroupControls.declined(boxItemGroup);
				}

				stack.setSize(markStackSize);
				
				// unable to stack whole group
				if(priority == Priority.CRONOLOGICAL) {
					break groups;
				}
				// try again with another group if possible
				extremePoints.reset();

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < extremePoints.calculateUsedWeight()) {
				throw new RuntimeException();
			}
			
			// TODO
			// remove points which are invalid for use with a new group
			// i.e. inner points which cannot be accessed without moving
			// something else.
			
			// successfully stacked group
			boxItemGroup.reset();
			if(boxItemGroupControls != null) {
				boxItemGroupControls.accepted(boxItemGroup);
			}
		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	protected abstract BoxItemGroupIterator createBoxItemGroupIterator(
			FilteredBoxItemGroups groups, 
			Priority itemGroupOrder, 
			Container container,
			ExtremePoints extremePoints
		);

	protected abstract IntermediatePlacementResult findBestPoint(
			FilteredBoxItems boxItems,
			int offset, int length,
			Priority priority,
			PointControls pointControls,
			Container container, 
			ExtremePoints extremePoints, Stack stack
		);
}
