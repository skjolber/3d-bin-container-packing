package com.github.skjolber.packing.validator;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.manifest.ManifestValidator;

public class DefaultManifestValidator implements ManifestValidator {

	private Container container;
	
	public DefaultManifestValidator(Container container) {
		super();
		this.container = container;
	}
	
	public Container getContainer() {
		return container;
	}

	@Override
	public boolean isValid(List<Box> boxes, List<ValidatorResultReason> reasons) {
		return true;
	}

}
