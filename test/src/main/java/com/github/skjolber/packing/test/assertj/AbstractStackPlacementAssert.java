package com.github.skjolber.packing.test.assertj;

import java.util.Objects;


import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;

public abstract class AbstractStackPlacementAssert<SELF extends AbstractStackPlacementAssert<SELF, ACTUAL>, ACTUAL extends StackPlacement>
extends AbstractPlacement3DAssert<SELF, ACTUAL> {

	protected AbstractStackPlacementAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF hasStackableName(String name) {
		isNotNull();
		if (!Objects.equals(name, actual.getStackable().getDescription())) {
			failWithMessage("Expected stackable name " + name + ", not " + actual.getStackable().getDescription());
		}
		return myself;
	}

	public SELF hasStackValue(StackValue stackValue) {
		isNotNull();
		if (!Objects.equals(stackValue, actual.getStackValue())) {
			failWithMessage("Expected stack value " + stackValue + ", not " + actual.getStackValue());
		}
		return myself;
	}

}
