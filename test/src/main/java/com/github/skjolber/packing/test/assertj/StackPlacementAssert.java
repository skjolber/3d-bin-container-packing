package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Placement;

public class StackPlacementAssert extends AbstractStackPlacementAssert<StackPlacementAssert, Placement> {

	public static StackPlacementAssert assertThat(Placement actual) {
		return new StackPlacementAssert(actual);
	}

	public StackPlacementAssert(Placement actual) {
		super(actual, StackPlacementAssert.class);
	}

}
