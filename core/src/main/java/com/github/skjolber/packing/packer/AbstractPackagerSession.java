package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.Point;

public abstract class AbstractPackagerSession implements PackagerSession {

	protected final ContainerItemsCalculator packagerContainerItems;
	protected final ContainerItemsCalculator initialContainerItems;

	public AbstractPackagerSession(List<RemainingBoxItem> boxItems, List<ContainerItem> containers, int containerCount) {
		this(new BoxItemsContainerItemsCalculator(containers, containerCount, boxItems));
	}

	public AbstractPackagerSession(List<ContainerItem> containers, int containerCount) {
		this(new ContainerItemsCalculator(containers, containerCount));
	}

	protected AbstractPackagerSession(ContainerItemsCalculator containerItemsCalculator) {
		this.packagerContainerItems = containerItemsCalculator;
		this.initialContainerItems = packagerContainerItems.copy();
	}

	protected AbstractPackagerSession(AbstractPackagerSession source) {
		this.packagerContainerItems = source.packagerContainerItems.copy();
		this.initialContainerItems = source.initialContainerItems.copy();
	}

	@Override
	public PackagerSession fresh() {
		ContainerItemsCalculator containers = initialContainerItems.copy();
		return fresh(containers.getContainerItems(), containers.getContainerCount());
	}

	/** Create a new session using a calculator no longer used by another branch. */
	protected abstract AbstractPackagerSession fresh(List<ContainerItem> containers, int containerCount);

	public static List<RemainingBoxItem> copyBoxItems(List<RemainingBoxItem> items) {
		List<RemainingBoxItem> copies = new ArrayList<>(items.size());
		for(RemainingBoxItem item : items) {
			copies.add(item.copy());
		}
		return copies;
	}

	/**
	 * Start a packaging operation: the box items with all their boxes remaining. The global index of a box item is its
	 * position in the input, so that sessions for the same input agree on them.
	 *
	 * @param items the box items (the input, which is not modified)
	 * @return the remaining box items, with global and local indexes
	 */
	public static List<RemainingBoxItem> toRemainingBoxItems(List<BoxItem> items) {
		List<RemainingBoxItem> remaining = new ArrayList<>(items.size());
		for(int i = 0; i < items.size(); i++) {
			BoxItem item = items.get(i);
			remaining.add(new RemainingBoxItem(item, item.getCount(), i, i));
		}
		return remaining;
	}

	/**
	 * Start a packaging operation: the groups with all their boxes remaining. The global index of a box item is its
	 * position in the input (the box items of all groups, in order). The groups have no index.
	 *
	 * @param groups the box item groups (the input, which is not modified)
	 * @return the remaining groups, with indexes
	 */
	public static List<RemainingBoxItemGroup> toRemainingBoxItemGroups(List<BoxItemGroup> groups) {
		List<RemainingBoxItemGroup> remaining = new ArrayList<>(groups.size());
		int globalIndex = 0;
		for(int i = 0; i < groups.size(); i++) {
			BoxItemGroup group = groups.get(i);
			List<RemainingBoxItem> items = new ArrayList<>(group.size());
			for(BoxItem item : group.getItems()) {
				items.add(new RemainingBoxItem(item, item.getCount(), -1, globalIndex++));
			}
			remaining.add(new RemainingBoxItemGroup(group, items));
		}
		return remaining;
	}

	/**
	 * Identify the box item of a placement within the packaging operation. A placement made outside of the operation
	 * refers to a box item as given to the packager (see {@link Placement#Placement(BoxItem, com.github.skjolber.packing.api.BoxStackValue, int, int, int, int)}):
	 * its global index is its position in the input.
	 *
	 * @param placement placement of a result
	 * @return the global index of the placement's box item, or -1 if unknown
	 */
	protected int getGlobalIndex(Placement placement) {
		RemainingBoxItem boxItem = placement.getRemainingBoxItem();
		if(boxItem.getGlobalIndex() != -1) {
			return boxItem.getGlobalIndex();
		}
		return getInputIndex(boxItem.getBoxItem());
	}

	/**
	 * @param boxItem a box item as given to the packager
	 * @return the position of the box item in the input (the box items of all groups, in order), or -1 if not found
	 */
	protected int getInputIndex(BoxItem boxItem) {
		return -1;
	}

	protected static int getInputIndex(List<BoxItem> items, BoxItem boxItem) {
		for(int i = 0; i < items.size(); i++) {
			if(items.get(i) == boxItem) {
				return i;
			}
		}
		return -1;
	}

	protected static int getGroupInputIndex(List<BoxItemGroup> groups, BoxItem boxItem) {
		int index = 0;
		for(BoxItemGroup group : groups) {
			for(BoxItem item : group.getItems()) {
				if(item == boxItem) {
					return index;
				}
				index++;
			}
		}
		return -1;
	}

	public static List<RemainingBoxItemGroup> copyBoxItemGroups(List<RemainingBoxItemGroup> groups) {
		List<RemainingBoxItemGroup> copies = new ArrayList<>(groups.size());
		for(RemainingBoxItemGroup group : groups) {
			copies.add(group.copy());
		}
		return copies;
	}

	@Override
	public ContainerItemsCalculator getContainerInventory() {
		return packagerContainerItems;
	}

	/** Resolve a result's container selection against this session's inventory. */
	protected ContainerItem resolveContainerItem(IntermediatePackagerResult result) {
		if(result == null || result.getContainerItem() == null) {
			throw new IllegalArgumentException("Missing container item");
		}
		int containerIndex = result.getContainerItem().getIndex();
		if(containerIndex < 0 || containerIndex >= packagerContainerItems.getContainerItemCount()) {
			throw new IllegalArgumentException("Unknown container item index " + containerIndex);
		}
		ContainerItem containerItem = packagerContainerItems.getContainerItem(containerIndex);
		if(!containerItem.isAvailable()) {
			throw new IllegalStateException("Container item index " + containerIndex + " is no longer available");
		}
		return containerItem;
	}

	@Override
	public IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult result) {

		Stack stack = result.getStack();
		ContainerItem peek = packagerContainerItems.getContainerItem(containerIndex);

		if(!peek.getContainer().fitsInside(stack)) {
			return null;
		}
		
		ContainerItem containerItem = result.getContainerItem();
		
		List<Point> initialPoints = peek.getInitialPoints();
		if(initialPoints != null && !initialPoints.isEmpty()) {
			if(!Objects.equals(containerItem.getInitialPoints(), initialPoints)) {
				return null;
			}
		}
		
		if(containerItem.getManifestControlsBuilderFactory() != null) {
			if(!Objects.equals(containerItem.getManifestControlsBuilderFactory(), peek.getManifestControlsBuilderFactory())) {
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
	
	protected abstract IntermediatePackagerResult copy(ContainerItem peek, IntermediatePackagerResult result, int index);

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
