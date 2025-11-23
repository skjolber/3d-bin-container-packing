package com.github.skjolber.packing.cost;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class VolumeWeightBucketContainerCostCalculator extends AbstractBucketWeightContainerCostCalculator {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private List<Bucket> buckets = new ArrayList<>();
		private double weightToVolumeRatio = -1d;
		private long volume = -1L;
		private String id;
		
		public Builder withId(String id) {
			this.id = id;
			return this;
		}
		
		public Builder withBucket(int minWeight, int maxWeightExclusive, int cost) {
			buckets.add(new Bucket(minWeight, maxWeightExclusive, cost));
			return this;
		}

		public Builder withVolumeToWeightRatio(double volumeToWeightRatio) {
			this.weightToVolumeRatio = volumeToWeightRatio;
			return this;
		}
		
		public Builder withVolume(long volume) {
			this.volume = volume;
			return this;
		}
		
		public ContainerCostCalculator build() {
			if(weightToVolumeRatio == -1d) {
				throw new IllegalStateException("Expected weight to volume ratio");
			}
			
			if(volume == -1) {
				throw new IllegalStateException("Expected volume");
			}

			if(buckets.isEmpty()) {
				throw new IllegalStateException("Expected one or more buckets");
			}

			for(int i = 0; i < buckets.size() - 1; i++) {
				Bucket currentBucket = buckets.get(i);
				Bucket nextBucket = buckets.get(i + 1);
				
				if(currentBucket.getMaxWeight() != nextBucket.getMinWeight()) {
					throw new IllegalStateException("Expected back to back buckets; bucket " + i + " max weight " + currentBucket.getMaxWeight() + " is not enough to reach bucket " + (i + 1) + " min weight " + nextBucket.getMinWeight());
				}
			}
			
			long minimumVolumeWeight = (long)(volume / weightToVolumeRatio);
			
			Bucket first = buckets.getFirst();
			Bucket last = buckets.getLast();
			
			if(minimumVolumeWeight <= first.getMinWeight()) {
				// weight dominates volume-weight, volume-weight can be ignored
				return new VolumeWeightBucketContainerCostCalculator(buckets, volume, id);
			} else if(minimumVolumeWeight >= last.getMaxWeight()) {
				// volume-weight dominates weight, weight can be ignored
				return new FixedContainerCostCalculator(last.getCost(), first.getMinWeight(), id);
			} else {
				// so the volume of the container is know, i.e. there is a minimum effective volume-weight.
				// all real weight below the effective volume-weight is "free", as in it does not affect the 
				// cost.

				Bucket limit = null;
				
				int index = 0;
				while(!buckets.get(index).holds(minimumVolumeWeight)) {
					index++;
				}
				
				limit = buckets.get(index);

				List<Bucket> correctedBuckets = new ArrayList<>();
				correctedBuckets.add(new Bucket(first.getMinWeight(), limit.getMaxWeight(), limit.getCost()));
				
				for(int i = index + 1; i < buckets.size(); i++) {
					correctedBuckets.add(buckets.get(i));
				}
				
				return new VolumeWeightBucketContainerCostCalculator(correctedBuckets, volume, id);
			}
		}
	}
	
	protected VolumeWeightBucketContainerCostCalculator(List<Bucket> buckets, long volume, String id) {
		super(buckets, volume, id);
	}
	
}
