package com.github.skjolber.packing.virtualbox.bounds;

/** One-shot search of complete assemblies within the supplied container limits. */
public interface VirtualBoxBoundsSearch {
	
	/** Execute the search. The start time uses {@link System#nanoTime()}. */
	VirtualBoxBoundsResult pack(long start);
	
}
