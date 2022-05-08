package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.Stack;

public class DefaultPackResult implements PackResult {

	private Stack stack;
	private Container container;
	private boolean last;

	@Override
	public boolean isEmpty() {
		return stack.isEmpty();
	}

	@Override
	public int getCount() {
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
		return stack.getContainerStackValue().getMaxLoadWeight();
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
		return stack.getContainerStackValue().getMaxLoadVolume();
	}

	@Override
	public Stack getStack() {
		return stack;
	}

	@Override
	public boolean containsLastStackable() {
		return last;
	}

}
