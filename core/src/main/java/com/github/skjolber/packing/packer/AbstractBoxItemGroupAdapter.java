package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.ControlledContainerItem;
import com.github.skjolber.packing.api.packager.PackagerInterruptedException;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

public abstract class AbstractBoxItemGroupAdapter<T extends IntermediatePackagerResult> implements PackagerAdapter<T> {

	private List<BoxItemGroup> remainingBoxItemGroups;
	private final PackagerInterruptSupplier interrupt;
	private final BoxPriority priority;
	private final ContainerItemsCalculator packagerContainerItems;

	public AbstractBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, ContainerItemsCalculator packagerContainerItems, BoxPriority priority, PackagerInterruptSupplier interrupt) {
		this.packagerContainerItems = packagerContainerItems;
		
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
	public T attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			return packGroup(remainingBoxItemGroups, priority, packagerContainerItems.getContainerItem(index), interrupt, abortOnAnyBoxTooBig);
		} finally {
			for(BoxItemGroup group : remainingBoxItemGroups) {
				group.reset();
			}
		}				
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		Container container = packagerContainerItems.toContainer(result.getContainerItem(), result.getStack());

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
	public List<Integer> getContainers(int maxCount) {
		return packagerContainerItems.getGroupContainers(remainingBoxItemGroups, maxCount);
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
		return packagerContainerItems.getContainerItem(index);
	}

	protected abstract T packGroup(List<BoxItemGroup> remainingBoxItemGroups, BoxPriority priority, ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig);

}
