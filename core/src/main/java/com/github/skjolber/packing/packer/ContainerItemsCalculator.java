package com.github.skjolber.packing.packer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.cost.ContainerCostCalculator;

public class ContainerItemsCalculator implements Cloneable {

	public static class Limit {
		
		private BigInteger value;
		private Set<Integer> containerIndexes;
		private long minimum;
		
		public Limit(BigInteger value, Set<Integer> containerIndexes, long minimum) {
			super();
			this.value = value;
			this.containerIndexes = containerIndexes;
			this.minimum = minimum;
		}
		
		public Set<Integer> getContainerIndexes() {
			return containerIndexes;
		}
		
		public long getMinimum() {
			return minimum;
		}
		
		public BigInteger getValue() {
			return value;
		}
	}
	
	protected final List<ControlledContainerItem> containerItems;
	protected final List<ControlledContainerItem> containerItemsSortedByWeight;
	protected final List<ControlledContainerItem> containerItemsSortedByVolume;
	protected int containerCount;
	protected final int resetContainerCount;
	
	protected long cost;

	public ContainerItemsCalculator(List<ControlledContainerItem> items, int containerCount) {
		this(items, containerCount, containerCount);
	}
	
	public ContainerItemsCalculator(List<ControlledContainerItem> items, int containerCount, int resetContainerCount) {
		for(int i = 0; i < items.size(); i++) {
			items.get(i).setIndex(i);
		}
		this.containerItems = items;
		this.containerCount = containerCount;
		this.resetContainerCount = resetContainerCount;

		this.containerItemsSortedByWeight = new ArrayList<>(items);
		this.containerItemsSortedByVolume = new ArrayList<>(items);
		
		Collections.sort(containerItemsSortedByWeight, ContainerItem.MAX_LOAD_WEIGHT_COMPARATOR);
		Collections.sort(containerItemsSortedByVolume, ContainerItem.MAX_LOAD_VOLUME_COMPARATOR);
	}

	@Override
	public ContainerItemsCalculator clone() {
		List<ControlledContainerItem> copies = new ArrayList<>(containerItems.size());
		for(ControlledContainerItem item : containerItems) {
			copies.add(new ControlledContainerItem(item));
		}
		ContainerItemsCalculator clone = new ContainerItemsCalculator(copies, containerCount, resetContainerCount);
		clone.cost = cost;
		return clone;
	}

	/** Restore each container item's count to its reset count. */
	public void reset() {
		for(ControlledContainerItem item : containerItems) {
			item.reset();
		}
		containerCount = resetContainerCount;
		cost = 0;
	}

	/**
	 * Return container indexes which can potentially hold the boxes using the
	 * remaining container count.
	 *
	 * @param boxes list of boxes
	 * @return eligible containers and their box-item fit records
	 */
	
	public ContainerItemsResult getContainers(List<BoxItem> boxes) {
		return getContainers(boxes, containerCount);
	}

	/**
	 * Return eligible container indexes for a search using at most
	 * {@code maxCount} of the remaining containers.
	 *
	 * @param boxes list of boxes
	 * @param maxCount maximum number of containers for this query
	 * @return eligible containers and their box-item fit records
	 */
	public ContainerItemsResult getContainers(List<BoxItem> boxes, int maxCount) {
		long totalVolume = 0;
		long totalWeight = 0;
		int boxCount = 0;
		for (BoxItem box : boxes) {
			// volume
			totalVolume += box.getVolume();

			// weight
			totalWeight += box.getWeight();

			boxCount += box.getCount();
		}

		// set a more realistic max; no more than one container per box
		maxCount = Math.min(Math.min(containerCount, maxCount), boxCount);

		boolean[][] fits = new boolean[boxes.size()][containerItems.size()];
		
		if(maxCount == 1) {
			List<Integer> result = new ArrayList<>(containerItems.size());

			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = getContainerItem(i);
				if(!item.isAvailable()) {
					continue;
				}

				// this container must be able to load all boxes
				Container container = item.getContainer();

				if(container.getMaxLoadVolume() < totalVolume) {
					continue;
				}
				if(container.getMaxLoadWeight() < totalWeight) {
					continue;
				}

				recordBoxFits(boxes, i, fits);
				if(!canLoadAll(fits, i)) {
					continue;
				}
				result.add(i);
			}
			return new ContainerItemsResult(result, fits, containerItems.size());
		}

		// sanity check - exact values for volume
		Limit totalAvailableVolume = calculateMaxVolume(maxCount);
		if(totalAvailableVolume.value.compareTo(BigInteger.valueOf(totalVolume)) < 0) {
			// constrained by volume
			return new ContainerItemsResult(Collections.emptyList(), fits, containerItems.size());
		}

		// sanity check - exact values for weight
		Limit totalAvailableWeight = calculateMaxWeight(maxCount);
		if(totalAvailableWeight.value.compareTo(BigInteger.valueOf(totalWeight)) < 0) {
			// constrained by weight
			return new ContainerItemsResult(Collections.emptyList(), fits, containerItems.size());
		}

		long minVolume = Long.MAX_VALUE;
		long minWeight = Long.MAX_VALUE;

		for (BoxItem box : boxes) {
			// volume
			long boxVolume = box.getBox().getVolume();
			if(boxVolume < minVolume) {
				minVolume = boxVolume;
			}

			// weight
			long boxWeight = box.getBox().getWeight();
			if(boxWeight < minWeight) {
				minWeight = boxWeight;
			}
		}

		List<Integer> result = new ArrayList<>(containerItems.size());
		for (int i = 0; i < containerItems.size(); i++) {
			ContainerItem item = containerItems.get(i);

			if(!item.isAvailable()) {
				continue;
			}

			Container container = item.getContainer();
			if(container.getMaxLoadVolume() < minVolume || container.getMaxLoadWeight() < minWeight) {
				// this container cannot even fit a single box
				continue;
			}

			// sanity-check use of this container
			// corner case: can we exchange a bigger container for the current and still have enough weight / volume?
			if(!totalAvailableVolume.containerIndexes.contains(i)) {
				long reduction = totalAvailableVolume.minimum - container.getMaxLoadVolume();

				BigInteger maxAvailableVolumeWithThisContainer = totalAvailableVolume.value.subtract(BigInteger.valueOf(reduction));
				if(maxAvailableVolumeWithThisContainer.compareTo(BigInteger.valueOf(totalVolume)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}
			}

			if(!totalAvailableWeight.containerIndexes.contains(i)) {
				long reduction = totalAvailableWeight.minimum - container.getMaxLoadWeight();

				BigInteger maxAvailableWeightWithThisContainer = totalAvailableWeight.value.subtract(BigInteger.valueOf(reduction));
				if(maxAvailableWeightWithThisContainer.compareTo(BigInteger.valueOf(totalWeight)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

			}

			// must be able to load at least one
			recordBoxFits(boxes, i, fits);
			if(!canLoadAtLeastOne(fits, i)) {
				continue;
			}
			result.add(i);
		}

		return new ContainerItemsResult(result, fits, containerItems.size());
	}

	public ContainerItemsResult getGroupContainers(List<BoxItemGroup> groups) {
		return getGroupContainers(groups, containerCount);
	}

	/**
	 * Return eligible container indexes for a search using at most
	 * {@code maxCount} of the remaining containers.
	 *
	 * @param groups list of box-item groups
	 * @param maxCount maximum number of containers for this query
	 * @return eligible containers and their group fit records
	 */
	public ContainerItemsResult getGroupContainers(List<BoxItemGroup> groups, int maxCount) {
		long totalBoxVolume = 0;
		long totalBoxWeight = 0;

		long minGroupVolume = Long.MAX_VALUE;
		long minGroupWeight = Long.MAX_VALUE;

		for (BoxItemGroup group : groups) {
			// volume
			for (BoxItem boxItem : group.getItems()) {

				long volume = boxItem.getVolume();
				if(minGroupVolume > volume) {
					minGroupVolume = volume;
				}
				long weight = boxItem.getWeight();
				if(minGroupWeight > weight) {
					minGroupWeight = weight;
				}
				
				totalBoxVolume += volume;

				// weight
				totalBoxWeight += weight;

			}
		}

		// at least one container per group
		maxCount = Math.min(Math.min(containerCount, maxCount), groups.size());

		boolean[][] fits = new boolean[groups.size()][containerItems.size()];

		if(maxCount == 1) {
			List<Integer> list = new ArrayList<>(containerItems.size());

			// check if everything can fit in the same container
			containers: 
			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = containerItems.get(i);
				if(!item.isAvailable()) {
					continue;
				}
				
				Container c = item.getContainer();
				if(c.getMaxLoadVolume() < totalBoxVolume) {
					continue;
				}
				if(c.getMaxLoadWeight() < totalBoxWeight) {
					continue;
				}
				
				recordGroupFits(groups, i, fits);
				if(!canLoadAll(fits, i)) {
					continue containers;
				}
				list.add(i);
			}
			return new ContainerItemsResult(list, fits, containerItems.size());
		}

		// sanity check - exact values for volume
		Limit totalAvailableVolume = calculateMaxVolume(maxCount);
		if(totalAvailableVolume.value.compareTo(BigInteger.valueOf(totalBoxVolume)) < 0) {
			// constrained by volume
			return new ContainerItemsResult(Collections.emptyList(), fits, containerItems.size());
		}

		// sanity check - exact values for weight
		Limit totalAvailableWeight = calculateMaxWeight(maxCount);
		if(totalAvailableWeight.value.compareTo(BigInteger.valueOf(totalBoxWeight)) < 0) {
			// constrained by weight
			return new ContainerItemsResult(Collections.emptyList(), fits, containerItems.size());
		}

		List<Integer> list = new ArrayList<>(getContainerItemCount());
		for (int i = 0; i < getContainerItemCount(); i++) {
			ContainerItem item = getContainerItem(i);
				
			if(!item.isAvailable()) {
				continue;
			}

			Container container = item.getContainer();

			if(container.getMaxLoadVolume() < minGroupVolume || container.getMaxLoadWeight() < minGroupWeight) {
				// this container cannot even fit a single group
				continue;
			}
				
			// sanity-check use of this container
			// corner case: can we exchange a bigger container for the current and still have enough weight / volume?
			if(!totalAvailableVolume.containerIndexes.contains(i)) {
				long reduction = totalAvailableVolume.minimum - container.getMaxLoadVolume();

				BigInteger maxAvailableVolumeWithThisContainer = totalAvailableVolume.value.subtract(BigInteger.valueOf(reduction));
				if(maxAvailableVolumeWithThisContainer.compareTo(BigInteger.valueOf(totalBoxVolume)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}
			}

			if(!totalAvailableWeight.containerIndexes.contains(i)) {
				long reduction = totalAvailableWeight.minimum - container.getMaxLoadWeight();

				BigInteger maxAvailableWeightWithThisContainer = totalAvailableWeight.value.subtract(BigInteger.valueOf(reduction));
				if(maxAvailableWeightWithThisContainer.compareTo(BigInteger.valueOf(totalBoxWeight)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

			}

			recordGroupFits(groups, i, fits);
			if(!canLoadAtLeastOne(fits, i)) {
				continue;
			}
			list.add(i);
		}

		return new ContainerItemsResult(list, fits, containerItems.size());
	}

	/**
	 * Return whether the available inventory has enough aggregate volume and
	 * weight and every remaining box-item type can be loaded by at least one
	 * container type.
	 *
	 * @param boxItems remaining box items from this packaging operation
	 * @return {@code true} if every non-empty box item has an available container
	 */
	public boolean isFeasible(List<BoxItem> boxItems) {
		return isFeasible(boxItems, containerCount, null);
	}

	/** Check box-item feasibility using at most {@code maxCount} containers. */
	public boolean isFeasible(List<BoxItem> boxItems, int maxCount) {
		return isFeasible(boxItems, maxCount, null);
	}

	/**
	 * Check box-item feasibility using at most {@code maxCount} containers while
	 * ignoring excluded container-item indexes.
	 */
	public boolean isFeasible(List<BoxItem> boxItems, int maxCount, boolean[] excluded) {
		long totalVolume = 0;
		long totalWeight = 0;
		int boxCount = 0;
		for(BoxItem boxItem : boxItems) {
			if(boxItem.isEmpty()) {
				continue;
			}
			totalVolume += boxItem.getVolume();
			totalWeight += boxItem.getWeight();
			boxCount += boxItem.getCount();
		}
		if(!hasCapacity(maxCount, boxCount, totalVolume, totalWeight, excluded)) {
			return false;
		}
		for(BoxItem boxItem : boxItems) {
			if(boxItem.isEmpty()) {
				continue;
			}
			boolean match = false;
			for(int containerItemIndex = 0; containerItemIndex < containerItems.size(); containerItemIndex++) {
				if(!isExcluded(containerItemIndex, excluded)
						&& containerItems.get(containerItemIndex).isAvailable()
						&& canLoad(boxItem, containerItemIndex)) {
					match = true;
					break;
				}
			}
			if(!match) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Return whether the available inventory has enough aggregate volume and
	 * weight and every remaining box-item group can be loaded by at least one
	 * container type.
	 *
	 * @param groups remaining groups from this packaging operation
	 * @return {@code true} if every non-empty group has an available container
	 */
	public boolean isGroupFeasible(List<BoxItemGroup> groups) {
		return isGroupFeasible(groups, containerCount, null);
	}

	/** Check box-item-group feasibility using at most {@code maxCount} containers. */
	public boolean isGroupFeasible(List<BoxItemGroup> groups, int maxCount) {
		return isGroupFeasible(groups, maxCount, null);
	}

	/**
	 * Check box-item-group feasibility using at most {@code maxCount} containers
	 * while ignoring excluded container-item indexes.
	 */
	public boolean isGroupFeasible(List<BoxItemGroup> groups, int maxCount, boolean[] excluded) {
		long totalVolume = 0;
		long totalWeight = 0;
		int groupCount = 0;
		for(BoxItemGroup group : groups) {
			if(group.isEmpty()) {
				continue;
			}
			totalVolume += group.getVolume();
			totalWeight += group.getWeight();
			groupCount++;
		}
		if(!hasCapacity(maxCount, groupCount, totalVolume, totalWeight, excluded)) {
			return false;
		}
		for(BoxItemGroup group : groups) {
			if(group.isEmpty()) {
				continue;
			}
			boolean match = false;
			for(int containerItemIndex = 0; containerItemIndex < containerItems.size(); containerItemIndex++) {
				if(!isExcluded(containerItemIndex, excluded)
						&& containerItems.get(containerItemIndex).isAvailable()
						&& canLoad(group, containerItemIndex)) {
					match = true;
					break;
				}
			}
			if(!match) {
				return false;
			}
		}
		return true;
	}

	private void recordBoxFits(List<BoxItem> boxes, int containerItemIndex, boolean[][] fits) {
		for(int boxItemIndex = 0; boxItemIndex < boxes.size(); boxItemIndex++) {
			fits[boxItemIndex][containerItemIndex] = canLoad(boxes.get(boxItemIndex), containerItemIndex);
		}
	}

	private void recordGroupFits(List<BoxItemGroup> groups, int containerItemIndex, boolean[][] fits) {
		for(int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
			fits[groupIndex][containerItemIndex] = canLoad(groups.get(groupIndex), containerItemIndex);
		}
	}

	public boolean canLoad(BoxItem boxItem, int containerItemIndex) {
		return containerItems.get(containerItemIndex).getContainer().canLoad(boxItem.getBox());
	}

	public boolean canLoad(BoxItemGroup group, int containerItemIndex) {
		return containerItems.get(containerItemIndex).getContainer().canLoad(group);
	}

	private static boolean canLoadAll(boolean[][] fits, int containerItemIndex) {
		for(boolean[] itemFits : fits) {
			if(!itemFits[containerItemIndex]) {
				return false;
			}
		}
		return true;
	}

	private static boolean canLoadAtLeastOne(boolean[][] fits, int containerItemIndex) {
		for(boolean[] itemFits : fits) {
			if(itemFits[containerItemIndex]) {
				return true;
			}
		}
		return false;
	}

	public Container toContainer(ContainerItem item, Stack stack) {
		item.decrement();
		containerCount--;

		Container container = item.getContainer();
		
		Container result = new Container(container.getId(), container.getDescription(),
				container.getDx(), container.getDy(), container.getDz(), 
				
				container.getEmptyWeight(), 
				
				container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), 
				
				container.getMaxLoadWeight(), stack, container.getMotion());

		ContainerCostCalculator costCalculator = item.getCostCalculator();
		if(costCalculator != null) {
			cost += costCalculator.calculateCost(result.getLoadWeight());
		}

		return result;
	}

	protected Limit calculateMaxVolume(int maxCount) {
		Set<Integer> includedContainerIndexes = new HashSet<>(containerItemsSortedByWeight.size() * 2);
		
		long minLoadVolume = Long.MAX_VALUE;
		
		BigInteger volume = BigInteger.valueOf(0);
		for(int i = containerItemsSortedByVolume.size() - 1; i >= 0 && maxCount > 0; i--) {
			ContainerItem containerItem = containerItemsSortedByVolume.get(i);
			if(!containerItem.isAvailable()) {
				continue;
			}

			int count = Math.min(maxCount, containerItem.getCount());
			
			BigInteger max = BigInteger.valueOf(containerItem.getContainer().getMaxLoadVolume()).multiply(BigInteger.valueOf(count));
			
			volume = volume.add(max);
			
			maxCount -= count;
			
			includedContainerIndexes.add(containerItem.getIndex());
			
			if(minLoadVolume > containerItem.getContainer().getMaxLoadVolume()) {
				minLoadVolume = containerItem.getContainer().getMaxLoadVolume();
			}
		}
		
		return new Limit(volume, includedContainerIndexes, minLoadVolume);
	}

	protected Limit calculateMaxWeight(int maxCount) {
		Set<Integer> includedContainerIndexes = new HashSet<>(containerItemsSortedByWeight.size() * 2);
		
		long minLoadWeight = Long.MAX_VALUE;
		
		BigInteger weight = BigInteger.valueOf(0);
		for(int i = containerItemsSortedByWeight.size() - 1; i >= 0 && maxCount > 0; i--) {
			ContainerItem containerItem = containerItemsSortedByWeight.get(i);
			if(!containerItem.isAvailable()) {
				continue;
			}
			
			int count = Math.min(maxCount, containerItem.getCount());

			BigInteger max = BigInteger.valueOf(containerItem.getContainer().getMaxLoadWeight()).multiply(BigInteger.valueOf(count));
			
			weight = weight.add(max);
			
			maxCount -= count;
			
			includedContainerIndexes.add(containerItem.getIndex());
			
			
			if(minLoadWeight > containerItem.getContainer().getMaxLoadWeight()) {
				minLoadWeight = containerItem.getContainer().getMaxLoadWeight();
			}
		}
		
		return new Limit(weight, includedContainerIndexes, minLoadWeight);
	}

	protected boolean hasMaxVolumeCapacity(int maxCount, long target) {
		return hasMaxVolumeCapacity(maxCount, target, null);
	}

	protected boolean hasMaxWeightCapacity(int maxCount, long target) {
		return hasMaxWeightCapacity(maxCount, target, null);
	}

	protected boolean hasCapacity(int maxCount, int unitCount, long totalVolume, long totalWeight,
			boolean[] excluded) {
		if(unitCount == 0) {
			return true;
		}
		maxCount = Math.min(Math.min(containerCount, maxCount), unitCount);
		return maxCount > 0 && hasMaxVolumeCapacity(maxCount, totalVolume, excluded)
				&& hasMaxWeightCapacity(maxCount, totalWeight, excluded);
	}

	protected boolean hasMaxVolumeCapacity(int maxCount, long target, boolean[] excluded) {
		return hasCapacity(containerItemsSortedByVolume, maxCount, target, true, excluded);
	}

	protected boolean hasMaxWeightCapacity(int maxCount, long target, boolean[] excluded) {
		return hasCapacity(containerItemsSortedByWeight, maxCount, target, false, excluded);
	}

	private static boolean hasCapacity(List<ControlledContainerItem> items, int maxCount, long target,
			boolean volume, boolean[] excluded) {
		long total = 0;
		for(int i = items.size() - 1; i >= 0 && maxCount > 0; i--) {
			ControlledContainerItem item = items.get(i);
			if(!item.isAvailable() || isExcluded(item.getIndex(), excluded)) {
				continue;
			}
			long capacity = volume ? item.getContainer().getMaxLoadVolume() : item.getContainer().getMaxLoadWeight();
			int count = Math.min(maxCount, item.getCount());
			long remaining = target - total;
			if(remaining <= 0 || capacity > 0 && count >= 1 + (remaining - 1) / capacity) {
				return true;
			}
			total += capacity * count;
			maxCount -= count;
		}
		return total >= target;
	}

	protected static boolean isExcluded(int containerItemIndex, boolean[] excluded) {
		return excluded != null && containerItemIndex < excluded.length && excluded[containerItemIndex];
	}

	protected static boolean hasExclusions(boolean[] excluded) {
		if(excluded != null) {
			for(boolean value : excluded) {
				if(value) {
					return true;
				}
			}
		}
		return false;
	}

	public int getContainerItemCount() {
		return containerItems.size();
	}

	/** Return the number of containers which can still be accepted. */
	public int getContainerCount() {
		return containerCount;
	}

	public ControlledContainerItem getContainerItem(int index) {
		return containerItems.get(index);
	}

	public List<ControlledContainerItem> getContainerItems() {
		return containerItems;
	}

	public boolean hasCost() {
		boolean anyCostCalculator = false;
		boolean allCostCalculators = true;
		for(ControlledContainerItem item : containerItems) {
			anyCostCalculator |= item.hasCostCalculator();
			allCostCalculators &= item.hasCostCalculator();
		}
		if(anyCostCalculator && !allCostCalculators) {
			throw new IllegalArgumentException("Expected either none or all containers to have a cost calculator");
		}
		return anyCostCalculator;
	}

	public long getCost() {
		return cost;
	}

}
