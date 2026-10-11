package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.validator.ValidatorResult;

public class ValidatorResultAssert extends AbstractValidatorResultAssert<ValidatorResultAssert, ValidatorResult> {

	public static ValidatorResultAssert assertThat(ValidatorResult actual) {
		return new ValidatorResultAssert(actual);
	}

	public ValidatorResultAssert(ValidatorResult actual) {
		super(actual, ValidatorResultAssert.class);
	}

}
