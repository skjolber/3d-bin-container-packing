package com.github.skjolber.packing.api.validator.manifest;

import com.github.skjolber.packing.api.Container;

public interface ManifestValidatorBuilder {

	ManifestValidatorBuilder withContainer(Container container);
	
	ManifestValidator build();

}
