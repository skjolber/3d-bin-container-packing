package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Container;

public class ContainerAssert extends AbstractContainerAssert<ContainerAssert, Container> {

	public static ContainerAssert assertThat(Container actual) {
		return new ContainerAssert(actual);
	}

	public ContainerAssert(Container actual) {
		super(actual, ContainerAssert.class);
	}

}
