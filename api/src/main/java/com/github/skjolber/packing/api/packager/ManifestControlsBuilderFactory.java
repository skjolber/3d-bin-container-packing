package com.github.skjolber.packing.api.packager;


@FunctionalInterface
public interface ManifestControlsBuilderFactory {
	
	ManifestControlsBuilder<?> createBoxItemControlsBuilder();
}