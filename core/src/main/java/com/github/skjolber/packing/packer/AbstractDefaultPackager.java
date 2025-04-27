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
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderFactory;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;
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
				BoxItem boxItem = (BoxItem) stackPlacement.getBoxItem();
				
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
				BoxItem boxItem = (BoxItem) stackPlacement.getBoxItem();
				
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

	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxes, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		Stack stack = new Stack();

		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		List<BoxItem> scopedBoxItems = boxes.stream().filter(s -> container.fitsInside(s.getBox())).collect(Collectors.toList());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(scopedBoxItems);

		ExtremePoints3D extremePoints3D = new ExtremePoints3D();
		extremePoints3D.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(filteredBoxItems.getMinArea(), filteredBoxItems.getMinVolume());

		BoxItemListener listener = compositeContainerItem.createBoxItemListener(container, stack, filteredBoxItems, extremePoints3D);

		int remainingLoadWeight = container.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && remainingLoadWeight > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				throw new PackagerInterruptedException();
			}
			
			IntermediatePlacementResult result = findBestPoint(filteredBoxItems, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
			if(result == null) {
				break;
			}

			Point point = result.getPoint();
			
			StackPlacement stackPlacement = new StackPlacement(null, result.getBoxItem(), result.getStackValue(), point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints3D.add(result.getPoint(), stackPlacement);

			result.getBoxItem().decrement();

			listener.accepted(result.getBoxItem());
			
			filteredBoxItems.removeEmpty();
	
			if(!filteredBoxItems.isEmpty()) {
				extremePoints3D.updateMinimums(result.getStackValue(), filteredBoxItems);
			}

			remainingLoadWeight -= result.getBoxItem().getBox().getWeight();
		}
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack);
	}

	public DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();
		
		List<BoxItemGroup> scopedBoxItemGroups = getFitsInside(boxItemGroups, container);

		DefaultFilteredBoxItemGroups filteredBoxItemGroups = new DefaultFilteredBoxItemGroups(scopedBoxItemGroups);

		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints3D = new MarkResetExtremePoints3D();
		extremePoints3D.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(filteredBoxItemGroups.getMinArea(), filteredBoxItemGroups.getMinVolume());

		BoxItemGroupListener listener = compositeContainerItem.createBoxItemGroupListener(container, stack, filteredBoxItemGroups, extremePoints3D);

		BoxItemGroupIterator boxItemGroupIterator = createBoxItemGroupIterator(filteredBoxItemGroups, itemGroupOrder, container, extremePoints3D);
		
		groups:
		while (!extremePoints3D.isEmpty() && !filteredBoxItemGroups.isEmpty() && boxItemGroupIterator.hasNext()) {
			int bestBoxItemGroupIndex = boxItemGroupIterator.next();

			BoxItemGroup boxItemGroup = filteredBoxItemGroups.remove(bestBoxItemGroupIndex);

			extremePoints3D.mark();
			
			int markStackSize = stack.size();
			
			while(!boxItemGroup.isEmpty()) {
				
				DefaultFilteredBoxItems items = new DefaultFilteredBoxItems(boxItemGroup.getItems()); 
				
				IntermediatePlacementResult bestPoint = findBestPoint(items, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderFactory(), container, stack);
				if(bestPoint == null) {
					if(itemGroupOrder == Order.FIXED) {
						break groups;
					}
					// discard the whole group
					extremePoints3D.reset();
					stack.setSize(markStackSize);
					
					continue groups;
				}
				
				bestPoint.getBoxItem().decrement();

				StackPlacement stackPlacement = new StackPlacement(boxItemGroup, bestPoint.getBoxItem(), bestPoint.getStackValue(), bestPoint.getPoint().getMinX(), bestPoint.getPoint().getMinY(), bestPoint.getPoint().getMinZ());
				stack.add(stackPlacement);
				extremePoints3D.add(bestPoint.getPoint(), stackPlacement);

				if(!boxItemGroup.isEmpty()) {
					extremePoints3D.updateMinimums(bestPoint.getStackValue(), filteredBoxItemGroups);
				}

			}
			
			if(container.getMaxLoadWeight() < extremePoints3D.getUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			listener.accepted(boxItemGroup);

		}
		
		return new DefaultIntermediatePackagerResult(containerItem, stack);
	}

	protected abstract BoxItemGroupIterator createBoxItemGroupIterator(
			FilteredBoxItemGroups filteredBoxItemGroups, 
			Order itemGroupOrder, 
			Container container,
			ExtremePoints extremePoints
		);

	protected abstract IntermediatePlacementResult findBestPoint(
			FilteredBoxItems items,
			ExtremePoints extremePoints, 
			FilteredPointsBuilderFactory filteredPointsBuilderFactory,
			Container container, 
			Stack stack
		);
}
