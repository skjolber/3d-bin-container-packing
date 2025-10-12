package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

/**
 * 
 * Packager intermediate result. This result type can be packager-specific and for internal comparison.
 * 
 */

public interface IntermediatePackagerResult {

	ContainerItem getContainerItem();
	
	Stack getStack();
	
	boolean isEmpty();
	
}
