package com.github.skjolber.packing.comparator.placement;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;

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
	public void unknownComparatorDoesNotDeclareTheSupportedArea() {
		PlacementComparator comparator = (a, b) -> 0;

		assertThat(comparator.usesSupportedArea()).isFalse();
	}

	@Test
	public void higherSupportComparatorPrefersHigherSupportedArea() {
		PlacementComparator comparator = DefaultPlacementComparatorFactory.newFactory()
				.higherSupportIsBetter()
				.higherVolumeIsBetter()
				.higherWeightIsBetter()
				.lowerAreaIsBetter()
				.lowerZIsBetter()
				.build(List.of());

		assertThat(comparator.prefersHigherSupportedArea()).isTrue();
	}

	@Test
	public void lowerSupportComparatorDoesNotPreferHigherSupportedArea() {
		PlacementComparator comparator = DefaultPlacementComparatorFactory.newFactory()
				.lowerZIsBetter()
				.lowerSupportIsBetter()
				.build(List.of());

		assertThat(comparator.prefersHigherSupportedArea()).isFalse();
	}

	@Test
	public void unknownComparatorDoesNotPreferHigherSupportedArea() {
		PlacementComparator comparator = (a, b) -> 0;

		assertThat(comparator.prefersHigherSupportedArea()).isFalse();
	}
}
