package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.MutableBoxItem;
import com.github.skjolber.packing.packer.MutableBoxItemGroup;
import com.github.skjolber.packing.packer.PackagerAdapter;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractPlainPackager extends AbstractPackager<DefaultIntermediatePackagerResult, PlainPackagerResultBuilder> {

	public static class PlacementResult {
		
		public PlacementResult(BoxItem boxItem, int boxItemIndex, BoxStackValue stackValue, int pointIndex, Point point) {
			super();
			this.boxItem = boxItem;
			this.boxItemIndex = boxItemIndex;
			this.stackValue = stackValue;
			this.pointIndex = pointIndex;
			this.point = point;
		}
		protected BoxItem boxItem; 
		protected int boxItemIndex; 
		protected BoxStackValue stackValue;
		protected int pointIndex;
		protected Point point;
	}
	
	public AbstractPlainPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	protected class PlainBoxItemAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<MutableBoxItem> remainingBoxItems;
		private final PackagerInterruptSupplier interrupt;

		public PlainBoxItemAdapter(List<BoxItem> boxItems, List<CompositeContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<MutableBoxItem> boxClones = new LinkedList<>();
			for (BoxItem item : boxItems) {
				BoxItem clone = item.clone();
				MutableBoxItem mutableBoxItem = new MutableBoxItem(clone);
				mutableBoxItem.setIndex(boxClones.size());
				boxClones.add(mutableBoxItem);
			}
			this.remainingBoxItems = boxClones;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultIntermediatePackagerResult attempt(int index, DefaultIntermediatePackagerResult best) {
			try {
				return AbstractPlainPackager.this.pack(remainingBoxItems, containerItems.get(index), interrupt);
			} finally {
				for(MutableBoxItem boxItem : remainingBoxItems) {
					boxItem.reset();
				}
			}				
		}

		@Override
		public Container accept(DefaultIntermediatePackagerResult result) {
			Container container = super.toContainer(result.getContainerItem(), result.getStack());

			Stack stack = container.getStack();

			for (StackPlacement stackPlacement : stack.getPlacements()) {
				MutableBoxItem boxItem = (MutableBoxItem) stackPlacement.getBoxItem();
				
				boxItem.decrementResetCount();
				boxItem.reset();
			}
			
			List<MutableBoxItem> remainingBoxItems = new ArrayList<>(this.remainingBoxItems.size());
			for (MutableBoxItem boxItem : this.remainingBoxItems) {
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

	}
	
	protected class PlainBoxItemGroupAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<MutableBoxItemGroup> remainingBoxItemGroups;
		private final PackagerInterruptSupplier interrupt;
		private final Order itemGroupOrder;

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, List<CompositeContainerItem> containerItems, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
			super(containerItems);

			List<MutableBoxItemGroup> groupClones = new LinkedList<>();
			for (BoxItemGroup boxItemGroup : boxItemGroups) {
				BoxItemGroup clone = boxItemGroup.clone();
				MutableBoxItemGroup mutableBoxItem = new MutableBoxItemGroup(clone);
				mutableBoxItem.setIndex(groupClones.size());
				groupClones.add(mutableBoxItem);
			}

			this.remainingBoxItemGroups = groupClones;
			this.itemGroupOrder = itemGroupOrder;
			this.interrupt = interrupt;
		}

		@Override
		public DefaultIntermediatePackagerResult attempt(int index, DefaultIntermediatePackagerResult best) {
			try {
				return AbstractPlainPackager.this.pack(remainingBoxItemGroups, itemGroupOrder, containerItems.get(index), interrupt);
			} finally {
				for(MutableBoxItemGroup group : remainingBoxItemGroups) {
					group.reset();
				}
			}				
		}

		@Override
		public Container accept(DefaultIntermediatePackagerResult result) {
			Container container = super.toContainer(result.getContainerItem(), result.getStack());

			Stack stack = container.getStack();

			for (StackPlacement stackPlacement : stack.getPlacements()) {
				MutableBoxItem boxItem = (MutableBoxItem) stackPlacement.getBoxItem();
				
				boxItem.decrementResetCount();
				boxItem.reset();
			}

			List<MutableBoxItemGroup> remainingBoxItems = new ArrayList<>(this.remainingBoxItemGroups.size());
			for (MutableBoxItemGroup boxItem : this.remainingBoxItemGroups) {
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

	}

	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItem> boxItems, List<CompositeContainerItem> containers, PackagerInterruptSupplier interrupt) {
		return new PlainBoxItemAdapter(boxItems, containers, interrupt);
	}
	
	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
		return new PlainBoxItemGroupAdapter(boxes, containers, itemGroupOrder, interrupt);
	}

	protected ScheduledThreadPoolExecutor getScheduledThreadPoolExecutor() {
		return scheduledThreadPoolExecutor;
	}

	public abstract DefaultIntermediatePackagerResult pack(List<MutableBoxItem> boxes, CompositeContainerItem targetContainer, PackagerInterruptSupplier interrupt);

	public abstract DefaultIntermediatePackagerResult pack(List<MutableBoxItemGroup> boxes, Order itemGroupOrder, CompositeContainerItem targetContainer, PackagerInterruptSupplier interrupt);
	
}
