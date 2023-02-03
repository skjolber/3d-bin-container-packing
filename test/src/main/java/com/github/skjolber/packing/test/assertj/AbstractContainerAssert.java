package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Stack;

public abstract class AbstractContainerAssert<SELF extends AbstractContainerAssert<SELF, ACTUAL>, ACTUAL extends Container>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractContainerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isStackedWithinContraints() {
		isNotNull();
		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			isStackValue(stack.getContainerStackValue());

			StackAssert.assertThat(actual.getStack()).isWithinLoadContraints();
		}
		return myself;
	}

	public SELF isStackedWithinLoadDimensions() {
		isNotNull();

		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			isStackValue(stack.getContainerStackValue());

			StackAssert.assertThat(actual.getStack()).isWithinLoadDimensions();
		}
		return myself;
	}

	public SELF isStackedWithinLoadWeight() {
		isNotNull();

		Stack stack = actual.getStack();
		if(stack == null) {
			failWithMessage("Expected stack");
		} else {
			isStackValue(stack.getContainerStackValue());

			StackAssert.assertThat(stack).isWithinLoadWeight();
		}

		return myself;
	}

	public SELF isStackValue(ContainerStackValue containerStackValue) {
		if(!hasStackValue(containerStackValue)) {
			failWithMessage("Expected stack to be stacked within the stack-values from the actual container, got " + containerStackValue);
		}
		return myself;
	}

	private boolean hasStackValue(ContainerStackValue containerStackValue) {
		for (ContainerStackValue stackValue : actual.getStackValues()) {
			if(stackValue.equals(containerStackValue)) {
				return true;
			}
		}
		return false;
	}

}
