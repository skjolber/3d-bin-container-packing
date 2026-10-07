package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
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

public abstract class AbstractBoxItemGroupSession extends AbstractPackagerSession implements PackagerSession {

	protected List<RemainingBoxItemGroup> remainingBoxItemGroups;
	protected final List<BoxItemGroup> initialBoxItemGroups;
	protected final PackagerInterruptSupplier interrupt;
	protected final Order order;

	protected final boolean maxLoadWeight;
	protected final boolean maxLoadPressure;
	protected final boolean maxLoadBoxCount;
	protected final boolean maxLoadIdenticalBoxCount;
	
	public AbstractBoxItemGroupSession(List<BoxItemGroup> boxItemGroups, List<ContainerItem> containers,
			int containerCount, Order order, PackagerInterruptSupplier interrupt) {
		this(toRemainingBoxItemGroups(boxItemGroups), boxItemGroups, containers, containerCount, order, interrupt);
	}

	private AbstractBoxItemGroupSession(List<RemainingBoxItemGroup> remainingBoxItemGroups, List<BoxItemGroup> boxItemGroups, List<ContainerItem> containers,
			int containerCount, Order order, PackagerInterruptSupplier interrupt) {
		super(new BoxItemGroupsContainerItemsCalculator(containers, containerCount, remainingBoxItemGroups));
		// the session packs the groups without modifying them (see AbstractPackager#createSession)
		this.initialBoxItemGroups = boxItemGroups;
		this.remainingBoxItemGroups = copyBoxItemGroups(remainingBoxItemGroups);
		for (int i = 0; i < this.remainingBoxItemGroups.size(); i++) {
			RemainingBoxItemGroup group = this.remainingBoxItemGroups.get(i);
			group.setIndex(i);
			group.mark();
		}
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

	protected AbstractBoxItemGroupSession(AbstractBoxItemGroupSession source) {
		super(source);
		this.initialBoxItemGroups = source.initialBoxItemGroups;
		this.remainingBoxItemGroups = copyBoxItemGroups(source.remainingBoxItemGroups);
		for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
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
	public abstract PackagerSession fork();

	@Override
	public IntermediatePackagerResult attempt(int index, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException {
		try {
			return packGroup(remainingBoxItemGroups, order, packagerContainerItems.getContainerItem(index), interrupt, abortOnAnyBoxTooBig);
		} finally {
			for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
				group.reset();
			}
		}				
	}

	@Override
	public Container accept(IntermediatePackagerResult result) {
		// check before changing anything, so that a rejected result leaves the session as it was
		checkWholeGroups(result.getStack());

		Container container = packagerContainerItems.toContainer(resolveContainerItem(result), result.getStack());

		Stack stack = container.getStack();

		for (Placement stackPlacement : stack.getPlacements()) {
			RemainingBoxItem boxItem = findRemainingBoxItem(getGlobalIndex(stackPlacement));
			boxItem.decrementResetCount();
			boxItem.reset();
		}

		List<RemainingBoxItemGroup> remainingBoxItems = new ArrayList<>(this.remainingBoxItemGroups.size());
		for (RemainingBoxItemGroup boxItem : this.remainingBoxItemGroups) {
			if(!boxItem.isEmpty()) {
				remainingBoxItems.add(boxItem);
			}
		}
		this.remainingBoxItemGroups = remainingBoxItems;

		return container;
	}

	/**
	 * Groups are packed whole: a result (also from another packager) must hold all boxes of each group it holds boxes
	 * of, and only boxes of the remaining groups.
	 */
	private void checkWholeGroups(Stack stack) {
		Map<Integer, Integer> countByGlobalIndex = new HashMap<>(stack.size() * 2);
		for (Placement placement : stack.getPlacements()) {
			countByGlobalIndex.merge(getGlobalIndex(placement), 1, Integer::sum);
		}
		int matched = 0;
		for (RemainingBoxItemGroup group : remainingBoxItemGroups) {
			boolean present = false;
			boolean complete = true;
			for (RemainingBoxItem boxItem : group.getItems()) {
				Integer count = countByGlobalIndex.get(boxItem.getGlobalIndex());
				if(count != null) {
					present = true;
					matched++;
				}
				if(count == null || count != boxItem.getCount()) {
					complete = false;
				}
			}
			if(present && !complete) {
				throw new IllegalArgumentException("Result does not contain complete box item group " + group.getId());
			}
		}
		if(matched != countByGlobalIndex.size()) {
			throw new IllegalArgumentException("Result contains box items outside the remaining box item groups");
		}
	}

	private RemainingBoxItem findRemainingBoxItem(int globalIndex) {
		for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
			for(RemainingBoxItem boxItem : group.getItems()) {
				if(boxItem.getGlobalIndex() == globalIndex) {
					return boxItem;
				}
			}
		}
		throw new IllegalArgumentException("Result contains unknown box item global index " + globalIndex);
	}

	@Override
	protected int getInputIndex(BoxItem boxItem) {
		return getGroupInputIndex(initialBoxItemGroups, boxItem);
	}

	@Override
	public List<Integer> getContainers() {
		return packagerContainerItems.getGroupContainers(remainingBoxItemGroups).getContainerIndexes();
	}

	@Override
	public List<RemainingBoxItemGroup> getRemainingBoxItemGroups() {
		return remainingBoxItemGroups;
	}

	@Override
	public int countRemainingBoxes() {
		int count = 0;
		for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
			count += group.getBoxCount();
		}
		return count;
	}

	@Override
	public long getRemainingVolume() {
		long volume = 0L;
		for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
			volume = Math.addExact(volume, group.getVolume());
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(RemainingBoxItemGroup group : remainingBoxItemGroups) {
			weight = Math.addExact(weight, group.getWeight());
		}
		return weight;
	}

	@Override
	public ContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}


	@Override
	public List<RemainingBoxItem> getRemainingBoxItems() {
		return null;
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return remainingBoxItemGroups.size();
	}

	protected abstract IntermediatePackagerResult packGroup(List<RemainingBoxItemGroup> remainingBoxItemGroups, Order order, ContainerItem containerItem, PackagerInterruptSupplier interrupt, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;

}
