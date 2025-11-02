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

public class ContainerItemsCalculator {

	public static class Limit {
		
		BigInteger value;
		Set<Integer> containerIndexes;
		long minimum;
		
		public Limit(BigInteger value, Set<Integer> containerIndexes, long minimum) {
			super();
			this.value = value;
			this.containerIndexes = containerIndexes;
			this.minimum = minimum;
		}
	}
	
	protected final List<ControlledContainerItem> containerItems;
	protected final List<ControlledContainerItem> containerItemsSortedByWeight;
	protected final List<ControlledContainerItem> containerItemsSortedByVolume;
	
	/**
	 * Create new instance
	 * 
	 * @param items container items.
	 */
	
	public ContainerItemsCalculator(List<ControlledContainerItem> items) {
		for(int i = 0; i < items.size(); i++) {
			items.get(i).setIndex(i);
		}
		this.containerItems = items;
		
		containerItemsSortedByWeight = new ArrayList<>(items);
		containerItemsSortedByVolume = new ArrayList<>(items);
		
		Collections.sort(containerItemsSortedByWeight, ContainerItem.MAX_LOAD_WEIGHT_COMPARATOR);
		Collections.sort(containerItemsSortedByVolume, ContainerItem.MAX_LOAD_VOLUME_COMPARATOR);
	}

	/**
	 * Return a list of containers which can potentially hold the boxes within the provided count
	 *
	 * @param boxes    list of boxes
	 * @param maxCount maximum number of possible containers
	 * @return list of containers
	 */
	
	public List<Integer> getContainers(List<BoxItem> boxes, int maxCount) {
		long totalVolume = 0;
		long totalWeight = 0;

		for (BoxItem box : boxes) {
			// volume
			totalVolume += box.getVolume();

			// weight
			totalWeight += box.getWeight();
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

		List<Integer> result = new ArrayList<>(getContainerItemCount());
		if(maxCount == 1) {

			for (int i = 0; i < getContainerItemCount(); i++) {
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

		} else {
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
			
			for (int i = 0; i < getContainerItemCount(); i++) {
				ContainerItem item = getContainerItem(i);

				if(!item.isAvailable()) {
					continue;
				}

				Container container = item.getContainer();
				if(container.getMaxLoadVolume() < minVolume || container.getMaxLoadWeight() < minWeight) {
					// this container cannot even fit a single box
					continue;
				}

				// santiy-check use of this container
				// corner case: can we exchange the a bigger container for the current and still have enough weight / volume?
				if(!totalAvailableVolume.containerIndexes.contains(item.getIndex())) {
					long reduction = totalAvailableVolume.minimum - container.getMaxLoadVolume();
					
					BigInteger maxAvailableVolumeWithThisContainer = totalAvailableVolume.value.subtract(BigInteger.valueOf(reduction));
					if(maxAvailableVolumeWithThisContainer.compareTo(BigInteger.valueOf(totalVolume)) < 0) {
						// this container cannot be used even together with all biggest boxes
						continue;
					}
				}

				if(!totalAvailableWeight.containerIndexes.contains(item.getIndex())) {
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
		}

		return result;
	}

	public List<Integer> getGroupContainers(List<BoxItemGroup> boxes, int maxCount) {
		long totalBoxVolume = 0;
		long totalBoxWeight = 0;

		long maxGroupVolume = 0;
		long maxGroupWeight = 0;

		long minGroupVolume = Long.MAX_VALUE;
		long minGroupWeight = Long.MAX_VALUE;

		for (BoxItemGroup group : boxes) {
			// volume
			for (BoxItem boxItem : group.getItems()) {

				long volume = boxItem.getVolume();
				if(maxGroupVolume < volume) {
					maxGroupVolume = volume;
				}
				if(minGroupVolume > volume) {
					minGroupVolume = volume;
				}
				long weight = boxItem.getWeight();
				if(maxGroupWeight < weight) {
					maxGroupWeight = weight;
				}
				if(minGroupWeight > weight) {
					minGroupWeight = weight;
				}
				
				totalBoxVolume += volume;

				// weight
				totalBoxWeight += weight;

			}
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

		if(maxCount == 1) {

			// check if everything can fit in the same container
			containers: 
			for (int i = 0; i < getContainerItemCount(); i++) {
				ContainerItem item = getContainerItem(i);
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
				
				for (BoxItemGroup group : boxes) {
					if(!c.canLoad(group)) {
						continue containers;
					}
				}
				list.add(i);
			}

		} else {
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
				
				// santiy-check use of this container
				// corner case: can we exchange the a bigger container for the current and still have enough weight / volume?
				if(!totalAvailableVolume.containerIndexes.contains(item.getIndex())) {
					long reduction = totalAvailableVolume.minimum - container.getMaxLoadVolume();
					
					BigInteger maxAvailableVolumeWithThisContainer = totalAvailableVolume.value.subtract(BigInteger.valueOf(reduction));
					if(maxAvailableVolumeWithThisContainer.compareTo(BigInteger.valueOf(totalBoxVolume)) < 0) {
						// this container cannot be used even together with all biggest boxes
						continue;
					}
				}

				if(!totalAvailableWeight.containerIndexes.contains(item.getIndex())) {
					long reduction = totalAvailableWeight.minimum - container.getMaxLoadWeight();
					
					BigInteger maxAvailableWeightWithThisContainer = totalAvailableWeight.value.subtract(BigInteger.valueOf(reduction));
					if(maxAvailableWeightWithThisContainer.compareTo(BigInteger.valueOf(totalBoxWeight)) < 0) {
						// this container cannot be used even together with all biggest boxes
						continue;
					}
					
				}

				if(!canLoadAtLeastOneGroup(container, boxes)) {
					continue;
				}
				list.add(i);
			}
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

		Container container = item.getContainer();
		
		return new Container(container.getId(), container.getDescription(), 
				container.getDx(), container.getDy(), container.getDz(), 
				
				container.getEmptyWeight(), 
				
				container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), 
				
				container.getMaxLoadWeight(), stack);
	}

	protected Limit calculateMaxVolume(int maxCount) {
		Set<Integer> includedContainerIndexes = new HashSet<>();
		
		long minVolume = Long.MAX_VALUE;
		
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
			
			if(minVolume > containerItem.getContainer().getMaxLoadVolume()) {
				minVolume = containerItem.getContainer().getMaxLoadVolume();
			}
		}
		
		return new Limit(volume, includedContainerIndexes, minVolume);
	}

	protected Limit calculateMaxWeight(int maxCount) {
		Set<Integer> includedContainerIndexes = new HashSet<>();
		
		long minWeight = Long.MAX_VALUE;
		
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
			
			
			if(minWeight > containerItem.getContainer().getMaxLoadWeight()) {
				minWeight = containerItem.getContainer().getMaxLoadWeight();
			}
		}
		
		return new Limit(weight, includedContainerIndexes, minWeight);
	}

	public int getContainerItemCount() {
		return containerItems.size();
	}
	
	public ControlledContainerItem getContainerItem(int index) {
		return containerItems.get(index);
	}
	
	public List<ControlledContainerItem> getContainerItems() {
		return containerItems;
	}

}
