package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Container;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
// TODO rename
public interface Adapter {
	Container accepted(PackResult result);
	PackResult attempt(int containerIndex);
	boolean hasMore(PackResult result);
}
