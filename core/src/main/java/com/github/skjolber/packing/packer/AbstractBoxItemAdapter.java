package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

public abstract class AbstractBoxItemAdapter extends AbstractPackagerAdapter implements PackagerAdapter {

	protected List<BoxItem> remainingBoxItems;
	protected final List<BoxItem> initialBoxItems;
	protected final PackagerInterruptSupplier interrupt;
	protected final Order order;
	
	protected final boolean maxLoadWeight;
	protected final boolean maxLoadPressure;
	protected final boolean maxLoadBoxCount;
	protected final boolean maxLoadIdenticalBoxCount;

	public AbstractBoxItemAdapter(List<BoxItem> boxItems, Order order, List<ControlledContainerItem> containers, PackagerInterruptSupplier interrupt) {
		super(containers);
		this.initialBoxItems = copyBoxItems(boxItems);
		
		this.order = order;
		
		List<BoxItem> boxClones = new ArrayList<>(boxItems.size());
		for (BoxItem item : boxItems) {
			BoxItem clone = item.clone();
			clone.setIndex(boxClones.size());
			boxClones.add(clone);
		}
		
		boolean maxLoadWeight = false;
		boolean maxLoadPressure = false;
		boolean maxLoadBoxCount = false;
		boolean maxLoadIdenticalBoxCount = false;
		
		for (BoxItem item : boxItems) {
			if(item.isMaxLoad() || item.getBox().isLoadIdenticalBoxOnly()) {
				Box box = item.getBox();	
				maxLoadWeight |= box.isMaxLoadWeight();
				maxLoadPressure |= box.isMaxLoadPressure();
				maxLoadBoxCount |= box.isMaxLoadBoxCount();
				maxLoadIdenticalBoxCount |= box.isLoadIdenticalBoxOnly();
			}
		}
		
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = maxLoadIdenticalBoxCount;
		
		this.remainingBoxItems = boxClones;
		this.interrupt = interrupt;
	}

	protected AbstractBoxItemAdapter(AbstractBoxItemAdapter source) {
		super(source);
		this.initialBoxItems = copyBoxItems(source.initialBoxItems);
		this.remainingBoxItems = copyBoxItems(source.remainingBoxItems);
		this.interrupt = source.interrupt;
		this.order = source.order;
		this.maxLoadWeight = source.maxLoadWeight;
		this.maxLoadPressure = source.maxLoadPressure;
		this.maxLoadBoxCount = source.maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = source.maxLoadIdenticalBoxCount;
	}

	@Override
	protected void resetState() {
		remainingBoxItems = copyBoxItems(initialBoxItems);
		for(int i = 0; i < remainingBoxItems.size(); i++) {
			remainingBoxItems.get(i).setIndex(i);
		}
	}

	@Override
	public abstract PackagerAdapter fork();

	@Override
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			ControlledContainerItem containerItem = packagerContainerItems.getContainerItem(index);
			return pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig);
		} finally {
			for(BoxItem boxItem : remainingBoxItems) {
				boxItem.reset();
			}
		}				
	}
	
	@Override
	public ControlledContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
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
	public long estimateMinimumCost(ContainerItemsCostCalculator calculator, int maxCount) {
		return calculator.getMinimumCost(packagerContainerItems, remainingBoxItems, maxCount);
	}

	@Override
	public int countRemainingBoxes() {
		int count = 0;
		for(BoxItem boxItem : remainingBoxItems) {
			count += boxItem.getCount();
		}
		return count;
	}

	@Override
	public long getRemainingVolume() {
		long volume = 0L;
		for(BoxItem boxItem : remainingBoxItems) {
			volume = Math.addExact(volume, boxItem.getVolume());
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(BoxItem boxItem : remainingBoxItems) {
			weight = Math.addExact(weight, boxItem.getWeight());
		}
		return weight;
	}

	protected abstract IntermediatePackagerResult pack(
			List<BoxItem> remainingBoxItems, ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig
			) throws PackagerInterruptedException;

	

}
