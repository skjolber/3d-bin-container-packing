package com.github.skjolber.packing.packer.strategy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Assigns individual boxes, or indivisible box-item groups, to the remaining
 * container inventory. The assignment observes individual fit, volume, weight,
 * inventory and the remaining container limit. It deliberately does not claim
 * that the assigned items have a feasible 3D placement.
 */
final class ContainerAllocationPlanner {

	enum Objective {
		FEASIBLE,
		FEWEST_CONTAINERS,
		LOWEST_COST
	}

	static final class Allocation {
		private final int[] containerIndexes;
		private final long cost;

		private Allocation(int[] containerIndexes, long cost) {
			this.containerIndexes = containerIndexes;
			this.cost = cost;
		}

		int getContainerCount() {
			return containerIndexes.length;
		}

		int getContainerIndex(int index) {
			return containerIndexes[index];
		}

		long getCost() {
			return cost;
		}
	}

	private static final class Unit {
		private final long volume;
		private final long weight;
		private final BoxItem boxItem;
		private final BoxItemGroup group;
		private boolean[] fits;
		private int fitCount;

		private Unit(BoxItem boxItem) {
			this.volume = boxItem.getBox().getVolume();
			this.weight = boxItem.getBox().getWeight();
			this.boxItem = boxItem;
			this.group = null;
		}

		private Unit(BoxItemGroup group) {
			this.volume = group.getVolume();
			this.weight = group.getWeight();
			this.boxItem = null;
			this.group = group;
		}

		private boolean canLoad(ContainerItemsCalculator calculator, int containerItemIndex) {
			if(boxItem != null) {
				return calculator.canLoad(boxItem, containerItemIndex);
			}
			return calculator.canLoad(group, containerItemIndex);
		}
	}

	private static final class Capacity {
		private final int containerIndex;
		private final int count;
		private final long volume;
		private final long weight;
		private final ContainerCostCalculator costCalculator;
		private final long minimumCost;

		private Capacity(int containerIndex, ControlledContainerItem item, int count, Objective objective) {
			this.containerIndex = containerIndex;
			this.count = count;
			this.volume = item.getContainer().getMaxLoadVolume();
			this.weight = item.getContainer().getMaxLoadWeight();
			this.costCalculator = item.getCostCalculator();
			if(objective == Objective.LOWEST_COST) {
				if(costCalculator == null) {
					throw new IllegalStateException("Missing cost calculator for container index " + containerIndex);
				}
				this.minimumCost = costCalculator.getMinimumCost();
				if(minimumCost < 0) {
					throw new IllegalStateException("Minimum container cost must be non-negative for index " + containerIndex);
				}
			} else {
				this.minimumCost = 0;
			}
		}
	}

	private final Objective objective;
	private final PackagerInterruptSupplier interrupt;
	private final List<Unit> units;
	private final Capacity[] capacities;
	private final int maxCount;
	private final int[] available;
	private final int[] openedTypes;
	private final long[] openedRemainingVolumes;
	private final long[] openedRemainingWeights;
	private final int[] selectedOpenContainerIndexes;
	private final long[] remainingVolumes;
	private final long[] remainingWeights;
	private final int[] capacityIndexesByVolume;
	private final int[] capacityIndexesByWeight;
	private final int[] capacityIndexesByCostPerVolume;
	private final int[] capacityIndexesByCostPerWeight;

	private Allocation best;
	private long openedMinimumCost;
	private long openedRemainingVolume;
	private long openedRemainingWeight;
	private int openedCount;
	private int visits;

	private ContainerAllocationPlanner(Objective objective, PackagerInterruptSupplier interrupt,
			List<Unit> units, List<Capacity> capacities, int maxCount) {
		this.objective = objective;
		this.interrupt = interrupt;
		this.units = units;
		this.capacities = capacities.toArray(Capacity[]::new);
		this.maxCount = maxCount;
		this.available = new int[this.capacities.length];
		for(int i = 0; i < available.length; i++) {
			available[i] = this.capacities[i].count;
		}
		this.openedTypes = new int[maxCount];
		this.openedRemainingVolumes = new long[maxCount];
		this.openedRemainingWeights = new long[maxCount];
		this.selectedOpenContainerIndexes = new int[units.size()];
		this.remainingVolumes = new long[units.size() + 1];
		this.remainingWeights = new long[units.size() + 1];
		for(int i = units.size() - 1; i >= 0; i--) {
			Unit unit = units.get(i);
			remainingVolumes[i] = remainingVolumes[i + 1] + unit.volume;
			remainingWeights[i] = remainingWeights[i + 1] + unit.weight;
		}
		this.capacityIndexesByVolume = capacityIndexesBy(this.capacities, true);
		this.capacityIndexesByWeight = capacityIndexesBy(this.capacities, false);
		this.capacityIndexesByCostPerVolume = capacityIndexesByCostPer(this.capacities, true);
		this.capacityIndexesByCostPerWeight = capacityIndexesByCostPer(this.capacities, false);
	}

	static Allocation plan(PackagerAdapter adapter, Objective objective, boolean[] excluded,
			PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		ContainerItemsCalculator calculator = adapter.getContainerItemsCalculator();
		int maxCount = adapter.getMaxContainerCount();
		List<BoxItemGroup> groups = adapter.getRemainingBoxItemGroups();
		List<Unit> units;
		if(groups != null) {
			if(!calculator.isGroupFeasible(groups, maxCount, excluded)) {
				return null;
			}
			units = groupUnits(groups);
		} else {
			List<BoxItem> items = adapter.getRemainingBoxItems();
			if(items == null) {
				return null;
			}
			if(!calculator.isFeasible(items, maxCount, excluded)) {
				return null;
			}
			units = boxUnits(items);
		}
		return plan(calculator, units, maxCount, objective, excluded, interrupt);
	}

	/**
	 * Returns true for adapters which do not expose their remaining items. This
	 * keeps the check usable by custom adapters while production adapters receive
	 * the full allocation test.
	 */
	static boolean canAllocate(PackagerAdapter adapter, PackagerInterruptSupplier interrupt)
			throws PackagerInterruptedException {
		ContainerItemsCalculator calculator = adapter.getContainerItemsCalculator();
		int maxCount = adapter.getMaxContainerCount();
		List<BoxItemGroup> groups = adapter.getRemainingBoxItemGroups();
		List<Unit> units;
		if(groups != null && !groups.isEmpty()) {
			if(!calculator.isGroupFeasible(groups, maxCount)) {
				return false;
			}
			units = groupUnits(groups);
		} else {
			List<BoxItem> items = adapter.getRemainingBoxItems();
			if(items == null || items.isEmpty()) {
				return true;
			}
			if(!calculator.isFeasible(items, maxCount)) {
				return false;
			}
			units = boxUnits(items);
		}
		return plan(calculator, units, maxCount, Objective.FEASIBLE, null, interrupt) != null;
	}

	private static List<Unit> boxUnits(List<BoxItem> items) {
		int count = 0;
		for(BoxItem item : items) {
			count += item.getCount();
		}
		List<Unit> units = new ArrayList<>(count);
		for(BoxItem item : items) {
			Unit unit = new Unit(item);
			for(int i = 0; i < item.getCount(); i++) {
				units.add(unit);
			}
		}
		return units;
	}

	private static List<Unit> groupUnits(List<BoxItemGroup> groups) {
		List<Unit> units = new ArrayList<>(groups.size());
		for(BoxItemGroup group : groups) {
			units.add(new Unit(group));
		}
		return units;
	}

	private static Allocation plan(ContainerItemsCalculator calculator, List<Unit> units, int maxCount,
			Objective objective, boolean[] excluded, PackagerInterruptSupplier interrupt)
			throws PackagerInterruptedException {
		if(units.isEmpty()) {
			return new Allocation(new int[0], 0);
		}
		maxCount = Math.min(maxCount, units.size());
		if(maxCount <= 0) {
			return null;
		}

		List<Capacity> capacities = new ArrayList<>(calculator.getContainerItemCount());
		for(int i = 0; i < calculator.getContainerItemCount(); i++) {
			ControlledContainerItem item = calculator.getContainerItem(i);
			if(!item.isAvailable() || excluded != null && i < excluded.length && excluded[i]) {
				continue;
			}
			int count = Math.min(item.getCount(), maxCount);
			if(count > 0) {
				capacities.add(new Capacity(i, item, count, objective));
			}
		}
		if(capacities.isEmpty()) {
			return null;
		}
		orderCapacities(capacities, objective);
		Allocation allocation = directAllocation(calculator, units, capacities, maxCount, objective);
		if(allocation != null) {
			return allocation;
		}

		for(Unit unit : units) {
			if(unit.fits != null) {
				continue;
			}
			unit.fits = new boolean[capacities.size()];
			unit.fitCount = 0;
			for(int i = 0; i < capacities.size(); i++) {
				Capacity capacity = capacities.get(i);
				if(unit.volume <= capacity.volume && unit.weight <= capacity.weight
						&& unit.canLoad(calculator, capacity.containerIndex)) {
					unit.fits[i] = true;
					unit.fitCount++;
				}
			}
			if(unit.fitCount == 0) {
				return null;
			}
		}
		units.sort(Comparator.comparingInt((Unit unit) -> unit.fitCount)
				.thenComparing(Comparator.comparingLong((Unit unit) -> unit.volume).reversed())
				.thenComparing(Comparator.comparingLong((Unit unit) -> unit.weight).reversed()));

		ContainerAllocationPlanner planner = new ContainerAllocationPlanner(objective, interrupt, units,
				capacities, maxCount);
		planner.search(0);
		return planner.best;
	}

	private static Allocation directAllocation(ContainerItemsCalculator calculator, List<Unit> units,
			List<Capacity> capacities, int maxCount, Objective objective) {
		if(units.size() == 1) {
			return singleUnitAllocation(calculator, units.get(0), capacities, objective);
		}
		if(maxCount == 1) {
			return oneContainerAllocation(calculator, units, capacities, objective);
		}
		if(objective == Objective.FEASIBLE) {
			return unlimitedInventoryAllocation(calculator, units, capacities, maxCount);
		}
		return null;
	}

	private static Allocation singleUnitAllocation(ContainerItemsCalculator calculator, Unit unit,
			List<Capacity> capacities, Objective objective) {
		Capacity best = null;
		long bestCost = Long.MAX_VALUE;
		for(Capacity capacity : capacities) {
			if(!fits(calculator, unit, capacity)) {
				continue;
			}
			if(objective != Objective.LOWEST_COST) {
				return new Allocation(new int[] {capacity.containerIndex}, 0);
			}
			long cost = capacity.costCalculator.calculateCost(unit.weight);
			if(cost < 0) {
				throw new IllegalStateException("Container cost must be non-negative for index "
						+ capacity.containerIndex);
			}
			if(cost < bestCost) {
				best = capacity;
				bestCost = cost;
			}
		}
		return best == null ? null : new Allocation(new int[] {best.containerIndex}, bestCost);
	}

	private static Allocation oneContainerAllocation(ContainerItemsCalculator calculator, List<Unit> units,
			List<Capacity> capacities, Objective objective) {
		long volume = 0;
		long weight = 0;
		for(Unit unit : units) {
			volume += unit.volume;
			weight += unit.weight;
		}
		Capacity best = null;
		long bestCost = Long.MAX_VALUE;
		for(Capacity capacity : capacities) {
			if(volume > capacity.volume || weight > capacity.weight) {
				continue;
			}
			boolean fits = true;
			for(Unit unit : units) {
				if(!fits(calculator, unit, capacity)) {
					fits = false;
					break;
				}
			}
			if(!fits) {
				continue;
			}
			if(objective != Objective.LOWEST_COST) {
				return new Allocation(new int[] {capacity.containerIndex}, 0);
			}
			long cost = capacity.costCalculator.calculateCost(weight);
			if(cost < 0) {
				throw new IllegalStateException("Container cost must be non-negative for index "
						+ capacity.containerIndex);
			}
			if(cost < bestCost) {
				best = capacity;
				bestCost = cost;
			}
		}
		return best == null ? null : new Allocation(new int[] {best.containerIndex}, bestCost);
	}

	private static Allocation unlimitedInventoryAllocation(ContainerItemsCalculator calculator, List<Unit> units,
			List<Capacity> capacities, int maxCount) {
		if(maxCount < units.size()) {
			return null;
		}
		for(Capacity capacity : capacities) {
			if(capacity.count < units.size()) {
				continue;
			}
			boolean fits = true;
			for(Unit unit : units) {
				if(!fits(calculator, unit, capacity)) {
					fits = false;
					break;
				}
			}
			if(fits) {
				int[] indexes = new int[units.size()];
				for(int i = 0; i < indexes.length; i++) {
					indexes[i] = capacity.containerIndex;
				}
				return new Allocation(indexes, 0);
			}
		}

		int remaining = units.size();
		for(Capacity capacity : capacities) {
			for(Unit unit : units) {
				if(!fits(calculator, unit, capacity)) {
					return null;
				}
			}
			remaining -= capacity.count;
		}
		if(remaining > 0) {
			return null;
		}
		int[] indexes = new int[units.size()];
		int index = 0;
		for(Capacity capacity : capacities) {
			int count = Math.min(capacity.count, indexes.length - index);
			for(int i = 0; i < count; i++) {
				indexes[index++] = capacity.containerIndex;
			}
		}
		return new Allocation(indexes, 0);
	}

	private static boolean fits(ContainerItemsCalculator calculator, Unit unit, Capacity capacity) {
		return unit.volume <= capacity.volume && unit.weight <= capacity.weight
				&& unit.canLoad(calculator, capacity.containerIndex);
	}

	private static void orderCapacities(List<Capacity> capacities, Objective objective) {
		if(objective == Objective.LOWEST_COST) {
			capacities.sort(Comparator.comparingLong((Capacity capacity) -> capacity.minimumCost)
					.thenComparing(Comparator.comparingLong((Capacity capacity) -> capacity.volume).reversed())
					.thenComparing(Comparator.comparingLong((Capacity capacity) -> capacity.weight).reversed()));
			return;
		}
		capacities.sort(Comparator.comparingLong((Capacity capacity) -> capacity.volume).reversed()
				.thenComparing(Comparator.comparingLong((Capacity capacity) -> capacity.weight).reversed()));
	}

	private void search(int unitIndex) throws PackagerInterruptedException {
		if((visits++ & 0x3ff) == 0 && interrupt.getAsBoolean()) {
			throw new PackagerInterruptedException();
		}
		if(best != null) {
			if(objective == Objective.FEASIBLE) {
				return;
			}
			if(objective == Objective.FEWEST_CONTAINERS && openedCount >= best.getContainerCount()) {
				return;
			}
			if(objective == Objective.LOWEST_COST && (openedMinimumCost > best.getCost()
					|| openedMinimumCost == best.getCost() && openedCount >= best.getContainerCount())) {
				return;
			}
		}
		if(unitIndex == units.size()) {
			accept();
			return;
		}
		if(!hasCapacityForRemainingUnits(unitIndex)) {
			return;
		}
		if(best != null && objective == Objective.FEWEST_CONTAINERS) {
			int additionalContainers = minimumAdditionalContainerCount(unitIndex);
			if(additionalContainers == Integer.MAX_VALUE
					|| openedCount + additionalContainers >= best.getContainerCount()) {
				return;
			}
		}
		if(best != null && objective == Objective.LOWEST_COST) {
			long additionalCost = minimumAdditionalCost(unitIndex);
			long minimumCost = openedMinimumCost + additionalCost;
			if(minimumCost > best.getCost() || minimumCost == best.getCost()
					&& openedCount >= best.getContainerCount()) {
				return;
			}
		}

		Unit unit = units.get(unitIndex);
		int minimumOpenContainerIndex = unitIndex > 0 && unit == units.get(unitIndex - 1)
				? selectedOpenContainerIndexes[unitIndex - 1] : 0;
		for(int i = minimumOpenContainerIndex; i < openedCount; i++) {
			int type = openedTypes[i];
			long remainingVolume = openedRemainingVolumes[i];
			long remainingWeight = openedRemainingWeights[i];
			if(!unit.fits[type] || unit.volume > remainingVolume || unit.weight > remainingWeight
					|| hasEquivalentOpenContainer(i, type, remainingVolume, remainingWeight)) {
				continue;
			}
			openedRemainingVolumes[i] = remainingVolume - unit.volume;
			openedRemainingWeights[i] = remainingWeight - unit.weight;
			openedRemainingVolume -= unit.volume;
			openedRemainingWeight -= unit.weight;
			selectedOpenContainerIndexes[unitIndex] = i;
			search(unitIndex + 1);
			openedRemainingVolumes[i] = remainingVolume;
			openedRemainingWeights[i] = remainingWeight;
			openedRemainingVolume += unit.volume;
			openedRemainingWeight += unit.weight;
			if(best != null && objective == Objective.FEASIBLE) {
				return;
			}
		}

		if(openedCount >= maxCount || best != null && objective == Objective.FEWEST_CONTAINERS
				&& openedCount + 1 >= best.getContainerCount()) {
			return;
		}
		for(int i = 0; i < capacities.length; i++) {
			if(available[i] == 0 || !unit.fits[i]) {
				continue;
			}
			Capacity capacity = capacities[i];
			if(best != null && objective == Objective.LOWEST_COST
					&& (openedMinimumCost + capacity.minimumCost > best.getCost()
							|| openedMinimumCost + capacity.minimumCost == best.getCost()
									&& openedCount + 1 >= best.getContainerCount())) {
				continue;
			}
			available[i]--;
			int openedIndex = openedCount++;
			openedTypes[openedIndex] = i;
			openedRemainingVolumes[openedIndex] = capacity.volume - unit.volume;
			openedRemainingWeights[openedIndex] = capacity.weight - unit.weight;
			openedRemainingVolume += capacity.volume - unit.volume;
			openedRemainingWeight += capacity.weight - unit.weight;
			selectedOpenContainerIndexes[unitIndex] = openedIndex;
			openedMinimumCost += capacity.minimumCost;
			search(unitIndex + 1);
			openedMinimumCost -= capacity.minimumCost;
			openedRemainingVolume -= capacity.volume - unit.volume;
			openedRemainingWeight -= capacity.weight - unit.weight;
			openedCount--;
			available[i]++;
			if(best != null && objective == Objective.FEASIBLE) {
				return;
			}
		}
	}

	private boolean hasCapacityForRemainingUnits(int unitIndex) {
		int availableContainerCount = maxCount - openedCount;
		long remainingVolume = remainingVolumes[unitIndex] - openedRemainingVolume;
		if(!hasAvailableCapacity(remainingVolume, availableContainerCount, capacityIndexesByVolume, true)) {
			return false;
		}
		long remainingWeight = remainingWeights[unitIndex] - openedRemainingWeight;
		return hasAvailableCapacity(remainingWeight, availableContainerCount, capacityIndexesByWeight, false);
	}

	private long minimumAdditionalCost(int unitIndex) {
		int availableContainerCount = maxCount - openedCount;
		long volume = remainingVolumes[unitIndex] - openedRemainingVolume;
		long volumeCost = minimumAdditionalCost(volume, availableContainerCount, capacityIndexesByCostPerVolume, true);
		long weight = remainingWeights[unitIndex] - openedRemainingWeight;
		long weightCost = minimumAdditionalCost(weight, availableContainerCount, capacityIndexesByCostPerWeight, false);
		return Math.max(volumeCost, weightCost);
	}

	private int minimumAdditionalContainerCount(int unitIndex) {
		int availableContainerCount = maxCount - openedCount;
		long volume = remainingVolumes[unitIndex] - openedRemainingVolume;
		int volumeCount = minimumContainerCount(volume, availableContainerCount, capacityIndexesByVolume, true);
		long weight = remainingWeights[unitIndex] - openedRemainingWeight;
		int weightCount = minimumContainerCount(weight, availableContainerCount, capacityIndexesByWeight, false);
		return Math.max(volumeCount, weightCount);
	}

	private int minimumContainerCount(long required, int availableContainerCount, int[] capacityIndexes,
			boolean volume) {
		if(required <= 0) {
			return 0;
		}
		int containerCount = 0;
		for(int capacityIndex : capacityIndexes) {
			if(availableContainerCount == 0) {
				break;
			}
			int count = Math.min(available[capacityIndex], availableContainerCount);
			if(count == 0) {
				continue;
			}
			long perContainer = volume ? capacities[capacityIndex].volume : capacities[capacityIndex].weight;
			if(perContainer == 0) {
				continue;
			}
			int needed = (int) (1 + (required - 1) / perContainer);
			if(needed <= count) {
				return containerCount + needed;
			}
			containerCount += count;
			required -= perContainer * count;
			availableContainerCount -= count;
		}
		return Integer.MAX_VALUE;
	}

	private long minimumAdditionalCost(long required, int availableContainerCount, int[] capacityIndexes,
			boolean volume) {
		if(required <= 0) {
			return 0;
		}
		double cost = 0;
		for(int capacityIndex : capacityIndexes) {
			if(availableContainerCount == 0) {
				break;
			}
			int count = Math.min(available[capacityIndex], availableContainerCount);
			if(count == 0) {
				continue;
			}
			Capacity capacity = capacities[capacityIndex];
			long perContainer = volume ? capacity.volume : capacity.weight;
			if(perContainer == 0) {
				continue;
			}
			if(count >= 1 + (required - 1) / perContainer) {
				return (long) (cost + (double) capacity.minimumCost * required / perContainer);
			}
			cost += (double) capacity.minimumCost * count;
			required -= perContainer * count;
			availableContainerCount -= count;
		}
		return Long.MAX_VALUE;
	}

	private boolean hasAvailableCapacity(long required, int availableContainerCount, int[] capacityIndexes,
			boolean volume) {
		if(required <= 0) {
			return true;
		}
		long capacity = 0;
		for(int capacityIndex : capacityIndexes) {
			if(availableContainerCount == 0) {
				break;
			}
			int count = Math.min(available[capacityIndex], availableContainerCount);
			if(count == 0) {
				continue;
			}
			long perContainer = volume ? capacities[capacityIndex].volume : capacities[capacityIndex].weight;
			long remaining = required - capacity;
			if(perContainer > 0 && count >= 1 + (remaining - 1) / perContainer) {
				return true;
			}
			capacity += perContainer * count;
			availableContainerCount -= count;
		}
		return false;
	}

	private static int[] capacityIndexesBy(Capacity[] capacities, boolean volume) {
		int[] indexes = new int[capacities.length];
		for(int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		for(int i = 1; i < indexes.length; i++) {
			int index = indexes[i];
			long value = volume ? capacities[index].volume : capacities[index].weight;
			int j = i;
			while(j > 0) {
				int previous = indexes[j - 1];
				long previousValue = volume ? capacities[previous].volume : capacities[previous].weight;
				if(previousValue >= value) {
					break;
				}
				indexes[j] = previous;
				j--;
			}
			indexes[j] = index;
		}
		return indexes;
	}

	private static int[] capacityIndexesByCostPer(Capacity[] capacities, boolean volume) {
		int[] indexes = new int[capacities.length];
		for(int i = 0; i < indexes.length; i++) {
			indexes[i] = i;
		}
		for(int i = 1; i < indexes.length; i++) {
			int index = indexes[i];
			int j = i;
			while(j > 0 && compareCostPerCapacity(capacities[index], capacities[indexes[j - 1]], volume) < 0) {
				indexes[j] = indexes[j - 1];
				j--;
			}
			indexes[j] = index;
		}
		return indexes;
	}

	private static int compareCostPerCapacity(Capacity left, Capacity right, boolean volume) {
		long leftCapacity = volume ? left.volume : left.weight;
		long rightCapacity = volume ? right.volume : right.weight;
		if(leftCapacity == 0 || rightCapacity == 0) {
			return Long.compare(leftCapacity, rightCapacity);
		}
		return Double.compare((double) left.minimumCost / leftCapacity, (double) right.minimumCost / rightCapacity);
	}

	private boolean hasEquivalentOpenContainer(int end, int type, long remainingVolume, long remainingWeight) {
		for(int i = 0; i < end; i++) {
			if(openedTypes[i] == type && openedRemainingVolumes[i] == remainingVolume
					&& openedRemainingWeights[i] == remainingWeight) {
				return true;
			}
		}
		return false;
	}

	private void accept() {
		long cost = 0;
		if(objective == Objective.LOWEST_COST) {
			for(int i = 0; i < openedCount; i++) {
				Capacity capacity = capacities[openedTypes[i]];
				long value = capacity.costCalculator.calculateCost(capacity.weight - openedRemainingWeights[i]);
				if(value < 0) {
					throw new IllegalStateException("Container cost must be non-negative for index "
							+ capacity.containerIndex);
				}
				cost += value;
			}
		}
		if(best != null) {
			if(objective == Objective.FEWEST_CONTAINERS && openedCount >= best.getContainerCount()) {
				return;
			}
			if(objective == Objective.LOWEST_COST
					&& (cost > best.getCost() || cost == best.getCost()
							&& openedCount >= best.getContainerCount())) {
				return;
			}
		}
		int[] indexes = new int[openedCount];
		for(int i = 0; i < indexes.length; i++) {
			indexes[i] = capacities[openedTypes[i]].containerIndex;
		}
		best = new Allocation(indexes, cost);
	}
}
