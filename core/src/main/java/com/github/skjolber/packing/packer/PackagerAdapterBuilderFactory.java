package com.github.skjolber.packing.packer;

import java.io.Closeable;

/**
 * 
 * Interface to allow for more low-level access to the packager
 * 
 */

public interface PackagerAdapterBuilderFactory extends Closeable {
	
	PackagerAdapterBuilder newPackagerAdapterBuilder();

}
