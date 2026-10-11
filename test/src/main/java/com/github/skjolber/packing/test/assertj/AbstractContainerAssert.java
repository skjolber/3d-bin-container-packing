package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;

public abstract class AbstractContainerAssert<SELF extends AbstractContainerAssert<SELF, ACTUAL>, ACTUAL extends Container>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractContainerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isStackedWithinConstraints() {
		isNotNull();
		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			StackAssert.assertThat(actual.getStack()).isWithinLoadConstraints(actual);
		}
		return myself;
	}

	public SELF isStackedWithinLoadDimensions() {
		isNotNull();

		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			StackAssert.assertThat(actual.getStack()).isWithinLoadDimensions(actual);
		}
		return myself;
	}

	public SELF isStackedWithinLoadWeight() {
		isNotNull();

		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			StackAssert.assertThat(stack).isWithinLoadWeight(actual);
		}

		return myself;
	}

}
