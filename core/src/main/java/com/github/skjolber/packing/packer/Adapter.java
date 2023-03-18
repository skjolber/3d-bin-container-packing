package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.Stackable;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// 
public interface Adapter<T extends PackResult> {
	T attempt(int containerIndex, T best);

	Container accept(T result);
	
	List<Integer> getContainers(int maxCount);
}
