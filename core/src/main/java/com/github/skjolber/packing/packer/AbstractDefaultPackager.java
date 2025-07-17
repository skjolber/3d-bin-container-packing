package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.BoxItemGroupControls;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItemGroups;
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

		public DefaultBoxItemAdapter(List<BoxItem> boxItems, List<CompositeContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<BoxItem> boxClones = new LinkedList<>();
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
				return AbstractDefaultPackager.this.pack(remainingBoxItems, containerItems.get(index), interrupt);
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
		private final Order itemGroupOrder;

		public DefaultBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, List<CompositeContainerItem> containerItems, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<BoxItemGroup> groupClones = new LinkedList<>();
			for (BoxItemGroup boxItemGroup : boxItemGroups) {
				BoxItemGroup clone = boxItemGroup.clone();
				clone.setIndex(groupClones.size());
				groupClones.add(clone);
				clone.mark();
			}

			this.remainingBoxItemGroups = groupClones;
			this.itemGroupOrder = itemGroupOrder;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultIntermediatePackagerResult attempt(int index, DefaultIntermediatePackagerResult best) throws PackagerInterruptedException {
			try {
				return AbstractDefaultPackager.this.pack(remainingBoxItemGroups, itemGroupOrder, containerItems.get(index), interrupt);
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
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItem> boxItems, List<CompositeContainerItem> containers, PackagerInterruptSupplier interrupt) {
		return new DefaultBoxItemAdapter(boxItems, containers, interrupt);
	}
	
	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
		return new DefaultBoxItemGroupAdapter(boxes, containers, itemGroupOrder, interrupt);
	}

	@Override
	public DefaultPackagerResultBuilder newResultBuilder() {
		return new DefaultPackagerResultBuilder().withPackager(this);
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxItems, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		Stack stack = new Stack();

		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		ExtremePoints3D extremePoints = new ExtremePoints3D();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, null, new DefaultFilteredBoxItems(boxItems), extremePoints);

		FilteredBoxItems filteredBoxItems = boxItemControls.getFilteredBoxItems();
		PointControls pointControls = compositeContainerItem.createPointControls(container, stack, null, filteredBoxItems, extremePoints);

		// remove boxes which do not fit at all
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			BoxItem boxItem = filteredBoxItems.get(i);
			if(!container.fitsInside(boxItem.getBox())) {
				filteredBoxItems.remove(i);
				i--;
				
				boxItemControls.declined(boxItem);
				pointControls.declined(boxItem);
			}
		}
		
		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		int remainingLoadWeight = container.getMaxLoadWeight();

		while (!extremePoints.isEmpty() && remainingLoadWeight > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacementResult result = findBestPoint(null, filteredBoxItems, pointControls, container, extremePoints, stack);
			if(result == null) {
				break;
			}

			Point point = result.getPoint();
			
			StackPlacement stackPlacement = new StackPlacement(result.getStackValue(), point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints.add(result.getPoint(), stackPlacement);

			result.getBoxItem().decrement();

			filteredBoxItems.removeEmpty();

			boxItemControls.accepted(result.getBoxItem());
			pointControls.accepted(result.getBoxItem());
			
			filteredBoxItems.removeEmpty();
	
			if(!filteredBoxItems.isEmpty()) {
				extremePoints.updateMinimums(result.getStackValue(), filteredBoxItems);
			}

			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
		}
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints = new MarkResetExtremePoints3D();
		extremePoints.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

		BoxItemGroupControls boxItemGroupControls = compositeContainerItem.createBoxItemGroupControls(container, stack, new DefaultFilteredBoxItemGroups(new ArrayList<>(boxItemGroups)), extremePoints);

		FilteredBoxItemGroups filteredBoxItemGroups = boxItemGroupControls.getFilteredBoxItemGroups();
		
		// remove groups which do not fit at all
		for(int i = 0; i < filteredBoxItemGroups.size(); i++) {
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.get(i);
			if(!container.fitsInside(boxItemGroup)) {
				filteredBoxItemGroups.remove(i);
				i--;
				
				boxItemGroupControls.declined(boxItemGroup);
			}
		}

		extremePoints.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());

		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, itemGroupOrder, container, extremePoints);

		groups:
		while (!extremePoints.isEmpty() && boxItemGroupIterator.hasNext()) {
			BoxItemGroup boxItemGroup = filteredBoxItemGroups.remove(boxItemGroupIterator.next());
			boxItemGroup.mark();

			BoxItemControls boxItemControls = compositeContainerItem.createBoxItemControls(container, stack, filteredBoxItemGroups, boxItemGroup, extremePoints);

			extremePoints.mark();
			int markStackSize = stack.size();
			
			FilteredBoxItems filteredBoxItems = boxItemControls.getFilteredBoxItems();

			PointControls pointControls = compositeContainerItem.createPointControls(container, stack, filteredBoxItemGroups, filteredBoxItems, extremePoints);

			while(!filteredBoxItems.isEmpty()) {
				
				IntermediatePlacementResult bestPoint = findBestPoint(filteredBoxItemGroups, filteredBoxItems, pointControls, container, extremePoints, stack);
				if(bestPoint == null) {
					boxItemGroup.reset();
					boxItemGroupControls.declined(boxItemGroup);

					if(itemGroupOrder == Order.FIXED) {
						break groups;
					}
					// discard the whole group
					extremePoints.reset();
					stack.setSize(markStackSize);
					
					continue groups;
				}
				
				BoxItem boxItem = bestPoint.getBoxItem();
				
				boxItem.decrement();

				filteredBoxItems.removeEmpty();
				
				StackPlacement stackPlacement = new StackPlacement(bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints.add(bestPoint.getPoint(), stackPlacement);

				if(!filteredBoxItemGroups.isEmpty()) {
					extremePoints.updateMinimums(bestPoint.getStackValue(), filteredBoxItemGroups);
				}
				
				boxItemControls.accepted(boxItem);
				pointControls.accepted(boxItem);
				
				filteredBoxItems.removeEmpty();
			}
			
			if(container.getMaxLoadWeight() < extremePoints.getUsedWeight()) {
				throw new RuntimeException();
			}
			boxItemGroup.reset();

			// successfully stacked group
			boxItemGroupControls.accepted(boxItemGroup);
		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	protected abstract BoxItemGroupIterator createBoxItemGroupIterator(
			FilteredBoxItemGroups groups, 
			Order itemGroupOrder, 
			Container container,
			ExtremePoints extremePoints
		);

	protected abstract IntermediatePlacementResult findBestPoint(
			FilteredBoxItemGroups groups,
			FilteredBoxItems boxItems,
			PointControls pointControls,
			Container container,
			ExtremePoints extremePoints, 
			Stack stack
		);
}
