package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.PackagerResult;

public class PackagerResultAssert extends AbstractPackagerResultAssert<PackagerResultAssert, PackagerResult> {

	public static PackagerResultAssert assertThat(PackagerResult actual) {
		return new PackagerResultAssert(actual);
	}

	public PackagerResultAssert(PackagerResult actual) {
		super(actual, PackagerResultAssert.class);
	}

}
