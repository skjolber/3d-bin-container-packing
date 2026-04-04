package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

public abstract class AbstractBoxItemGroupAdapter extends AbstractPackagerAdapter implements PackagerAdapter {

	private List<BoxItemGroup> remainingBoxItemGroups;
	private final PackagerInterruptSupplier interrupt;
	private final Order order;

	public AbstractBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, ContainerItemLoadsCalculator packagerContainerItems, Order order, PackagerInterruptSupplier interrupt) {
		super(packagerContainerItems);
		
		List<BoxItemGroup> groupClones = new LinkedList<>();
		for (BoxItemGroup boxItemGroup : boxItemGroups) {
			BoxItemGroup clone = boxItemGroup.clone();
			clone.setIndex(groupClones.size());
			groupClones.add(clone);
			clone.mark();
		}

		this.remainingBoxItemGroups = groupClones;
		this.order = order;
		this.interrupt = interrupt;
	}

	@Override
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			return packGroup(remainingBoxItemGroups, order, containerItemsCalculator.getContainerItem(index), interrupt, abortOnAnyBoxTooBig);
		} finally {
			for(BoxItemGroup group : remainingBoxItemGroups) {
				group.reset();
			}
		}				
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		Container container = containerItemsCalculator.toContainer(result.getContainerItem(), result.getStack());

		Stack stack = container.getStack();

		for (Placement stackPlacement : stack.getPlacements()) {
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
	public ContainerItemLoadCalculations getContainers(int maxCount) {
		return containerItemsCalculator.getGroupContainers(remainingBoxItemGroups, maxCount);
	}
	
	@Override
	public int countRemainingBoxes() {
		int count = 0;
		for(BoxItemGroup group : remainingBoxItemGroups) {
			count += group.getBoxCount();
		}
		return count;
	}
	
	@Override
	public ControlledContainerItem getContainerItem(int index) {
		return containerItemsCalculator.getContainerItem(index);
	}

	protected abstract IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order, ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig);

}
