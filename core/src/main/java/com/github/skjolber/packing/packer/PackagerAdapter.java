package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// 
public interface PackagerAdapter<T extends IntermediatePackagerResult> {

	T attempt(int containerIndex, T best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;
	
	T peek(int containerIndex, T existing);

	Container accept(T result);

	List<Integer> getContainers(int maxCount);
	
	ContainerItem getContainerItem(int index);
	
	int countRemainingBoxes();
}
