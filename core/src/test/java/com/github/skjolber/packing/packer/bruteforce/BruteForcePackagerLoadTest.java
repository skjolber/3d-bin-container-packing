package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

class BruteForcePackagerLoadTest extends AbstractLoadBruteForcePackagerTest {

	@Override
	protected BruteForcePackager createPackager() {
		return BruteForcePackager.newBuilder().build();
	}

	@Test
	void usesThePointFilterWithLoadConstraints() {
		CountingPointFilter pointFilter = new CountingPointFilter();
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withPointFilter(pointFilter).build()) {
			PackagerResultAssert.assertThat(packColumn(packager)).isSuccess();
		}
		assertThat(pointFilter.getCount()).isPositive();
	}

}
