package com.github.skjolber.packing.api.packager;

@FunctionalInterface
public interface BoxItemGroupControlsBuilderFactory {
	
	BoxItemGroupControlsBuilder<?> createBoxItemGroupControlsBuilder();
	
}
