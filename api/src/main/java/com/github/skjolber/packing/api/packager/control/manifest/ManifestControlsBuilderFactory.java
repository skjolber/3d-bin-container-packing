package com.github.skjolber.packing.api.packager.control.manifest;


@FunctionalInterface
public interface ManifestControlsBuilderFactory {
	
	ManifestControlsBuilder<?> createBoxItemControlsBuilder();
}