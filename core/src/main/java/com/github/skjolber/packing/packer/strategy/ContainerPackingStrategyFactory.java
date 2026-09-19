package com.github.skjolber.packing.packer.strategy;

/** Selects a container packing strategy for a packing operation. */
@FunctionalInterface
public interface ContainerPackingStrategyFactory {

	ContainerPackingStrategy create(boolean hasContainerCost);
}
