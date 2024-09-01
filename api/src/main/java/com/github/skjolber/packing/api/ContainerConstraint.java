package com.github.skjolber.packing.api;

import com.github.skjolber.packing.api.packager.LoadableItemFilterBuilder;

/**
 * 
 * Interface for handling which Stackables, or combinations of Stackables go into a Container.
 * 
 */

public interface ContainerConstraint {

	LoadableItemFilterBuilder<?> newLoadableFilterBuilder();

}
