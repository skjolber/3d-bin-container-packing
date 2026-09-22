package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

public abstract class AbstractBoxItemGroupAdapter extends AbstractPackagerAdapter implements PackagerAdapter {

	protected List<BoxItemGroup> remainingBoxItemGroups;
	protected final List<BoxItemGroup> initialBoxItemGroups;
	protected final PackagerInterruptSupplier interrupt;
	protected final Order order;

	protected final boolean maxLoadWeight;
	protected final boolean maxLoadPressure;
	protected final boolean maxLoadBoxCount;
	protected final boolean maxLoadIdenticalBoxCount;
	
	public AbstractBoxItemGroupAdapter(List<BoxItemGroup> boxItemGroups, List<ControlledContainerItem> containers,
			int containerCount, Order order, PackagerInterruptSupplier interrupt) {
		super(new BoxItemGroupsContainerItemsCalculator(containers, containerCount, initializeGlobalIndexesForGroups(boxItemGroups)));
		this.initialBoxItemGroups = copyBoxItemGroups(boxItemGroups);
		
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
		
		boolean maxLoadWeight = false;
		boolean maxLoadPressure = false;
		boolean maxLoadBoxCount = false;
		boolean maxLoadIdenticalBoxCount = false;
		
		for (BoxItemGroup boxItemGroup : boxItemGroups) {
			for (BoxItem item : boxItemGroup.getItems()) {
				if(item.isMaxLoad() || item.getBox().isLoadIdenticalBoxOnly()) {
					Box box = item.getBox();	
					maxLoadWeight |= box.isMaxLoadWeight();
					maxLoadPressure |= box.isMaxLoadPressure();
					maxLoadBoxCount |= box.isMaxLoadBoxCount();
					maxLoadIdenticalBoxCount |= box.isLoadIdenticalBoxOnly();
				}
			}
		}
		
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = maxLoadIdenticalBoxCount;
		
	}

	protected AbstractBoxItemGroupAdapter(AbstractBoxItemGroupAdapter source) {
		super(source);
		this.initialBoxItemGroups = copyBoxItemGroups(source.initialBoxItemGroups);
		this.remainingBoxItemGroups = copyBoxItemGroups(source.remainingBoxItemGroups);
		for(BoxItemGroup group : remainingBoxItemGroups) {
			group.mark();
		}
		this.interrupt = source.interrupt;
		this.order = source.order;
		this.maxLoadWeight = source.maxLoadWeight;
		this.maxLoadPressure = source.maxLoadPressure;
		this.maxLoadBoxCount = source.maxLoadBoxCount;
		this.maxLoadIdenticalBoxCount = source.maxLoadIdenticalBoxCount;
	}

	@Override
	protected void resetState() {
		List<BoxItemGroup> groups = new LinkedList<>(copyBoxItemGroups(initialBoxItemGroups));
		for(int i = 0; i < groups.size(); i++) {
			groups.get(i).setIndex(i);
			groups.get(i).mark();
		}
		remainingBoxItemGroups = groups;
	}

	@Override
	public abstract PackagerAdapter fork();

	@Override
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			return packGroup(remainingBoxItemGroups, order, packagerContainerItems.getContainerItem(index), interrupt, abortOnAnyBoxTooBig);
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
	public List<Integer> getContainers() {
		return packagerContainerItems.getGroupContainers(remainingBoxItemGroups).getContainerIndexes();
	}

	@Override
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return remainingBoxItemGroups;
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
	public long getRemainingVolume() {
		long volume = 0L;
		for(BoxItemGroup group : remainingBoxItemGroups) {
			volume = Math.addExact(volume, group.getVolume());
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(BoxItemGroup group : remainingBoxItemGroups) {
			weight = Math.addExact(weight, group.getWeight());
		}
		return weight;
	}

	@Override
	public ControlledContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}


	@Override
	public List<BoxItem> getRemainingBoxItems() {
		return null;
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return remainingBoxItemGroups.size();
	}

	protected abstract IntermediatePackagerResult packGroup(List<BoxItemGroup> remainingBoxItemGroups, Order order, ControlledContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig);

}
