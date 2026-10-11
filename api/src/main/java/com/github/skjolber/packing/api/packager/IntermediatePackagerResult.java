package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;

/**
 * 
 * Packager intermediate result. This result type can be packager-specific and for internal comparison.
 * 
 */

public interface IntermediatePackagerResult {

	ContainerItem getContainerItem();
	
	Stack getStack();
	
	boolean isEmpty();

	/**
	 * @return the total volume of the packed boxes
	 */
	default long getLoadVolume() {
		return getStack().getVolume();
	}

	/**
	 * @return the total weight of the packed boxes
	 */
	default long getLoadWeight() {
		return getStack().getWeight();
	}

	/**
	 * @return the number of packed boxes
	 */
	default int getBoxCount() {
		return getStack().size();
	}

}
