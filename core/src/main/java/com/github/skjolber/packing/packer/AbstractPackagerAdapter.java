package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;

public abstract class AbstractPackagerAdapter implements PackagerAdapter {

	protected final ContainerItemsCalculator packagerContainerItems;
	protected final ContainerItemsCalculator initialContainerItems;

	public AbstractPackagerAdapter(List<BoxItem> boxItems, List<ControlledContainerItem> containers, int containerCount) {
		this(new BoxItemsContainerItemsCalculator(containers, containerCount, boxItems));
	}

	public AbstractPackagerAdapter(List<ControlledContainerItem> containers, int containerCount) {
		this(new ContainerItemsCalculator(containers, containerCount));
	}

	protected AbstractPackagerAdapter(ContainerItemsCalculator containerItemsCalculator) {
		this.packagerContainerItems = containerItemsCalculator;
		this.initialContainerItems = packagerContainerItems.clone();
	}

	protected AbstractPackagerAdapter(AbstractPackagerAdapter source) {
		this.packagerContainerItems = source.packagerContainerItems.clone();
		this.initialContainerItems = source.initialContainerItems.clone();
	}

	@Override
	public PackagerAdapter fresh() {
		ContainerItemsCalculator containers = initialContainerItems.clone();
		return fresh(containers.getContainerItems(), containers.getContainerCount());
	}

	/** Create a new adapter using a calculator no longer used by another branch. */
	protected abstract AbstractPackagerAdapter fresh(List<ControlledContainerItem> containers, int containerCount);

	@Override
	public void reset() {
		packagerContainerItems.reset();
		resetState();
	}

	protected abstract void resetState();

	protected static List<BoxItem> copyBoxItems(List<BoxItem> items) {
		List<BoxItem> copies = new ArrayList<>(items.size());
		for(BoxItem item : items) {
			copies.add(new BoxItem(item.getBox().clone(), item.getCount(), item.getLocalIndex(), item.getGlobalIndex()));
		}
		return copies;
	}

	/** Assign stable identities once, before any iterator starts changing local indexes. */
	protected static List<BoxItem> initializeGlobalIndexes(List<BoxItem> items) {
		int next = 0;
		for(BoxItem item : items) {
			if(item.getGlobalIndex() != -1) {
				next = Math.max(next, item.getGlobalIndex() + 1);
			}
		}
		for(BoxItem item : items) {
			if(item.getGlobalIndex() == -1) {
				item.setGlobalIndex(next++);
			}
		}
		return items;
	}

	protected static List<BoxItemGroup> initializeGlobalIndexesForGroups(List<BoxItemGroup> groups) {
		List<BoxItem> items = new ArrayList<>();
		for(BoxItemGroup group : groups) {
			items.addAll(group.getItems());
		}
		initializeGlobalIndexes(items);
		return groups;
	}

	protected static List<BoxItemGroup> copyBoxItemGroups(List<BoxItemGroup> groups) {
		List<BoxItemGroup> copies = new ArrayList<>(groups.size());
		for(BoxItemGroup group : groups) {
			copies.add(new BoxItemGroup(group.getId(), copyBoxItems(group.getItems()), group.getIndex()));
		}
		return copies;
	}

	@Override
	public ContainerItemsCalculator getContainerItemsCalculator() {
		return packagerContainerItems;
	}

	@Override
	public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult result) {

		Stack stack = result.getStack();
		ControlledContainerItem peek = packagerContainerItems.getContainerItem(containerIndex);

		if(!peek.getContainer().fitsInside(stack)) {
			return null;
		}
		
		ControlledContainerItem containerItem = result.getContainerItem();
		
		List<Point> initialPoints = peek.getInitialPoints();
		if(initialPoints != null && !initialPoints.isEmpty()) {
			if(!Objects.equals(containerItem.getInitialPoints(), initialPoints)) {
				return null;
			}
		}
		
		if(containerItem.getBoxItemControlsBuilderFactory() != null) {
			if(!Objects.equals(containerItem.getBoxItemControlsBuilderFactory(), peek.getBoxItemControlsBuilderFactory())) {
				return null;
			}
		}

		if(containerItem.getPointControlsBuilderFactory() != null) {
			if(!Objects.equals(containerItem.getPointControlsBuilderFactory(), peek.getPointControlsBuilderFactory())) {
				return null;
			}
		}
		
		
		return copy(peek, result, containerIndex);
	}
	
	protected abstract IntermediatePackagerResult copy(ControlledContainerItem peek, IntermediatePackagerResult result, int index);

	public int getMaxContainerCount() {
		int count = packagerContainerItems.getContainerCount();

		int groups = countRemainingBoxItemGroups();
		if(groups != -1) {
			count = Math.min(count, groups);
		} else {
			count = Math.min(count, countRemainingBoxes());
		}
		return count;
	}

}
