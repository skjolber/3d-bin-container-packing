package com.github.skjolber.packing.cost;

import java.util.ArrayList;
import java.util.List;

public class BucketContainerCostCalculator extends AbstractBucketWeightContainerCostCalculator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private final List<Bucket> buckets = new ArrayList<>();
		private long volume = -1L;
		private String id;
		private long fixedCost;

		public Builder withBucket(long minWeight, long maxWeightExclusive, long cost) {
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

		public BucketContainerCostCalculator build() {
			if(volume <= 0) {
				throw new IllegalStateException("Expected positive volume");
			}
			if(buckets.isEmpty()) {
				throw new IllegalStateException("Expected one or more buckets");
			}
			return new BucketContainerCostCalculator(buckets, volume, id, fixedCost);
		}
	}

	public BucketContainerCostCalculator(List<Bucket> buckets, long volume, String id, long fixedCost) {
		super(buckets, volume, id, fixedCost);
	}
}
