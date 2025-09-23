package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public class EmptyIntermediatePackagerResult implements IntermediatePackagerResult {

	public static final EmptyIntermediatePackagerResult EMPTY = new EmptyIntermediatePackagerResult();
	
	@Override
	public ContainerItem getContainerItem() {
		throw new RuntimeException();
	}
	
	@Override
	public Stack getStack() {
		throw new RuntimeException();
	}

	@Override
	public boolean isEmpty() {
		return true;
	}


}
