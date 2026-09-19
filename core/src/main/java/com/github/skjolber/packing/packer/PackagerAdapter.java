package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;

/**
 * Logical packager for wrapping pre-processing / optimizations.
 */
// 
public interface PackagerAdapter {

	IntermediatePackagerResult attempt(int containerIndex, IntermediatePackagerResult best, boolean abortOnAnyBoxTooBig) throws PackagerInterruptedException;
	
	IntermediatePackagerResult peek(int containerIndex, IntermediatePackagerResult existing);

	Container accept(IntermediatePackagerResult result);

	List<Integer> getContainers(int maxCount);

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

	boolean hasContainerCost();

	long getRemainingVolume();

	long getRemainingWeight();

	/**
	 * Safe lower bound for packing the remaining boxes within {@code maxCount}
	 * containers. Adapters without a cost estimator may return zero.
	 */
	default long estimateMinimumCost(ContainerItemsCostCalculator calculator, int maxCount) {
		return 0;
	}
	
	ContainerItem getContainerItem(int index);
	
	int countRemainingBoxes();

	/** Maximum useful search depth, bounded by available containers and packing units. */
	int getMaximumContainerCount(int requestedLimit);
}
