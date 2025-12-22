package com.github.skjolber.packing.api.cost;

public interface ContainerCostCalculator {

	long getCostPerVolume(long weight);
	long getCostPerWeight(long weight);
	
	long calculateCost(long weight);
	
	long getMinimumCost();
	long getMaximumCost();

	long getFixedCost();

	String getId();
}
