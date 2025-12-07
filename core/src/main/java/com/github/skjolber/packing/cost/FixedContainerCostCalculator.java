package com.github.skjolber.packing.cost;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class FixedContainerCostCalculator implements ContainerCostCalculator {

	protected final long cost;
	protected final long volume;
	protected final String id;
	
	public FixedContainerCostCalculator(int cost, long volume, String id) {
		this.cost = cost;
		this.volume = volume;
		this.id = id;
	}

	@Override
	public long calculateCost(long weight) {
		return cost;
	}

	@Override
	public long getCostPerVolume(long weight) {
		return cost / volume;
	}

	@Override
	public long getCostPerWeight(long weight) {
		return cost / weight;
	}

	public String getId() {
		return id;
	}
}
