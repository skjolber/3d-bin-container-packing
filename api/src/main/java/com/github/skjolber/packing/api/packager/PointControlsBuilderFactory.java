package com.github.skjolber.packing.api.packager;


@FunctionalInterface
public interface PointControlsBuilderFactory {
	
	PointControlsBuilder<?> createPointControlsBuilder();
}