package com.github.skjolber.packing.comparator.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Placement controls validate load only for candidates which would be selected when the comparator
 * does not read the supported area; this must be reported correctly.
 */
public class PlacementComparatorSupportedAreaTest {

	@Test
	public void defaultPackagerComparatorDoesNotUseSupportedArea() {
		PlacementComparator comparator = DefaultPlacementComparatorFactory.newFactory()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerAreaIsBetter()
				.lowerZIsBetter()
				.build(List.of());

		assertThat(comparator.usesSupportedArea()).isFalse();
	}

	@Test
	public void supportComparatorUsesSupportedArea() {
		PlacementComparator comparator = DefaultPlacementComparatorFactory.newFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerAreaIsBetter()
				.lowerZIsBetter()
				.build(List.of());

		assertThat(comparator.usesSupportedArea()).isTrue();
	}

	@Test
	public void supportLaterInChainUsesSupportedArea() {
		PlacementComparator comparator = DefaultPlacementComparatorFactory.newFactory()
				.lowerZIsBetter()
				.higherSupportIsBetter()
				.build(List.of());

		assertThat(comparator.usesSupportedArea()).isTrue();
	}

	@Test
	public void unknownComparatorUsesSupportedArea() {
		PlacementComparator comparator = (a, b) -> 0;

		assertThat(comparator.usesSupportedArea()).isTrue();
	}
}
