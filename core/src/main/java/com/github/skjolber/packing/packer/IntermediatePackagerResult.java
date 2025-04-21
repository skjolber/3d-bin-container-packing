package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

public interface IntermediatePackagerResult {

	ContainerItem getContainerItem();
	
	Stack getStack();
	
	boolean isEmpty();
	
}
