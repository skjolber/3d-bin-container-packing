package com.github.skjolber.packing.api.cost;

/**
 * Calculates the cost of using one fixed-size container for a given load
 * weight. Cost and weight units are application-defined, but must be used
 * consistently by the caller.
 */
public interface ContainerCostCalculator {

	/**
	 * @param weight load weight
	 * @return cost per unit of container load volume
	 */
	double getCostPerVolume(long weight);

	/**
	 * @param weight load weight
	 * @return cost per unit of load weight
	 */
	double getCostPerWeight(long weight);

	/**
	 * @param weight load weight
	 * @return total cost for the container
	 */
	long calculateCost(long weight);

	/**
	 * A lower bound on {@link #calculateCost(long)} for any supported load weight.
	 * Branch-and-bound container strategies rely on this value being no greater
	 * than the actual cost.
	 */
	long getMinimumCost();

	long getMaximumCost();

	long getFixedCost();

	String getId();
}
