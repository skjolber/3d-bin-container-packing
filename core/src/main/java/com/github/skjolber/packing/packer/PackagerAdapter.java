package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.PackResult;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// 
public interface PackagerAdapter<T extends IntermediatePackagerResult> {

	T attempt(int containerIndex, T best);

	Container accept(T result);

	List<Integer> getContainers(int maxCount);
	
	ContainerItem getContainerItem(int index);
}
