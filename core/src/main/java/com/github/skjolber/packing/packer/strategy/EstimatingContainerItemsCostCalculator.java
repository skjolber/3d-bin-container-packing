package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;

/**
 * Cheap, safe lower bound based on total weight and volume. Each dimension is
 * priced fractionally using the most cost-effective available capacity first.
 */
public class EstimatingContainerItemsCostCalculator extends AbstractContainerItemsCostCalculator {

	@Override
	public long getMinimumCost(ContainerItemsCalculator containers, List<BoxItem> boxes, int maxCount) {
		long volume = 0;
		long weight = 0;
		for(BoxItem item : boxes) {
			volume += item.getBox().getVolume() * item.getCount();
			weight += item.getBox().getWeight() * item.getCount();
		}
		if(boxes.isEmpty()) {
			return 0;
		}
		return getMinimumCost(containers, volume, weight, maxCount, costCapacities(containers));
	}

	@Override
	public long getGroupMinimumCost(ContainerItemsCalculator containers, List<BoxItemGroup> groups, int maxCount) {
		long volume = 0;
		long weight = 0;
		for(BoxItemGroup group : groups) {
			for(BoxItem item : group.getItems()) {
				volume += item.getBox().getVolume() * item.getCount();
				weight += item.getBox().getWeight() * item.getCount();
			}
		}
		if(groups.isEmpty()) {
			return 0;
		}
		return getMinimumCost(containers, volume, weight, maxCount, costCapacities(containers));
	}

	long getMinimumCost(ContainerItemsCalculator containers, long volume, long weight, int maxCount, List<CostCapacity> capacities) {
		if(volume == 0 && weight == 0) {
			return 0;
		}
		if(maxCount == 0 || !containers.hasMaxVolumeCapacity(maxCount, volume)
				|| !containers.hasMaxWeightCapacity(maxCount, weight)) {
			return Long.MAX_VALUE;
		}
		return Math.max(fractionalCost(capacities, volume, true), fractionalCost(capacities, weight, false));
	}

	private static long fractionalCost(List<CostCapacity> capacities, long target, boolean volume) {
		if(target == 0) {
			return 0;
		}
		List<CostCapacity> sorted = new ArrayList<>(capacities);
		sorted.sort((first, second) -> {
			long firstCapacity = volume ? first.volume : first.weight;
			long secondCapacity = volume ? second.volume : second.weight;
			if(firstCapacity == 0 || secondCapacity == 0) {
				return Long.compare(secondCapacity, firstCapacity);
			}
			return Double.compare((double)first.minimumCost / firstCapacity,
					(double)second.minimumCost / secondCapacity);
		});
		long remaining = target;
		double cost = 0;
		for(CostCapacity capacity : sorted) {
			long size = volume ? capacity.volume : capacity.weight;
			if(size == 0) {
				continue;
			}
			double available = (double)size * capacity.count;
			if(remaining <= available) {
				cost += (double)capacity.minimumCost * remaining / size;
				return (long)cost;
			}
			remaining -= (long)available;
			cost += (double)capacity.minimumCost * capacity.count;
		}
		return Long.MAX_VALUE;
	}
}
