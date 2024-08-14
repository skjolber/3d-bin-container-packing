package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.StackPlacement;

public class StackPlacementAssert extends AbstractStackPlacementAssert<StackPlacementAssert, StackPlacement> {

	public static StackPlacementAssert assertThat(StackPlacement actual) {
		return new StackPlacementAssert(actual);
	}

	public StackPlacementAssert(StackPlacement actual) {
		super(actual, StackPlacementAssert.class);
	}

}
