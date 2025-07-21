package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Priority;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemGroupControls;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultBoxItemGroupFilteredBoxItems;
import com.github.skjolber.packing.api.packager.DefaultBoxItemGroupFilteredBoxItems.InnerFilteredBoxItemGroup;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
import com.github.skjolber.packing.api.packager.PointControls;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.iterator.BoxItemGroupIterator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractDefaultPackager extends AbstractPackager<DefaultIntermediatePackagerResult, DefaultPackagerResultBuilder> {

	public AbstractDefaultPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	protected class DefaultBoxItemAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<BoxItem> remainingBoxItems;
		private final PackagerInterruptSupplier interrupt;
		private final Priority priority;

		public DefaultBoxItemAdapter(List<BoxItem> boxItems, Priority priority, List<CompositeContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
			super(containerItems);
			this.priority = priority;
			
			List<BoxItem> boxClones = new ArrayList<>(boxItems.size());
			for (BoxItem item : boxItems) {
				BoxItem clone = item.clone();
				clone.setIndex(boxClones.size());
				boxClones.add(clone);
			}
			this.remainingBoxItems = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultIntermediatePackagerResult attempt(int index, DefaultIntermediatePackagerResult best) throws PackagerInterruptedException {
			try {
				return AbstractDefaultPackager.this.pack(remainingBoxItems, containerItems.get(index), interrupt, priority);
			} finally {
				for(BoxItem boxItem : remainingBoxItems) {
					boxItem.reset();
				}
			}				
		}

		@Override
		public Container accept(DefaultIntermediatePackagerResult result) {
			Container container = super.toContainer(result.getContainerItem(), result.getStack());

			Stack stack = container.getStack();

			for (StackPlacement stackPlacement : stack.getPlacements()) {
				BoxItem boxItem = (BoxItem) stackPlacement.getStackValue().getBox().getBoxItem();
				
				boxItem.decrementResetCount();
				boxItem.reset();
			}
			
			List<BoxItem> remainingBoxItems = new ArrayList<>(this.remainingBoxItems.size());
			for (BoxItem boxItem : this.remainingBoxItems) {
				if(!boxItem.isEmpty()) {
					remainingBoxItems.add(boxItem);
				}
			}
			this.remainingBoxItems = remainingBoxItems;

			return container;
		}

		@Override
		public List<Integer> getContainers(int maxCount) {
			return getContainers(remainingBoxItems, maxCount);
		}

		@Override
		public int countRemainingBoxes() {
			int count = 0;
			for(BoxItem boxItem : remainingBoxItems) {
				count += boxItem.getCount();
			}
			return count;
		}

	}
	
	protected class DefaultBoxItemGroupAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<BoxItemGroup> remainingBoxItemGroups;
		private final PackagerInterruptSupplier interrupt;
		private final Priority priority;

		public DefaultBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, List<CompositeContainerItem> containerItems, Priority priority, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<BoxItemGroup> groupClones = new LinkedList<>();
			for (BoxItemGroup boxItemGroup : boxItemGroups) {
				BoxItemGroup clone = boxItemGroup.clone();
				clone.setIndex(groupClones.size());
				groupClones.add(clone);
				clone.mark();
			}

			this.remainingBoxItemGroups = groupClones;
			this.priority = priority;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultIntermediatePackagerResult attempt(int index, DefaultIntermediatePackagerResult best) throws PackagerInterruptedException {
			try {
				return AbstractDefaultPackager.this.packGroup(remainingBoxItemGroups, priority, containerItems.get(index), interrupt);
			} finally {
				for(BoxItemGroup group : remainingBoxItemGroups) {
					group.reset();
				}
			}				
		}

		@Override
		public Container accept(DefaultIntermediatePackagerResult result) {
			Container container = super.toContainer(result.getContainerItem(), result.getStack());

			Stack stack = container.getStack();

			for (StackPlacement stackPlacement : stack.getPlacements()) {
				BoxItem boxItem = (BoxItem) stackPlacement.getStackValue().getBox().getBoxItem();
				
				boxItem.decrementResetCount();
				boxItem.reset();
			}

			List<BoxItemGroup> remainingBoxItems = new ArrayList<>(this.remainingBoxItemGroups.size());
			for (BoxItemGroup boxItem : this.remainingBoxItemGroups) {
				if(!boxItem.isEmpty()) {
					remainingBoxItems.add(boxItem);
				}
			}
			this.remainingBoxItemGroups = remainingBoxItems;

			return container;
		}

		@Override
		public List<Integer> getContainers(int maxCount) {
			return getGroupContainers(remainingBoxItemGroups, maxCount);
		}
		
		@Override
		public int countRemainingBoxes() {
			int count = 0;
			for(BoxItemGroup group : remainingBoxItemGroups) {
				count += group.getBoxCount();
			}
			return count;
		}

	}

	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItem> boxItems, Priority priority, List<CompositeContainerItem> containers, PackagerInterruptSupplier interrupt) {
		return new DefaultBoxItemAdapter(boxItems, priority, containers, interrupt);
	}
	
	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> groupAdapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Priority priority, PackagerInterruptSupplier interrupt) {
		return new DefaultBoxItemGroupAdapter(boxes, containers, priority, interrupt);
	}

	@Override
	public DefaultPackagerResultBuilder newResultBuilder() {
		return new DefaultPackagerResultBuilder().withPackager(this);
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxItems, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt, Priority priority) throws PackagerInterruptedException {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		ExtremePoints extremePoints = new ExtremePoints3D();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(boxItems);
		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);
		
		// remove boxes which do not fit due to volume, weight or dimensions
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {

				if(priority != Priority.NATURAL) {
					filteredBoxItems.remove(i);
					i--;
					
					boxItemControls.declined(boxItem);
					pointControls.declined(boxItem);
				} else {
					// remove all later then the first removed

					
					
					
				}
			}
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
						if(priority != Priority.NATURAL) {
							filteredBoxItems.remove(i);
							i--;
							
							boxItemControls.declined(boxItem);
							pointControls.declined(boxItem);
						} else {
							// remove all later then the first removed
							
							
						}
					}
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
							if(priority != Priority.NATURAL) {
								filteredBoxItems.remove(i);
								i--;
								
								boxItemControls.declined(boxItem);
								pointControls.declined(boxItem);
							} else {
								// remove all later then the first removed
								
								
								
							}
						}
					}				
				}
			}
		}
		
		// ignore decline for the rest
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}


	public DefaultIntermediatePackagerResult packGroup(List<BoxItemGroup> boxItemGroups, Priority priority, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();
		
		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		
		DefaultBoxItemGroupFilteredBoxItems filteredBoxItems = new DefaultBoxItemGroupFilteredBoxItems(boxItemGroups);

		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItems, extremePoints);

		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItems, extremePoints);

		BoxItemGroupControls boxItemGroupControls = boxItemControls instanceof BoxItemGroupControls ? (BoxItemGroupControls)boxItemControls : null;

		InnerFilteredBoxItemGroup filteredBoxItemGroups = filteredBoxItems.getGroups();
						
		if(priority != Priority.NATURAL) {
	
			// remove boxes which do not fit due to volume, weight or stack value dimensions
			for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
				if(!container.fitsInside(boxItemGroup)) {
					filteredBoxItemGroups.remove(i);
					i--;
					
					for(int k = 0; k < boxItemGroup.size(); k++) {
						boxItemControls.declined(boxItemGroup.get(k));
						pointControls.declined(boxItemGroup.get(k));
					}
					
					if(boxItemGroupControls != null) {
						boxItemGroupControls.declined(boxItemGroup);
					}
	
				}
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
			
			int boxItemStartIndex = filteredBoxItems.getGroupStartIndex(groupIndex);
			
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(groupIndex);
			boxItemGroup.mark();
			
			extremePoints.mark();
			int markStackSize = stack.size();
			
			if(boxItemGroupControls != null) {
				boxItemGroupControls.attempt(boxItemGroup, boxItemStartIndex, boxItemGroup.size());
			}
			
			while(!boxItemGroup.isEmpty()) {
				
				IntermediatePlacementResult bestPoint = findBestPoint(filteredBoxItems, boxItemStartIndex, boxItemGroup.size(), priority, pointControls, container, extremePoints, stack);				
				
				StackPlacement stackPlacement = new StackPlacement(bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints.add(bestPoint.getPoint(), stackPlacement);
				
				remainingLoadWeight -= bestPoint.getBoxItem().getBox().getWeight();
				remainingLoadVolume -= bestPoint.getBoxItem().getBox().getVolume();
				
				// decrement box item without deleting the whole group
				bestPoint.getBoxItem().decrement();
				boxItemGroup.removeEmpty();
				filteredBoxItems.removeEmpty(false);
				
				boxItemControls.accepted(bestPoint.getBoxItem());
				pointControls.accepted(bestPoint.getBoxItem());

				if(!filteredBoxItems.isEmpty()) {
					// remove groups are too big according to total volume / weight
					
					for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
						BoxItemGroup g = filteredBoxItemGroups.get(i);
						if(g.getVolume() > remainingLoadVolume || g.getWeight() > remainingLoadWeight) {
							filteredBoxItemGroups.remove(i);
							i--;
							
							for(int k = 0; k < boxItemGroup.size(); k++) {
								boxItemControls.declined(boxItemGroup.get(k));
								pointControls.declined(boxItemGroup.get(k));
							}
							
							if(boxItemGroupControls != null) {
								boxItemGroupControls.declined(boxItemGroup);
							}
						}
					}
					
					// remove small points
					extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());
					
					// remove groups which have boxes which are too big for the current points
					long maxPointArea = extremePoints.getMaxArea();
					long maxPointVolume = extremePoints.getMaxVolume();
										
					if(maxPointArea < maxBoxArea || maxPointVolume < maxBoxVolume) {
	
						g:
						for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
							BoxItemGroup g = filteredBoxItemGroups.get(i);
	
							for(int k = 0; k < g.size(); k++) {
								BoxItem boxItem = g.get(k);
								Box box = boxItem.getBox();
								if(box.getVolume() > maxPointVolume || box.getMinimumArea() > maxPointArea) {
	
									filteredBoxItemGroups.remove(i);
									i--;
									
									for(int l = 0; l < boxItemGroup.size(); l++) {
										boxItemControls.declined(boxItemGroup.get(l));
										pointControls.declined(boxItemGroup.get(l));
									}
									
									if(boxItemGroupControls != null) {
										boxItemGroupControls.declined(boxItemGroup);
									}
									continue g;
									
								}
							}				
						}
					}
					
				}
				
				if(!filteredBoxItemGroups.contains(boxItemGroup)) {
					// the current group was removed, assume packaging unsuccessful.
					break;
				}
			}

			boolean removed = !filteredBoxItemGroups.contains(boxItemGroup);
			if(!removed) {
				filteredBoxItemGroups.remove(boxItemGroup);
			}
			
			if(removed || !boxItemGroup.isEmpty()) {				
				boxItemGroup.reset();

				for(int i = markStackSize; i < stack.size(); i++) {
					boxItemControls.undo(stack.getPlacements().get(i).getStackValue().getBox().getBoxItem());
				}
				
				for(int k = 0; k < boxItemGroup.size(); k++) {
					boxItemControls.declined(boxItemGroup.get(k));
					pointControls.declined(boxItemGroup.get(k));
				}
				
				if(boxItemGroupControls != null) {
					boxItemGroupControls.declined(boxItemGroup);
				}

				stack.setSize(markStackSize);
				
				// unable to stack whole group
				if(priority == Priority.NATURAL) {
					break groups;
				}
				// try again with another group if possible
				extremePoints.reset();

				continue groups;
			}
			
			if(container.getMaxLoadWeight() < extremePoints.getUsedWeight()) {
				throw new RuntimeException();
			}
			
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
