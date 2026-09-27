package com.github.skjolber.packing.boundingbox;

/** One-shot search of complete assemblies within the supplied container limits. */
public interface BruteForceBoundingBoxSearch {
	/** Execute the search. The start time uses {@link System#nanoTime()}. */
	BruteForceBoundingBoxResult pack(long start);
}
