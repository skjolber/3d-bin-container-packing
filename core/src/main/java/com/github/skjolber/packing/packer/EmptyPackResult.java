package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResult;

public class EmptyPackResult implements PackResult {

	public static final EmptyPackResult EMPTY = new EmptyPackResult();

	@Override
	public boolean isEmpty() {
		return true;
	}

	@Override
	public int getSize() {
		return 0;
	}

	@Override
	public long getLoadVolume() {
		return 0;
	}

	@Override
	public int getLoadWeight() {
		return 0;
	}

	@Override
	public int getMaxLoadWeight() {
		return 0;
	}

	@Override
	public int getWeight() {
		return 0;
	}

	@Override
	public long getVolume() {
		return 0;
	}

	@Override
	public long getMaxLoadVolume() {
		return 0;
	}

	@Override
	public boolean containsLastStackable() {
		return false;
	}

	@Override
	public Container getContainer() {
		return null;
	}

	@Override
	public int getContainerItemIndex() {
		return -1;
	}

}
