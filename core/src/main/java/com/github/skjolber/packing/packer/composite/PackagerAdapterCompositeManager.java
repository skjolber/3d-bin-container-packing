package com.github.skjolber.packing.packer.composite;

import java.util.List;

import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;

/**
 * Interface for deciding which packager adapter to use. 
 */

public interface PackagerAdapterCompositeManager {

	/**
	 * Accept result based on the packager adapter index and the result. 
	 * 
	 * @param result
	 * @return true if accepted. If false, the composte packager should continue to the next packager adapter.
	 */
	
	boolean accept(int packagerAdapterIndex, IntermediatePackagerResult result);

	/**
	 * Get the packager adapters to use for the current packaging state.
	 *  
	 * @return list of packager adapters. The order of the list is the order in which the packager adapters will be used.
	 */
	
	List<PackagerAdapter> getAdapters();
}