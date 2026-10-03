package com.github.skjolber.packing.test.assertj;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.PackagerResult;

public abstract class AbstractPackagerResultAssert<SELF extends AbstractPackagerResultAssert<SELF, ACTUAL>, ACTUAL extends PackagerResult>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	protected AbstractPackagerResultAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public SELF isSuccess() {
		isNotNull();
		if(!actual.isSuccess()) {
			failWithMessage("Expected success, got " + actual.size() + " container(s)");
		}
		return myself;
	}

	public SELF isNotSuccess() {
		isNotNull();
		if(actual.isSuccess()) {
			failWithMessage("Expected no success, got " + actual.size() + " container(s)");
		}
		return myself;
	}

	public SELF hasContainerCount(int count) {
		isNotNull();
		if(actual.size() != count) {
			failWithMessage("Expected " + count + " container(s), got " + actual.size());
		}
		return myself;
	}

	/**
	 * @param containerIndex index of the container in the result
	 * @param count expected number of placed boxes
	 */
	public SELF hasStackSize(int containerIndex, int count) {
		isNotNull();
		if(containerIndex >= actual.size()) {
			failWithMessage("Expected container at index " + containerIndex + ", got " + actual.size() + " container(s)");
		} else {
			int size = actual.get(containerIndex).getStack().size();
			if(size != count) {
				failWithMessage("Expected " + count + " box(es) in container " + containerIndex + ", got " + size);
			}
		}
		return myself;
	}
}
