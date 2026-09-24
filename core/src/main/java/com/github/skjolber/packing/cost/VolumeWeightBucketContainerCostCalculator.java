package com.github.skjolber.packing.cost;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class VolumeWeightBucketContainerCostCalculator extends AbstractBucketWeightContainerCostCalculator {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		private final List<Bucket> buckets = new ArrayList<>();
		private double weightToVolumeRatio = -1.0d;
		private long volume = -1L;
		private String id;
		private long fixedCost;

		public Builder withFixedCost(long fixedCost) {
			this.fixedCost = fixedCost;
			return this;
		}

		public Builder withId(String id) {
			this.id = id;
			return this;
		}

		public Builder withBucket(long minWeight, long maxWeightExclusive, long cost) {
			buckets.add(new Bucket(minWeight, maxWeightExclusive, cost));
			return this;
		}

		public Builder withWeightToVolumeRatio(double weightToVolumeRatio) {
			this.weightToVolumeRatio = weightToVolumeRatio;
			return this;
		}

		public Builder withVolume(long volume) {
			this.volume = volume;
			return this;
		}

		public ContainerCostCalculator build() {
			if(!Double.isFinite(weightToVolumeRatio) || weightToVolumeRatio <= 0.0d) {
				throw new IllegalStateException("Expected positive weight-to-volume ratio");
			}
			if(volume <= 0) {
				throw new IllegalStateException("Expected positive volume");
			}
			if(buckets.isEmpty()) {
				throw new IllegalStateException("Expected one or more buckets");
			}

			for(int i = 1; i < buckets.size(); i++) {
				Bucket previous = buckets.get(i - 1);
				Bucket current = buckets.get(i);
				if(previous.getMaxWeight() != current.getMinWeight()) {
					throw new IllegalStateException("Expected contiguous, ascending buckets at index " + i);
				}
			}

			long minimumVolumeWeight = (long)Math.ceil(volume / weightToVolumeRatio);
			Bucket first = buckets.get(0);
			Bucket last = buckets.get(buckets.size() - 1);

			if(minimumVolumeWeight <= first.getMinWeight()) {
				return new VolumeWeightBucketContainerCostCalculator(buckets, volume, id, fixedCost);
			}
			if(minimumVolumeWeight >= last.getMaxWeight()) {
				return new FixedContainerCostCalculator(last.getCost(), volume, id, fixedCost);
			}

			int index = 0;
			while(!buckets.get(index).holds(minimumVolumeWeight)) {
				index++;
			}

			Bucket limit = buckets.get(index);
			List<Bucket> correctedBuckets = new ArrayList<>(buckets.size() - index);
			correctedBuckets.add(new Bucket(first.getMinWeight(), limit.getMaxWeight(), limit.getCost()));
			for(int i = index + 1; i < buckets.size(); i++) {
				correctedBuckets.add(buckets.get(i));
			}

			return new VolumeWeightBucketContainerCostCalculator(correctedBuckets, volume, id, fixedCost);
		}
	}

	protected VolumeWeightBucketContainerCostCalculator(List<Bucket> buckets, long volume, String id, long fixedCost) {
		super(buckets, volume, id, fixedCost);
	}
}
