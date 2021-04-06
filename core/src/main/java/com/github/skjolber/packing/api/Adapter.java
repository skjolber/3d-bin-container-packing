package com.github.skjolber.packing.api;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
public interface Adapter {
	Container accepted(PackResult result);
	PackResult attempt(int containerIndex);
	boolean hasMore(PackResult result);
}
