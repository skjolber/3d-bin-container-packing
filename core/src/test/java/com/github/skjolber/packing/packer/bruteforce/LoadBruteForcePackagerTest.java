package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

class LoadBruteForcePackagerTest extends AbstractLoadBruteForcePackagerTest {

	@Override
	protected LoadBruteForcePackager createPackager() {
		return LoadBruteForcePackager.newBuilder().build();
	}

	@Test
	void usesThePointFilterWithLoadConstraints() {
		CountingPointFilter pointFilter = new CountingPointFilter();
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().withPointFilter(pointFilter).build()) {
			PackagerResultAssert.assertThat(packColumn(packager)).isSuccess();
		}
		assertThat(pointFilter.getCount()).isPositive();
	}

	@Test
	void builderKeepsSkipReversePermutations() {
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().withSkipReversePermutations(true).build()) {
			assertThat(packager.filterReversePermutations).isTrue();
		}
	}
}
