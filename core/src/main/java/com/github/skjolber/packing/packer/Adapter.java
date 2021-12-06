package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// 
public interface Adapter<T extends PackResult> {
	T attempt(int containerIndex, T best);
	Container accept(T result);
}
