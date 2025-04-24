package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractSimplePackager extends AbstractPackager<DefaultIntermediatePackagerResult, DefaultPackagerResultBuilder> {

	public AbstractSimplePackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	protected class PlainBoxItemAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<BoxItem> remainingBoxItems;
		private final PackagerInterruptSupplier interrupt;

		public PlainBoxItemAdapter(List<BoxItem> boxItems, List<CompositeContainerItem> containerItems, PackagerInterruptSupplier interrupt) {
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
				return AbstractSimplePackager.this.pack(remainingBoxItems, containerItems.get(index), interrupt);
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
	
	protected class PlainBoxItemGroupAdapter extends AbstractPackagerAdapter<DefaultIntermediatePackagerResult> {

		private List<BoxItemGroup> remainingBoxItemGroups;
		private final PackagerInterruptSupplier interrupt;
		private final Order itemGroupOrder;

		public PlainBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, List<CompositeContainerItem> containerItems, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
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
				return AbstractSimplePackager.this.pack(remainingBoxItemGroups, itemGroupOrder, containerItems.get(index), interrupt);
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
		return new PlainBoxItemAdapter(boxItems, containers, interrupt);
	}
	
	@Override
	protected PackagerAdapter<DefaultIntermediatePackagerResult> adapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Order itemGroupOrder, PackagerInterruptSupplier interrupt) {
		return new PlainBoxItemGroupAdapter(boxes, containers, itemGroupOrder, interrupt);
	}

	@Override
	public DefaultPackagerResultBuilder newResultBuilder() {
		return new DefaultPackagerResultBuilder().withPackager(this);
	}
	
	public abstract DefaultIntermediatePackagerResult pack(List<BoxItem> boxes, CompositeContainerItem targetContainer, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;

	public abstract DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxes, Order itemGroupOrder, CompositeContainerItem targetContainer, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException;
	
}
