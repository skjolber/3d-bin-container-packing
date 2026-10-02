package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

/**
 * Logical packager for wrapping pre-processing / optimizations.
 */
// 
public interface PackagerAdapter {

	IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;
	
	IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing);

	/**
	 * Accept a result produced during the same packaging operation. The result
	 * may originate from a different packager adapter implementation.
	 */
	Container accept(IntermediatePackagerResult result);

	List<Integer> getContainers();

	/** Creates an independent adapter at the start of the same packaging operation. */
	PackagerAdapter fresh();

	/**
	 * Creates an independent adapter at the current packing state. The caller may
	 * accept a container on the fork without changing this adapter. Implementations
	 * without replaying previous container packings.
	 */
	PackagerAdapter fork();

	/** Restore this adapter to the start of the same packaging operation. */
	void reset();

	long getRemainingVolume();

	long getRemainingWeight();

	ContainerItemsCalculator getContainerItemsCalculator();

	List<BoxItem> getRemainingBoxItems();

	List<BoxItemGroup> getRemainingBoxItemGroups();
	
	ContainerItem getContainerItem(int index);
	
	int countRemainingBoxes();

	int countRemainingBoxItemGroups();

	int getMaxContainerCount();
}
