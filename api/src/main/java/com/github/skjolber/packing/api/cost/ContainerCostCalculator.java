package com.github.skjolber.packing.api.cost;


/**
 * 
 * Price calculation for a single container (i.e. a single fixed size container, with variable weight).
 * 
 */

public interface ContainerCostCalculator {

	long getCostPerVolume(long weight);
	long getCostPerWeight(long weight);
	
	long calculateCost(long weight);
	
	long getMinimumCost();
	long getMaximumCost();

	long getFixedCost();

	String getId();
}
