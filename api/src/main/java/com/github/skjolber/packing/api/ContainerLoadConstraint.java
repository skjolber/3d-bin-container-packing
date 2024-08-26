package com.github.skjolber.packing.api;

public interface ContainerLoadConstraint {

	boolean canLoad(Stackable stackable, Container container);

}
