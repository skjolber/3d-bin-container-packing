package com.github.skjolber.packing.cost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public abstract class AbstractBucketWeightContainerCostCalculator implements ContainerCostCalculator {

	public static class Bucket {

		private final long cost;
		private final long minWeight;
		private final long maxWeight;

		/**
		 * @param minWeight minimum load weight, inclusive
		 * @param maxWeight maximum load weight, exclusive
		 * @param cost variable cost for this bucket
		 */
		public Bucket(long minWeight, long maxWeight, long cost) {
			if(minWeight < 0) {
				throw new IllegalArgumentException("Minimum weight must be non-negative");
			}
			if(maxWeight <= minWeight) {
				throw new IllegalArgumentException("Maximum weight must be greater than minimum weight");
			}
			if(cost < 0) {
				throw new IllegalArgumentException("Cost must be non-negative");
			}
			this.minWeight = minWeight;
			this.maxWeight = maxWeight;
			this.cost = cost;
		}

		public boolean holds(long weight) {
			return minWeight <= weight && weight < maxWeight;
		}

		public long getCost() {
			return cost;
		}

		public long getMaxWeight() {
			return maxWeight;
		}

		public long getMinWeight() {
			return minWeight;
		}
	}

	protected final long minimumWeight;
	protected final long maximumWeight;

	protected final long minimumCost;
	protected final long maximumCost;

	protected final long volume;
	protected final List<Bucket> buckets;
	protected final String id;
	protected final long fixedCost;

	protected AbstractBucketWeightContainerCostCalculator(List<Bucket> buckets, long volume, String id, long fixedCost) {
		if(buckets == null || buckets.isEmpty()) {
			throw new IllegalArgumentException("Expected one or more buckets");
		}
		if(volume <= 0) {
			throw new IllegalArgumentException("Volume must be positive");
		}
		if(fixedCost < 0) {
			throw new IllegalArgumentException("Fixed cost must be non-negative");
		}

		List<Bucket> copy = new ArrayList<>(buckets);
		for(int i = 1; i < copy.size(); i++) {
			Bucket previous = copy.get(i - 1);
			Bucket current = copy.get(i);
			if(previous.maxWeight != current.minWeight) {
				throw new IllegalArgumentException("Expected contiguous, ascending buckets at index " + i);
			}
		}

		long minCost = Long.MAX_VALUE;
		long maxCost = Long.MIN_VALUE;
		for(Bucket bucket : copy) {
			minCost = Math.min(minCost, bucket.cost);
			maxCost = Math.max(maxCost, bucket.cost);
		}

		this.buckets = Collections.unmodifiableList(copy);
		this.minimumWeight = copy.get(0).minWeight;
		this.maximumWeight = copy.get(copy.size() - 1).maxWeight;
		this.minimumCost = Math.addExact(minCost, fixedCost);
		this.maximumCost = Math.addExact(maxCost, fixedCost);
		this.fixedCost = fixedCost;
		this.volume = volume;
		this.id = id;
	}

	public long getMinimumWeight() {
		return minimumWeight;
	}

	public long getMaximumWeight() {
		return maximumWeight;
	}

	@Override
	public long getMinimumCost() {
		return minimumCost;
	}

	@Override
	public long getMaximumCost() {
		return maximumCost;
	}

	@Override
	public double getCostPerVolume(long weight) {
		return calculateCost(weight) / (double)volume;
	}

	@Override
	public double getCostPerWeight(long weight) {
		long cost = calculateCost(weight);
		if(weight == 0) {
			return cost == 0 ? 0.0d : Double.POSITIVE_INFINITY;
		}
		return cost / (double)weight;
	}

	@Override
	public long getFixedCost() {
		return fixedCost;
	}

	@Override
	public long calculateCost(long weight) {
		if(weight < 0 || weight >= maximumWeight) {
			throw new IllegalArgumentException("Weight " + weight + " is outside supported range 0-" + maximumWeight + " for calculator id=" + id);
		}
		if(weight < minimumWeight) {
			return Math.addExact(buckets.get(0).cost, fixedCost);
		}

		for(Bucket bucket : buckets) {
			if(bucket.holds(weight)) {
				return Math.addExact(bucket.cost, fixedCost);
			}
		}

		throw new IllegalStateException("No cost bucket matched weight " + weight + " for calculator id=" + id);
	}

	@Override
	public String getId() {
		return id;
	}
}
