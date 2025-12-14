package com.github.skjolber.packing.api.validator.manifest;

import java.util.List;

import com.github.skjolber.packing.api.Box;

/**
 * 
 * Validator for items.
 * 
 */

public interface ManifestValidator {

	boolean isValid(List<Box> boxes);
	
}
