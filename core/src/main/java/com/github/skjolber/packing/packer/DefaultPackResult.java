package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.PackResult;

public class DefaultPackResult implements PackResult {

	private final Stack stack;
	private final Container container;
	private final boolean last;
	private final int index;

	public DefaultPackResult(Container container, Stack stack, boolean last, int index) {
		this.stack = stack;
		this.container = container;
		this.last = last;
		this.index = index;
	}

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public int getSize() {
		return stack.getSize();
	}

	@Override
	public long getLoadVolume() {
		return stack.getVolume();
	}

	@Override
	public int getLoadWeight() {
		return stack.getWeight();
	}

	@Override
	public int getMaxLoadWeight() {
		return container.getMaxLoadWeight();
	}

	@Override
	public int getWeight() {
		return stack.getWeight() + container.getEmptyWeight();
	}

	@Override
	public long getVolume() {
		return container.getVolume();
	}

	@Override
	public long getMaxLoadVolume() {
		return container.getMaxLoadVolume();
	}

	@Override
	public boolean containsLastStackable() {
		return last;
	}

	@Override
	public Container getContainer() {
		return container;
	}

	public int getContainerItemIndex() {
		return index;
	}
}
