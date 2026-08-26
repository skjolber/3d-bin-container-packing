package com.github.skjolber.packing.cost;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class BucketContainerCostCalculator extends AbstractBucketWeightContainerCostCalculator {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private List<Bucket> buckets = new ArrayList<>();
		private long volume = -1L;
		private String id;
		
		// for non-shipping type fixed cost, i.e. for container + handling etc
		private long fixedCost;
		
		public Builder withBucket(int minWeight, int maxWeightExclusive, int cost) {
			buckets.add(new Bucket(minWeight, maxWeightExclusive, cost));
			return this;
		}
		
		public Builder withFixedCost(long fixedCost) {
			this.fixedCost = fixedCost;
			return this;
		}
		
		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withVolume(long volume) {
			this.volume = volume;
			return this;
		}
		
		public ContainerCostCalculator build() {
			if(volume == -1) {
				throw new IllegalStateException("Expected volume");
			}

			if(buckets.isEmpty()) {
				throw new IllegalStateException("Expected one or more buckets");
			}

			// weight dominates volume-weight, volume-weight can be ignored
			return new BucketContainerCostCalculator(buckets, volume, id, fixedCost);
		}
		
	}
	
	protected BucketContainerCostCalculator(List<Bucket> buckets, long volume, String id, long fixedCost) {
		super(buckets, volume, id, fixedCost);
	}
	
}
