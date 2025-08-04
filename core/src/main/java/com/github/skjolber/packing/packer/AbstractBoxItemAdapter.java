package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;

public abstract class AbstractBoxItemAdapter<C extends ContainerItem, T extends IntermediatePackagerResult> implements PackagerAdapter<T> {

	protected List<BoxItem> remainingBoxItems;
	protected final PackagerInterruptSupplier interrupt;
	protected final BoxPriority priority;
	protected final AbstractContainerItemsCalculator<C> packagerContainerItems;

	public AbstractBoxItemAdapter(List<BoxItem> boxItems, BoxPriority priority, AbstractContainerItemsCalculator<C> packagerContainerItems, PackagerInterruptSupplier interrupt) {
		this.packagerContainerItems = packagerContainerItems;
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
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			return pack(remainingBoxItems, packagerContainerItems.getContainerItem(index), interrupt, priority, abortOnAnyBoxTooBig);
		} finally {
			for(BoxItem boxItem : remainingBoxItems) {
				boxItem.reset();
			}
		}				
	}
	
	@Override
	public C getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		Container container = packagerContainerItems.toContainer(result.getContainerItem(), result.getStack());

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
		return packagerContainerItems.getContainers(remainingBoxItems, maxCount);
	}

	@Override
	public int countRemainingBoxes() {
		int count = 0;
		for(BoxItem boxItem : remainingBoxItems) {
			count += boxItem.getCount();
		}
		return count;
	}

	protected abstract IntermediatePackagerResult pack(List<BoxItem> remainingBoxItems, C containerItem, PackagerInterruptSupplier interrupt, BoxPriority priority, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;


}