package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// 
public interface PackagerAdapter {

	IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;
	
	IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing);

	Container accept(IntermediatePackagerResult result);

	List<Integer> getContainers(int maxCount);
	
	ContainerItem getContainerItem(int index);
	
	int countRemainingBoxes();
}
