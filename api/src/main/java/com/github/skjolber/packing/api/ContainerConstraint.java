package com.github.skjolber.packing.api;

import com.github.skjolber.packing.api.packager.ContainerLoadInputsFilterBuilder;

/**
 * 
 * Interface for handling which {@linkplain StackableItem}, or combinations of {@linkplain StackableItem} go into a Container.
 * 
 */

public interface ContainerConstraint {

	ContainerLoadInputsFilterBuilder<?> newStackableItemsFilterBuilder();

}
