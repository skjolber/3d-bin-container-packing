package com.github.skjolber.packing.cost;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class LinearBucketWeightContainerCostCalculator implements ContainerCostCalculator {

	protected final long minimumWeight;
	protected final long maximumWeight;
	protected final long minimumCost;
	
	// cost per weight
	protected final long cost;
	protected final long weight;
	protected final long volume;
	
	protected final String id;
	
	// for non-shipping type fixed cost, i.e. for container + handling etc
	protected final long fixedCost;
	
	public LinearBucketWeightContainerCostCalculator(int minimumCost, int minimumWeight, int maximumWeight, int cost, int weight, long volume, String id, long fixedCost) {
		super();
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
		if(weight > maximumWeight) {
			throw new IllegalArgumentException();
		}
		if(weight < minimumWeight) {
			return minimumCost + fixedCost;
		}
		
		long w = weight - minimumWeight;
		
		long count = w / this.weight;
		if(w % this.weight != 0) {
			count++;
		}
		
		return minimumCost + cost * count + fixedCost;
	}

	@Override
	public long getCostPerVolume(long weight) {
		return calculateCost(weight) / volume;
	}

	@Override
	public long getCostPerWeight(long weight) {
		return calculateCost(weight) / weight;
	}

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
		return minimumCost + fixedCost;
	}
}
