package com.github.skjolberg.packing.impl;

import com.github.skjolberg.packing.BoxItem;
import com.github.skjolberg.packing.Container;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Logical packager for wrapping preprocessing / optimizations.
 */
public interface Adapter {
	Container accepted(PackResult result);
	PackResult attempt(int containerIndex);
	boolean hasMore(PackResult result);
}
