package com.github.skjolber.packing.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.cost.AbstractBucketWeightContainerCostCalculator.Bucket;

public class VolumeWeightBucketContainerCostCalculatorTest {

	@Test
	public void testCalculate() { // mostly testing the builder
		List<Bucket> buckets = new ArrayList<>();
		
		buckets.add(new Bucket(0, 1000, 10000));
		buckets.add(new Bucket(1000, 2000, 12500));
		buckets.add(new Bucket(2000, 3000, 15000));
		
		VolumeWeightBucketContainerCostCalculator calculator = (VolumeWeightBucketContainerCostCalculator) VolumeWeightBucketContainerCostCalculator.newBuilder()
				.withVolume(10000)
				.withVolumeToWeightRatio(5)
				.withBucket(0, 1000, 15000)
				.withBucket(1000, 1500, 10000)
				.withBucket(1500, 2000, 11000)
				.withBucket(2000, 3000, 12000)
				.withBucket(3000, 4000, 13000)
				.withBucket(4000, 5000, 14000)
				.build();
		
		assertEquals(12000, calculator.getMinimumCost()); // not 10000 because of volume weight
		assertEquals(14000, calculator.getMaximumCost());

		assertEquals(12000, calculator.calculateCost(1250));
		assertEquals(12000, calculator.calculateCost(1750));
		assertEquals(12000, calculator.calculateCost(2500));
		assertEquals(13000, calculator.calculateCost(3500));
		assertEquals(14000, calculator.calculateCost(4500));
	}
	
}
