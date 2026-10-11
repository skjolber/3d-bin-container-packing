package com.github.skjolber.packing.packer.strategy.cost;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.packager.strategy.ContainerInventory;

/**
 * Cheap, safe lower bound based on total weight and volume. Each dimension is
 * priced fractionally using the most cost-effective available capacity first.
 */
public class EstimatingContainerItemsCostCalculator extends AbstractContainerItemsCostCalculator {

	/** Cheapest cost for each unit of volume first; capacities with no volume last. */
	private static final CostCapacityComparator VOLUME_COST_EFFICIENCY = (first, second) -> {
		if(first.volume == 0 || second.volume == 0) {
			return Long.compare(second.volume, first.volume);
		}
		return Double.compare((double)first.minimumCost / first.volume, (double)second.minimumCost / second.volume);
	};

	/** Cheapest cost for each unit of weight first; capacities with no weight last. */
	private static final CostCapacityComparator WEIGHT_COST_EFFICIENCY = (first, second) -> {
		if(first.weight == 0 || second.weight == 0) {
			return Long.compare(second.weight, first.weight);
		}
		return Double.compare((double)first.minimumCost / first.weight, (double)second.minimumCost / second.weight);
	};

	@Override
	public long getMinimumCost(ContainerInventory containers, List<BoxItem> boxes, int maxCount) {
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
	public long getGroupMinimumCost(ContainerInventory containers, List<BoxItemGroup> groups, int maxCount) {
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

	long getMinimumCost(ContainerInventory containers, long volume, long weight, int maxCount, List<CostCapacity> capacities) {
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
		CostCapacity[] sorted = sorted(capacities, volume ? VOLUME_COST_EFFICIENCY : WEIGHT_COST_EFFICIENCY);
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
