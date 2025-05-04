package com.github.skjolber.packing.api.packager;


@FunctionalInterface
public interface BoxItemControlsBuilderFactory {
	
	BoxItemControlsBuilder<?> createBoxItemControlsBuilder();
}