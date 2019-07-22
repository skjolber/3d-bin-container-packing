package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.Container;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
public interface Adapter {
	Container accepted(PackResult result);
	PackResult attempt(int containerIndex);
	boolean hasMore(PackResult result);
}
