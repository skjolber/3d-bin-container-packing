package com.github.skjolber.packing.cost;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class LinearBucketWeightContainerCostCalculator implements ContainerCostCalculator {

	protected final long minimumWeight;
	protected final long maximumWeight;
	protected final long minimumCost;
	protected final long cost;
	protected final long weight;
	protected final long volume;
	protected final String id;
	protected final long fixedCost;

	public LinearBucketWeightContainerCostCalculator(long minimumCost, long minimumWeight, long maximumWeight,
			long cost, long weight, long volume, String id, long fixedCost) {
		if(minimumCost < 0 || cost < 0 || fixedCost < 0) {
			throw new IllegalArgumentException("Costs must be non-negative");
		}
		if(minimumWeight < 0 || maximumWeight < minimumWeight) {
			throw new IllegalArgumentException("Invalid weight range");
		}
		if(weight <= 0) {
			throw new IllegalArgumentException("Weight step must be positive");
		}
		if(volume <= 0) {
			throw new IllegalArgumentException("Volume must be positive");
		}
		this.minimumCost = minimumCost;
		this.minimumWeight = minimumWeight;
		this.maximumWeight = maximumWeight;
		this.cost = cost;
		this.weight = weight;
		this.volume = volume;
		this.id = id;
		this.fixedCost = fixedCost;
	}

	@Override
	public long calculateCost(long weight) {
		if(weight < 0 || weight > maximumWeight) {
			throw new IllegalArgumentException("Weight " + weight + " is outside supported range 0-" + maximumWeight + " for calculator id=" + id);
		}
		if(weight <= minimumWeight) {
			return Math.addExact(minimumCost, fixedCost);
		}

		long extraWeight = weight - minimumWeight;
		long count = extraWeight / this.weight;
		if(extraWeight % this.weight != 0) {
			count++;
		}

		return Math.addExact(Math.addExact(minimumCost, Math.multiplyExact(cost, count)), fixedCost);
	}

	@Override
	public double getCostPerVolume(long weight) {
		return calculateCost(weight) / (double)volume;
	}

	@Override
	public double getCostPerWeight(long weight) {
		long calculatedCost = calculateCost(weight);
		if(weight == 0) {
			return calculatedCost == 0 ? 0.0d : Double.POSITIVE_INFINITY;
		}
		return calculatedCost / (double)weight;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public long getFixedCost() {
		return fixedCost;
	}

	@Override
	public long getMaximumCost() {
		return calculateCost(maximumWeight);
	}

	@Override
	public long getMinimumCost() {
		return Math.addExact(minimumCost, fixedCost);
	}
}
