package com.github.skjolber.packing.packer.composite;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerAdapterBuilderFactory;

/**
 * 
 */

public interface PackagerAdapterManager {

	boolean accept(IntermediatePackagerResult result);

	List<PackagerAdapter> getAdapters();
}