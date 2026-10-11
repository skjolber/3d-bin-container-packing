package com.github.skjolber.packing.cost;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class FixedContainerCostCalculator implements ContainerCostCalculator {

	protected final long cost;
	protected final long volume;
	protected final long fixedCost;
	protected final long totalCost;
	protected final String id;

	public FixedContainerCostCalculator(long cost, long volume, String id, long fixedCost) {
		if(cost < 0) {
			throw new IllegalArgumentException("Cost must be non-negative");
		}
		if(volume <= 0) {
			throw new IllegalArgumentException("Volume must be positive");
		}
		if(fixedCost < 0) {
			throw new IllegalArgumentException("Fixed cost must be non-negative");
		}
		this.cost = cost;
		this.volume = volume;
		this.id = id;
		this.fixedCost = fixedCost;
		this.totalCost = Math.addExact(cost, fixedCost);
	}

	@Override
	public long calculateCost(long weight) {
		if(weight < 0) {
			throw new IllegalArgumentException("Weight must be non-negative");
		}
		return totalCost;
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
		return totalCost;
	}

	@Override
	public long getMinimumCost() {
		return totalCost;
	}
}
