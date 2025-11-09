package com.github.skjolber.packing.cost;

import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public abstract class AbstractBucketWeightContainerCostCalculator implements ContainerCostCalculator {

	protected static class Bucket {
		int cost;
		int minWeight;
		int maxWeight;
		
		/**
		 * 
		 * Weight bucket
		 * 
		 * @param minWeight minimum weight (inclusive)
		 * @param maxWeight minimum weight (exclusive)
		 * @param cost cost
		 */
		
		public Bucket(int minWeight, int maxWeight, int cost) {
			super();
			this.minWeight = minWeight;
			this.maxWeight = maxWeight;
			this.cost = cost;
		}

		public boolean is(long minimumVolumeWeight) {
			return minWeight <= minimumVolumeWeight && minimumVolumeWeight < maxWeight;
		}
	}

	protected final long minimumWeight;
	protected final long maximumWeight;
	
	protected final long minimumCost;
	protected final long maximumCost;

	protected final long volume;
	
	protected final List<Bucket> buckets;
	protected final String id;
	
	protected AbstractBucketWeightContainerCostCalculator(List<Bucket> buckets, long volume, String id) {
		this.buckets = buckets;
		
		this.minimumWeight = buckets.getFirst().minWeight;
		this.maximumWeight = buckets.getLast().maxWeight;
		
		this.minimumCost = buckets.getFirst().cost;
		this.maximumCost = buckets.getLast().cost;
		
		this.volume = volume;
		this.id = id;
	}

	public long getMinimumWeight() {
		return minimumWeight;
	}

	public long getMaximumWeight() {
		return maximumWeight;
	}

	public long getMinimumCost() {
		return minimumCost;
	}

	public long getMaximumCost() {
		return maximumCost;
	}

	@Override
	public long getCostPerVolume(long weight) {
		return calculateCost(weight) / volume;
	}

	@Override
	public long getCostPerWeight(long weight) {
		return calculateCost(weight) / weight;
	}

	@Override
	public long calculateCost(long weight) {
		if(weight > maximumWeight) {
			throw new IllegalArgumentException();
		}
		
		for(Bucket bucket : buckets) {
			if(bucket.is(weight)) {
				return bucket.cost;
			}
		}
		
		// should never happen
		throw new RuntimeException();
		
	}

	@Override
	public String getId() {
		return id;
	}
}
