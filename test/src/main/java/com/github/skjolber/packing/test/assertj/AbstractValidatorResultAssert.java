package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;

public abstract class AbstractValidatorResultAssert<SELF extends AbstractValidatorResultAssert<SELF, ACTUAL>, ACTUAL extends ValidatorResult>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractValidatorResultAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isValid() {
		isNotNull();
		if(!actual.isValid()) {
			failWithMessage("Expected valid result, got reasons %s", describe(actual.getReasons()));
		}
		return myself;
	}

	public SELF isNotValid() {
		isNotNull();
		if(actual.isValid()) {
			failWithMessage("Expected invalid result");
		}
		return myself;
	}

	/**
	 * @param expected expected reason types, in order
	 */
	@SafeVarargs
	public final SELF hasReasons(Class<? extends ValidatorResultReason>... expected) {
		isNotNull();
		List<ValidatorResultReason> reasons = actual.getReasons();
		if(reasons.size() != expected.length) {
			failWithMessage("Expected %s reason(s), got %s", expected.length, describe(reasons));
		}
		for(int i = 0; i < expected.length; i++) {
			if(!expected[i].isInstance(reasons.get(i))) {
				failWithMessage("Expected reason %s to be %s, got %s", i, expected[i].getSimpleName(), describe(List.of(reasons.get(i))));
			}
		}
		return myself;
	}

	/**
	 * @return reason types and messages, for failure messages
	 */
	public static String describe(List<ValidatorResultReason> reasons) {
		StringBuilder builder = new StringBuilder();
		builder.append('[');
		for(int i = 0; i < reasons.size(); i++) {
			if(i > 0) {
				builder.append(", ");
			}
			ValidatorResultReason reason = reasons.get(i);
			builder.append(reason.getClass().getSimpleName()).append(": ").append(reason.getMessage());
		}
		builder.append(']');
		return builder.toString();
	}
}
