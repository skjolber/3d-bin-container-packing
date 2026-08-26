package com.github.skjolber.packing.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.cost.AbstractBucketWeightContainerCostCalculator.Bucket;

public class BucketContainerCostCalculatorTest {

	@Test
	public void testCalculate() {
		List<Bucket> buckets = new ArrayList<>();
		
		buckets.add(new Bucket(0, 1000, 10000));
		buckets.add(new Bucket(1000, 2000, 12500));
		buckets.add(new Bucket(2000, 3000, 15000));
		
		BucketContainerCostCalculator calculator = new BucketContainerCostCalculator(buckets, 1000, null, 0);
		
		assertEquals(10000, calculator.getMinimumCost());
		assertEquals(15000, calculator.getMaximumCost());
		
		assertEquals(10000, calculator.calculateCost(123));
		assertEquals(12500, calculator.calculateCost(1000));
		assertEquals(15000, calculator.calculateCost(3000 - 1));
		
		assertEquals(10, calculator.getCostPerVolume(100));
		assertEquals(100, calculator.getCostPerWeight(100));
	}
}
