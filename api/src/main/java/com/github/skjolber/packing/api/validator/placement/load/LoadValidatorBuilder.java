package com.github.skjolber.packing.api.validator.placement.load;

import com.github.skjolber.packing.api.Container;

public interface LoadValidatorBuilder {
	
	LoadValidatorBuilder withContainer(Container container);
	
	LoadValidator build();
	
}
