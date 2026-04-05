package com.github.skjolber.packing.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LinearBucketWeightContainerCostCalculatorTest {

	@Test
	public void test() {
		LinearBucketWeightContainerCostCalculator calculator = new LinearBucketWeightContainerCostCalculator(10000, 0, 5000, 500, 100, 2500, null, 0);
		
		assertEquals(10000 + 500, calculator.calculateCost(1));
		assertEquals(10000 + 500, calculator.calculateCost(31));
		assertEquals(10000 + 1000, calculator.calculateCost(101));
		assertEquals(10000 + 1000, calculator.calculateCost(131));
	}
}
