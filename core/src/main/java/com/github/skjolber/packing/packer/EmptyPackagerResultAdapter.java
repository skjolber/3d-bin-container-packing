package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public class EmptyPackagerResultAdapter implements IntermediatePackagerResult {

	public static final EmptyPackagerResultAdapter EMPTY = new EmptyPackagerResultAdapter();
	
	@Override
	public ContainerItem getContainerItem() {
		return null;
	}

	@Override
	public Stack getStack() {
		return null;
	}

	@Override
	public boolean containsLastStackable() {
		return false;
	}

	@Override
	public boolean isEmpty() {
		return true;
	}

}
