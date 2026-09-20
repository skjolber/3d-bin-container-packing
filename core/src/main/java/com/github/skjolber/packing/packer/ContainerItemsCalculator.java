package com.github.skjolber.packing.packer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.github.skjolber.packing.api.Box;
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
	 * @return list of containers
	 */
	
	public List<Integer> getContainers(List<BoxItem> boxes) {
		return getContainers(boxes, containerCount);
	}

	/**
	 * Return eligible container indexes for a search using at most
	 * {@code maxCount} of the remaining containers.
	 *
	 * @param boxes list of boxes
	 * @param maxCount maximum number of containers for this query
	 * @return list of container indexes
	 */
	public List<Integer> getContainers(List<BoxItem> boxes, int maxCount) {
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

				if(!canLoadAll(container, boxes)) {
					continue;
				}
				result.add(i);
			}
			return result;
		}

		// sanity check - exact values for volume
		Limit totalAvailableVolume = calculateMaxVolume(maxCount);
		if(totalAvailableVolume.value.compareTo(BigInteger.valueOf(totalVolume)) < 0) {
			// constrained by volume
			return Collections.emptyList();
		}

		// sanity check - exact values for weight
		Limit totalAvailableWeight = calculateMaxWeight(maxCount);
		if(totalAvailableWeight.value.compareTo(BigInteger.valueOf(totalWeight)) < 0) {
			// constrained by weight
			return Collections.emptyList();
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
			if(!canLoadAtLeastOneBox(container, boxes)) {
				continue;
			}
			result.add(i);
		}

		return result;
	}

	public List<Integer> getGroupContainers(List<BoxItemGroup> groups) {
		return getGroupContainers(groups, containerCount);
	}

	/**
	 * Return eligible container indexes for a search using at most
	 * {@code maxCount} of the remaining containers.
	 *
	 * @param groups list of box-item groups
	 * @param maxCount maximum number of containers for this query
	 * @return list of container indexes
	 */
	public List<Integer> getGroupContainers(List<BoxItemGroup> groups, int maxCount) {
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
				
				for (BoxItemGroup group : groups) {
					if(!c.canLoad(group)) {
						continue containers;
					}
				}
				list.add(i);
			}
			return list;
		}

		// sanity check - exact values for volume
		Limit totalAvailableVolume = calculateMaxVolume(maxCount);
		if(totalAvailableVolume.value.compareTo(BigInteger.valueOf(totalBoxVolume)) < 0) {
			// constrained by volume
			return Collections.emptyList();
		}

		// sanity check - exact values for weight
		Limit totalAvailableWeight = calculateMaxWeight(maxCount);
		if(totalAvailableWeight.value.compareTo(BigInteger.valueOf(totalBoxWeight)) < 0) {
			// constrained by weight
			return Collections.emptyList();
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

			if(!canLoadAtLeastOneGroup(container, groups)) {
				continue;
			}
			list.add(i);
		}

		return list;
	}

	protected boolean canLoadAtLeastOneBox(Container containerBox, Iterable<BoxItem> boxes) {
		for (BoxItem boxItem : boxes) {
			Box box = boxItem.getBox();
			if(containerBox.canLoad(box)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean canLoadAll(Container containerBox, Iterable<BoxItem> boxes) {
		for (BoxItem box : boxes) {
			if(!containerBox.canLoad(box)) {
				return false;
			}
		}
		return true;
	}
	
	protected boolean canLoadAtLeastOneGroup(Container containerBox, List<BoxItemGroup> boxes) {
		for (BoxItemGroup group : boxes) {
			for (BoxItem boxItem : group.getItems()) {
				Box box = boxItem.getBox();
				if(containerBox.canLoad(box)) {
					return true;
				}
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
		return hasCapacity(containerItemsSortedByVolume, maxCount, target, true);
	}

	protected boolean hasMaxWeightCapacity(int maxCount, long target) {
		return hasCapacity(containerItemsSortedByWeight, maxCount, target, false);
	}

	private static boolean hasCapacity(List<ControlledContainerItem> items, int maxCount, long target, boolean volume) {
		long total = 0;
		for(int i = items.size() - 1; i >= 0 && maxCount > 0; i--) {
			ControlledContainerItem item = items.get(i);
			if(!item.isAvailable()) {
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
