package com.github.skjolber.packing.api.validator.manifest;

@FunctionalInterface
public interface ManifestValidatorBuilderFactory {
	
	ManifestValidatorBuilder createManifestValidatorBuilder();

}