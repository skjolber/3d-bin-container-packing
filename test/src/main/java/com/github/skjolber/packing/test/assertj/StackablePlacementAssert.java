package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.StackPlacement;

public class StackablePlacementAssert extends AbstractStackPlacementAssert<StackablePlacementAssert, StackPlacement> {

	public static StackablePlacementAssert assertThat(StackPlacement actual) {
		return new StackablePlacementAssert(actual);
	}

	public StackablePlacementAssert(StackPlacement actual) {
		super(actual, StackablePlacementAssert.class);
	}

}
