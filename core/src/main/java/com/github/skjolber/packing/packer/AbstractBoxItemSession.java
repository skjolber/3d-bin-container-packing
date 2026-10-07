package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;

public abstract class AbstractBoxItemSession extends AbstractPackagerSession implements PackagerSession {

	protected List<RemainingBoxItem> remainingBoxItems;
	protected final List<BoxItem> initialBoxItems;
	protected final PackagerInterruptSupplier interrupt;
	protected final Order order;
	
	protected final boolean maxLoadWeight;
	protected final boolean maxLoadPressure;
	protected final boolean maxLoadBoxCount;
	protected final boolean maxLoadIdenticalBoxCount;

	public AbstractBoxItemSession(List<BoxItem> boxItems, Order order, List<ContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt) {
		this(toRemainingBoxItems(boxItems), boxItems, order, containers, containerCount, interrupt);
	}

	private AbstractBoxItemSession(List<RemainingBoxItem> remainingBoxItems, List<BoxItem> boxItems, Order order, List<ContainerItem> containers, int containerCount, PackagerInterruptSupplier interrupt) {
		super(remainingBoxItems, containers, containerCount);
		// the session packs the box items without modifying them (see AbstractPackager#createSession)
		this.initialBoxItems = boxItems;
		this.order = order;

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
		
		this.remainingBoxItems = remainingBoxItems;
		this.interrupt = interrupt;
	}

	protected AbstractBoxItemSession(AbstractBoxItemSession source) {
		super(source);
		this.initialBoxItems = source.initialBoxItems;
		this.remainingBoxItems = copyBoxItems(source.remainingBoxItems);
		this.interrupt = source.interrupt;
		this.order = source.order;
		this.maxLoadWeight = source.maxLoadWeight;
		this.maxLoadPressure = source.maxLoadPressure;
		this.maxLoadBoxCount = source.maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = source.maxLoadIdenticalBoxCount;
	}

	@Override
	public abstract PackagerSession fork();

	@Override
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			ContainerItem containerItem = packagerContainerItems.getContainerItem(index);
			return pack(remainingBoxItems, containerItem, interrupt, order, abortOnAnyBoxTooBig);
		} finally {
			for(RemainingBoxItem boxItem : remainingBoxItems) {
				boxItem.reset();
			}
		}				
	}

	@Override
	public ContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		Container container = packagerContainerItems.toContainer(resolveContainerItem(result), result.getStack());

		Stack stack = container.getStack();

		for (Placement stackPlacement : stack.getPlacements()) {
			RemainingBoxItem boxItem = findRemainingBoxItem(getGlobalIndex(stackPlacement));
			boxItem.decrementResetCount();
			boxItem.reset();
		}
		
		List<RemainingBoxItem> remainingBoxItems = new ArrayList<>(this.remainingBoxItems.size());
		for (RemainingBoxItem boxItem : this.remainingBoxItems) {
			if(!boxItem.isEmpty()) {
				remainingBoxItems.add(boxItem);
			}
		}
		this.remainingBoxItems = remainingBoxItems;

		return container;
	}

	private RemainingBoxItem findRemainingBoxItem(int globalIndex) {
		for(RemainingBoxItem boxItem : remainingBoxItems) {
			if(boxItem.getGlobalIndex() == globalIndex) {
				return boxItem;
			}
		}
		throw new IllegalArgumentException("Result contains unknown box item global index " + globalIndex);
	}

	@Override
	protected int getInputIndex(BoxItem boxItem) {
		return getInputIndex(initialBoxItems, boxItem);
	}

	@Override
	public List<Integer> getContainers() {
		return packagerContainerItems.getContainers(remainingBoxItems).getContainerIndexes();
	}

	@Override
	public List<RemainingBoxItem> getRemainingBoxItems() {
		return remainingBoxItems;
	}

	@Override
	public int countRemainingBoxes() {
		int count = 0;
		for(RemainingBoxItem boxItem : remainingBoxItems) {
			count += boxItem.getCount();
		}
		return count;
	}

	@Override
	public long getRemainingVolume() {
		long volume = 0L;
		for(RemainingBoxItem boxItem : remainingBoxItems) {
			volume = Math.addExact(volume, boxItem.getVolume());
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(RemainingBoxItem boxItem : remainingBoxItems) {
			weight = Math.addExact(weight, boxItem.getWeight());
		}
		return weight;
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return -1;
	}

	@Override
	public List<RemainingBoxItemGroup> getRemainingBoxItemGroups() {
		return null;
	}

	protected abstract IntermediatePackagerResult pack(
			List<RemainingBoxItem> remainingBoxItems, ContainerItem containerItem, PackagerInterruptSupplier interrupt, Order order, boolean abortOnAnyBoxTooBig
			) throws PackagerInterruptedException;

	

}
