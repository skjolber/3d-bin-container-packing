package com.github.skjolber.packing.packer;

import java.util.List;
import java.util.Objects;

import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.point.Point;

public abstract class AbstractPackagerAdapter implements PackagerAdapter {

	protected final ContainerItemsCalculator packagerContainerItems;

	public AbstractPackagerAdapter(ContainerItemsCalculator packagerContainerItems) {
		this.packagerContainerItems = packagerContainerItems;
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
