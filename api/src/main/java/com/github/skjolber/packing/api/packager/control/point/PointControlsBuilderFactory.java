package com.github.skjolber.packing.api.packager.control.point;


@FunctionalInterface
public interface PointControlsBuilderFactory {
	
	PointControlsBuilder<?> createPointControlsBuilder();
}