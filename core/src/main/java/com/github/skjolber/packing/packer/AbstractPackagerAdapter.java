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
	protected final boolean containerCost;

	public AbstractPackagerAdapter(List<ControlledContainerItem> containers) {
		this.packagerContainerItems = new ContainerItemsCalculator(containers);
		this.initialContainerItems = packagerContainerItems.clone();
		this.containerCost = hasContainerCost(packagerContainerItems);
	}

	protected AbstractPackagerAdapter(AbstractPackagerAdapter source) {
		this.packagerContainerItems = source.packagerContainerItems.clone();
		this.initialContainerItems = source.initialContainerItems.clone();
		this.containerCost = source.containerCost;
	}

	@Override
	public PackagerAdapter fresh() {
		return fresh(initialContainerItems.clone().getContainerItems());
	}

	/** Create a new adapter using a calculator no longer used by another branch. */
	protected abstract AbstractPackagerAdapter fresh(List<ControlledContainerItem> containers);

	@Override
	public void reset() {
		packagerContainerItems.reset();
		resetState();
	}

	protected abstract void resetState();

	protected static List<BoxItem> copyBoxItems(List<BoxItem> items) {
		List<BoxItem> copies = new ArrayList<>(items.size());
		for(BoxItem item : items) {
			copies.add(new BoxItem(item.getBox().clone(), item.getCount(), item.getIndex()));
		}
		return copies;
	}

	protected static List<BoxItemGroup> copyBoxItemGroups(List<BoxItemGroup> groups) {
		List<BoxItemGroup> copies = new ArrayList<>(groups.size());
		for(BoxItemGroup group : groups) {
			copies.add(new BoxItemGroup(group.getId(), copyBoxItems(group.getItems()), group.getIndex()));
		}
		return copies;
	}

	@Override
	public boolean hasContainerCost() {
		return containerCost;
	}

	private static boolean hasContainerCost(ContainerItemsCalculator containers) {
		boolean anyCostCalculator = false;
		boolean allCostCalculators = true;
		for(ControlledContainerItem item : containers.getContainerItems()) {
			anyCostCalculator |= item.hasCostCalculator();
			allCostCalculators &= item.hasCostCalculator();
		}
		if(anyCostCalculator && !allCostCalculators) {
			throw new IllegalArgumentException("Expected either none or all containers to have a cost calculator");
		}
		return anyCostCalculator;
	}

	@Override
	public int getMaximumContainerCount(int requestedLimit) {
		int maximum = Math.min(requestedLimit, countRemainingBoxes());
		if(maximum <= 0) {
			return 0;
		}
		int available = 0;
		for(int i = 0; i < packagerContainerItems.getContainerItemCount(); i++) {
			int count = packagerContainerItems.getContainerItem(i).getCount();
			if(count >= maximum - available) {
				return maximum;
			}
			available += count;
		}
		return available;
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
	
}
