package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Stack;

public class StackAssert extends AbstractStackAssert<StackAssert, Stack> {

	public static StackAssert assertThat(Stack actual) {
		return new StackAssert(actual);
	}

	public StackAssert(Stack actual) {
		super(actual, StackAssert.class);
	}

}
