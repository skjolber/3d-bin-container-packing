package com.github.skjolber.packing.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

class ContainerCostCalculatorTest {

	@Test
	void calculatesBucketCostAndRatiosWithoutIntegerTruncation() {
		BucketContainerCostCalculator calculator = BucketContainerCostCalculator.newBuilder()
				.withVolume(1_000)
				.withFixedCost(250)
				.withBucket(0, 1_000, 10_000)
				.withBucket(1_000, 2_000, 12_500)
				.build();

		assertEquals(10_250L, calculator.calculateCost(500));
		assertEquals(10.25d, calculator.getCostPerVolume(500));
		assertEquals(20.5d, calculator.getCostPerWeight(500));
		assertEquals(10_250L, calculator.getMinimumCost());
		assertEquals(12_750L, calculator.getMaximumCost());
		assertThrows(IllegalArgumentException.class, () -> calculator.calculateCost(2_000));
	}

	@Test
	void calculatesLinearBucketCost() {
		LinearBucketWeightContainerCostCalculator calculator = new LinearBucketWeightContainerCostCalculator(
				10_000, 0, 5_000, 500, 100, 2_500, "linear", 200);

		assertEquals(10_200L, calculator.calculateCost(0));
		assertEquals(10_700L, calculator.calculateCost(1));
		assertEquals(10_700L, calculator.calculateCost(100));
		assertEquals(11_200L, calculator.calculateCost(101));
		assertEquals(35_200L, calculator.calculateCost(5_000));
	}

	@Test
	void appliesVolumetricWeightFloor() {
		ContainerCostCalculator calculator = VolumeWeightBucketContainerCostCalculator.newBuilder()
				.withVolume(10_000)
				.withWeightToVolumeRatio(5.0d)
				.withBucket(0, 1_000, 15_000)
				.withBucket(1_000, 1_500, 10_000)
				.withBucket(1_500, 2_000, 11_000)
				.withBucket(2_000, 3_000, 12_000)
				.withBucket(3_000, 4_000, 13_000)
				.withBucket(4_000, 5_000, 14_000)
				.build();

		assertEquals(12_000L, calculator.getMinimumCost());
		assertEquals(14_000L, calculator.getMaximumCost());
		assertEquals(12_000L, calculator.calculateCost(1_250));
		assertEquals(12_000L, calculator.calculateCost(2_500));
		assertEquals(13_000L, calculator.calculateCost(3_500));
	}
}
