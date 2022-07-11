package com.github.skjolber.packing.test.assertj;

import com.github.skjolber.packing.api.Packager;

public class PackagerAssert extends AbstractPackagerAssert<PackagerAssert, Packager> {

	public static PackagerAssert assertThat(Packager actual) {
		return new PackagerAssert(actual);
	}

	public PackagerAssert(Packager actual) {
		super(actual, PackagerAssert.class);
	}

}
