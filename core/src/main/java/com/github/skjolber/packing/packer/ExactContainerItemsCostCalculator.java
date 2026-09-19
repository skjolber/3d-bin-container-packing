package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;

/**
 * Finds the least expensive assignment of whole boxes or groups to available
 * containers by weight, volume and individual fit. This does not establish
 * that the complete 3D placement inside each container is feasible.
 */
public class ExactContainerItemsCostCalculator extends AbstractContainerItemsCostCalculator {

	private static final class CostUnit {
		private final long volume;
		private final long weight;
		private final Predicate<Container> canLoad;
		private boolean[] fits;

		private CostUnit(long volume, long weight, Predicate<Container> canLoad) {
			this.volume = volume;
			this.weight = weight;
			this.canLoad = canLoad;
		}
	}

	private static final class OpenContainer {
		private final int type;
		private long remainingVolume;
		private long remainingWeight;

		private OpenContainer(int type, CostCapacity capacity) {
			this.type = type;
			this.remainingVolume = capacity.volume;
			this.remainingWeight = capacity.weight;
		}
	}

	private final EstimatingContainerItemsCostCalculator estimate = new EstimatingContainerItemsCostCalculator();

	@Override
	public long getMinimumCost(ContainerItemsCalculator containers, List<BoxItem> boxes, int maxCount) {
		List<CostUnit> units = new ArrayList<>();
		for(BoxItem item : boxes) {
			Box box = item.getBox();
			CostUnit unit = new CostUnit(box.getVolume(), box.getWeight(), container -> container.canLoad(box));
			for(int i = 0; i < item.getCount(); i++) {
				units.add(unit);
			}
		}
		return calculateMinimumCost(containers, units, maxCount);
	}

	@Override
	public long getGroupMinimumCost(ContainerItemsCalculator containers, List<BoxItemGroup> groups, int maxCount) {
		List<CostUnit> units = new ArrayList<>(groups.size());
		for(BoxItemGroup group : groups) {
			long volume = 0;
			long weight = 0;
			for(BoxItem item : group.getItems()) {
				volume += item.getBox().getVolume() * item.getCount();
				weight += item.getBox().getWeight() * item.getCount();
			}
			units.add(new CostUnit(volume, weight, container -> container.canLoad(group)));
		}
		return calculateMinimumCost(containers, units, maxCount);
	}

	private long calculateMinimumCost(ContainerItemsCalculator containers, List<CostUnit> units, int maxCount) {
		if(units.isEmpty()) {
			return 0;
		}
		List<CostCapacity> capacities = costCapacities(containers);
		long volume = 0;
		long weight = 0;
		for(CostUnit unit : units) {
			volume += unit.volume;
			weight += unit.weight;
		}
		if(estimate.getMinimumCost(containers, volume, weight, maxCount, capacities) == Long.MAX_VALUE) {
			return Long.MAX_VALUE;
		}
		units.sort(Comparator.comparingLong((CostUnit unit) -> unit.volume).reversed()
				.thenComparing(Comparator.comparingLong((CostUnit unit) -> unit.weight).reversed()));
		for(CostUnit unit : units) {
			if(unit.fits != null) {
				continue;
			}
			unit.fits = new boolean[capacities.size()];
			boolean anyFit = false;
			for(int i = 0; i < capacities.size(); i++) {
				CostCapacity capacity = capacities.get(i);
				unit.fits[i] = unit.volume <= capacity.volume && unit.weight <= capacity.weight
						&& unit.canLoad.test(capacity.container);
				anyFit |= unit.fits[i];
			}
			if(!anyFit) {
				return Long.MAX_VALUE;
			}
		}
		int[] available = new int[capacities.size()];
		for(int i = 0; i < available.length; i++) {
			available[i] = capacities.get(i).count;
		}
		return search(units, 0, capacities, available, new ArrayList<>(), maxCount, 0, Long.MAX_VALUE);
	}

	private static long search(List<CostUnit> units, int unitIndex, List<CostCapacity> capacities,
			int[] available, List<OpenContainer> opened, int maxCount, long cost, long best) {
		if(cost >= best) {
			return best;
		}
		if(unitIndex == units.size()) {
			long actualCost = 0;
			for(OpenContainer container : opened) {
				CostCapacity capacity = capacities.get(container.type);
				long price = capacity.calculator.calculateCost(capacity.weight - container.remainingWeight);
				if(price < 0) {
					throw new IllegalStateException("Negative container cost");
				}
				if(price >= best - actualCost) {
					return best;
				}
				actualCost += price;
			}
			return Math.min(actualCost, best);
		}
		CostUnit unit = units.get(unitIndex);
		for(int i = 0; i < opened.size(); i++) {
			OpenContainer container = opened.get(i);
			if(unit.volume > container.remainingVolume || unit.weight > container.remainingWeight
					|| !unit.fits[container.type]) {
				continue;
			}
			boolean equivalent = false;
			for(int j = 0; j < i; j++) {
				OpenContainer previous = opened.get(j);
				if(previous.type == container.type && previous.remainingVolume == container.remainingVolume
						&& previous.remainingWeight == container.remainingWeight) {
					equivalent = true;
					break;
				}
			}
			if(equivalent) {
				continue;
			}
			container.remainingVolume -= unit.volume;
			container.remainingWeight -= unit.weight;
			best = search(units, unitIndex + 1, capacities, available, opened, maxCount, cost, best);
			container.remainingVolume += unit.volume;
			container.remainingWeight += unit.weight;
		}
		if(opened.size() >= maxCount) {
			return best;
		}
		for(int i = 0; i < capacities.size(); i++) {
			CostCapacity capacity = capacities.get(i);
			if(available[i] == 0 || !unit.fits[i] || capacity.minimumCost >= best - cost) {
				continue;
			}
			available[i]--;
			OpenContainer container = new OpenContainer(i, capacity);
			container.remainingVolume -= unit.volume;
			container.remainingWeight -= unit.weight;
			opened.add(container);
			best = search(units, unitIndex + 1, capacities, available, opened, maxCount,
					cost + capacity.minimumCost, best);
			opened.remove(opened.size() - 1);
			available[i]++;
		}
		return best;
	}
}
