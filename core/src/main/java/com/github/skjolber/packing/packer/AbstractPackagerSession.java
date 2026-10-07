package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.strategy.PackagerSession;
import com.github.skjolber.packing.api.point.Point;

public abstract class AbstractPackagerSession implements PackagerSession {

	protected final ContainerItemsCalculator packagerContainerItems;
	protected final ContainerItemsCalculator initialContainerItems;

	public AbstractPackagerSession(List<BoxItem> boxItems, List<ContainerItem> containers, int containerCount) {
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

	public static List<BoxItem> copyBoxItems(List<BoxItem> items) {
		List<BoxItem> copies = new ArrayList<>(items.size());
		for(BoxItem item : items) {
			copies.add(new BoxItem(item.getBox().copy(), item.getCount(), item.getLocalIndex(), item.getGlobalIndex()).withOrderingOf(item));
		}
		return copies;
	}

	/** Assign stable identities once, before any iterator starts changing local indexes. */
	public static List<BoxItem> initializeGlobalIndexes(List<BoxItem> items) {
		int next = 0;
		int[] assigned = new int[items.size()];
		int assignedCount = 0;
		for(BoxItem item : items) {
			if(item.getGlobalIndex() != -1) {
				assigned[assignedCount++] = item.getGlobalIndex();
				next = Math.max(next, item.getGlobalIndex() + 1);
			}
		}
		if(assignedCount > 1) {
			Arrays.sort(assigned, 0, assignedCount);
			for(int i = 1; i < assignedCount; i++) {
				if(assigned[i] == assigned[i - 1]) {
					throw new IllegalArgumentException("Duplicate box item global index " + assigned[i]);
				}
			}
		}
		for(BoxItem item : items) {
			if(item.getGlobalIndex() == -1) {
				item.setGlobalIndex(next++);
			}
		}
		return items;
	}

	/** Assign stable identities to the input's box items, so that sessions for the same input agree on them. */
	public static void initializeGlobalIndexes(PackagerInput input) {
		if(input.hasBoxItems()) {
			initializeGlobalIndexes(input.getBoxItems());
		} else {
			initializeGlobalIndexesForGroups(input.getBoxItemGroups());
		}
	}

	public static List<BoxItemGroup> initializeGlobalIndexesForGroups(List<BoxItemGroup> groups) {
		List<BoxItem> items = new ArrayList<>();
		for(BoxItemGroup group : groups) {
			items.addAll(group.getItems());
		}
		initializeGlobalIndexes(items);
		return groups;
	}

	public static List<BoxItemGroup> copyBoxItemGroups(List<BoxItemGroup> groups) {
		List<BoxItemGroup> copies = new ArrayList<>(groups.size());
		for(BoxItemGroup group : groups) {
			copies.add(new BoxItemGroup(group.getId(), copyBoxItems(group.getItems()), group.getIndex()).withOrderingOf(group));
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
