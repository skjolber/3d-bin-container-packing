package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.Container;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
public interface Adapter {
	Container accepted(PackResult result);
	PackResult attempt(int containerIndex);
	boolean hasMore(PackResult result);
}
