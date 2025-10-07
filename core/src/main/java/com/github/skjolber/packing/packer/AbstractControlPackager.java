package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.placement.PlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;
import com.github.skjolber.packing.iterator.PackagerBoxItems;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation.
 */
public abstract class AbstractControlPackager<I extends Placement, P extends IntermediatePackagerResult, B extends PackagerResultBuilder> extends AbstractPackager<P, B> {

	public AbstractControlPackager(Comparator<P> comparator) {
		super(comparator);
	}

	public P pack(List<BoxItem> boxItems, ControlledContainerItem controlContainerItem, PackagerInterruptSupplier interrupt, BoxPriority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		Container container = controlContainerItem.getContainer();

		Stack stack = new Stack();

		PointCalculator pointCalculator = new ExtremePoints3D();
		pointCalculator.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		DefaultBoxItemSource boxItemSource = new DefaultBoxItemSource(boxItems);
		ManifestControls manifestControls = controlContainerItem.createBoxItemControls(container, stack, boxItemSource, pointCalculator, null);

		PointControls pointControls = controlContainerItem.createPointControls(container, stack, boxItemSource, pointCalculator);
		
		// remove boxes which do not fit due to volume, weight or dimensions
		List<BoxItem> removed = new ArrayList<>(boxItemSource.size());
		for(int i = 0; i < boxItemSource.size(); i++) {
			BoxItem boxItem = boxItemSource.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(abortOnAnyBoxTooBig) {
					return createEmptyIntermediatePackagerResult();
				}
				
				if(priority != BoxPriority.CRONOLOGICAL) {
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
			
			removed.clear();
		}
		
		long maxBoxArea = boxItemSource.getMaxArea();
		long maxBoxVolume = boxItemSource.getMaxVolume();
		
		pointCalculator.setMinimumAreaAndVolumeLimit(boxItemSource.getMinArea(), boxItemSource.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		PlacementControls<I> placementControls = createControls(boxItemSource, 0, boxItemSource.size(), priority, pointControls, container, pointCalculator, stack);

		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !boxItemSource.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				throw new PackagerInterruptedException();
			}

			Placement placement = placementControls.getPlacement(0, boxItemSource.size());
			if(placement == null) {
				break;
			}
			
			stack.add(placement);
			pointCalculator.add(placement.getPoint(), placement);
			
			remainingLoadWeight -= placement.getBoxItem().getBox().getWeight();
			remainingLoadVolume -= placement.getBoxItem().getBox().getVolume();
			
			boxItemSource.decrement(placement.getBoxItem().getIndex(), 1);

			manifestControls.accepted(placement.getBoxItem());
			pointControls.accepted(placement.getBoxItem());
			
			if(!boxItemSource.isEmpty()) {
				
				// remove items are too big according to total volume / weight
				for(int i = 0; i < boxItemSource.size(); i++) {
					BoxItem boxItem = boxItemSource.get(i);
					Box box = boxItem.getBox();
					if(box.getVolume() > remainingLoadVolume || box.getWeight() > remainingLoadWeight) {
						
						if(abortOnAnyBoxTooBig) {
							return createEmptyIntermediatePackagerResult();
						}
						
						if(priority != BoxPriority.CRONOLOGICAL) {
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
					
					removed.clear();
				}
				
				// remove small points
				pointCalculator.setMinimumAreaAndVolumeLimit(boxItemSource.getMinArea(), boxItemSource.getMinVolume());				
				
				// remove boxes which are too big for the available points
				long maxPointArea = pointCalculator.getMaxArea();
				long maxPointVolume = pointCalculator.getMaxVolume();
				
				if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
					for(int i = 0; i < boxItemSource.size(); i++) {
						BoxItem boxItem = boxItemSource.get(i);
						Box box = boxItem.getBox();
						if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
							
							if(abortOnAnyBoxTooBig) {
								return createEmptyIntermediatePackagerResult();
							}
							
							if(priority != BoxPriority.CRONOLOGICAL) {
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
						
						removed.clear();
					}
				}
			}
		}
		
		// ignore decline for the rest
		
		return createIntermediatePackagerResult(controlContainerItem, stack);
	}

	public P packGroup(List<BoxItemGroup> boxItemGroups, BoxPriority priority, ControlledContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) {
		ContainerItem containerItem = compositeContainerItem;
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D(true);
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		
		PackagerBoxItems packagerBoxItems = new PackagerBoxItems(boxItemGroups);

		BoxItemSource filteredBoxItems = packagerBoxItems.getFilteredBoxItems();

		BoxItemGroupSource filteredBoxItemGroups = packagerBoxItems.getFilteredBoxItemGroups();

		ManifestControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints, filteredBoxItemGroups);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
						
		List<BoxItemGroup> removedBoxItemGroups = new ArrayList<>();

		if(priority != BoxPriority.CRONOLOGICAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup)) {
					if(abortOnAnyBoxTooBig) {
						return createEmptyIntermediatePackagerResult();
					}
					if(priority != BoxPriority.CRONOLOGICAL) {
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
				if(boxItemControls != null) {
					boxItemControls.filteredGroups(removedBoxItemGroups);
				}
				if(pointControls != null) {
					pointControls.filteredGroups(removedBoxItemGroups);
				}
				removedBoxItemGroups.clear();
			}
		}
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());
		
		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, priority, container, extremePoints);

		int remainingLoadWeight = container.getMaxLoadWeight();
		long remainingLoadVolume = container.getMaxLoadVolume();

		long maxBoxVolume = filteredBoxItems.getMaxVolume();
		long maxBoxArea = filteredBoxItems.getMaxVolume();
		
		PlacementControls<I> placementControls = createControls(filteredBoxItems, 0, filteredBoxItems.size(), priority, pointControls, container, extremePoints, stack);

		groups:
		while (remainingLoadWeight > 0 && remainingLoadVolume > 0 && !extremePoints.isEmpty() && boxItemGroupIterator.hasNext()) {
			int groupIndex = boxItemGroupIterator.next();
			
			int boxItemStartIndex = packagerBoxItems.getFirstBoxItemIndexForGroup(groupIndex);
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			boxItemGroup.mark();
			
			extremePoints.mark();
			int markStackSize = stack.size();
			
			boxItemControls.attempt(boxItemGroup, boxItemStartIndex, boxItemGroup.size());
			
			while(!boxItemGroup.isEmpty()) {
				
				Placement placement = placementControls.getPlacement(boxItemStartIndex, boxItemGroup.size());				
				if(placement == null) {
					break;
				}
				
				stack.add(placement);
				extremePoints.add(placement.getPoint(), placement);
				
				remainingLoadWeight -= placement.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= placement.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				packagerBoxItems.decrement(placement.getBoxItem().getIndex());

				boxItemControls.accepted(placement.getBoxItem());
				pointControls.accepted(placement.getBoxItem());

				if(!filteredBoxItemGroups.isEmpty()) {
					// remove groups are too big according to total volume / weight
					
					for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
						BoxItemGroup g = filteredBoxItemGroups.get(i);
						if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
							
							if(abortOnAnyBoxTooBig) {
								return createEmptyIntermediatePackagerResult();
							}
							
							if(priority != BoxPriority.CRONOLOGICAL) {
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
						if(boxItemControls != null) {
							boxItemControls.filteredGroups(removedBoxItemGroups);
						}
						if(pointControls != null) {
							pointControls.filteredGroups(removedBoxItemGroups);
						}
						removedBoxItemGroups.clear();
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
										return createEmptyIntermediatePackagerResult();
									}

									if(priority != BoxPriority.CRONOLOGICAL) {
										filteredBoxItemGroups.remove(i);
										i--;
										
										removedBoxItemGroups.add(boxItemGroup);
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
							if(boxItemControls != null) {
								boxItemControls.filteredGroups(removedBoxItemGroups);
							}
							if(pointControls != null) {
								pointControls.filteredGroups(removedBoxItemGroups);
							}
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

				List<BoxItem> removedBoxItems = new ArrayList<>();
				// undo any work on this group
				for(int i = markStackSize; i < stack.size(); i++) {
					removedBoxItems.add(stack.getPlacements().get(i).getStackValue().getBox().getBoxItem());
				}
				if(!removedBoxItems.isEmpty()) {
					if(boxItemControls != null) {
						boxItemControls.undo(removedBoxItems);
					}
					if(pointControls != null) {
						pointControls.undo(removedBoxItems);
					}
					
					removedBoxItems.clear();
				}
				
				if(boxItemControls != null) {
					boxItemControls.attemptFailure(boxItemGroup);
				}
				if(pointControls != null) {
					pointControls.attemptFailure(boxItemGroup);
				}
				
				stack.setSize(markStackSize);
				
				// unable to stack whole group
				if(priority == BoxPriority.CRONOLOGICAL) {
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
			if(boxItemControls != null) {
				boxItemControls.attemptSuccess(boxItemGroup);
			}
			if(pointControls != null) {
				pointControls.attemptSuccess(boxItemGroup);
			}
		}
		
		return createIntermediatePackagerResult(containerItem, stack);
	}

	protected abstract P createIntermediatePackagerResult(ContainerItem containerItem, Stack stack);

	protected abstract P createEmptyIntermediatePackagerResult();

	protected abstract BoxItemGroupIterator createBoxItemGroupIterator(
			BoxItemGroupSource groups, 
			BoxPriority itemGroupOrder, 
			Container container,
			PointCalculator pointCalculator
		);

	protected abstract PlacementControls<I> createControls(
			BoxItemSource boxItems,
			int offset, int length,
			BoxPriority priority,
			PointControls pointControls,
			Container container, 
			PointCalculator pointCalculator, Stack stack
		);
}
