package com.github.skjolber.packing.packer;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.Stackable;

public abstract class AbstractPackagerAdapter<T extends PackResult> implements PackagerAdapter<T> {

	protected final List<ContainerItem> containerItems;

	protected long maxContainerLoadVolume = Long.MIN_VALUE;
	protected long maxContainerLoadWeight = Long.MIN_VALUE;

	public AbstractPackagerAdapter(List<ContainerItem> items) {
		this.containerItems = items;

		calculateMaxLoadVolume();
		calculateMaxLoadWeight();
	}

	private void calculateMaxLoadVolume() {
		maxContainerLoadVolume = Long.MIN_VALUE;

		for (ContainerItem item : containerItems) {
			if(!item.isAvailable()) {
				continue;
			}
			Container container = item.getContainer();
			// volume
			long boxVolume = container.getVolume();
			if(boxVolume > maxContainerLoadVolume) {
				maxContainerLoadVolume = boxVolume;
			}
		}
	}

	private void calculateMaxLoadWeight() {
		maxContainerLoadWeight = Long.MIN_VALUE;

		for (ContainerItem item : containerItems) {
			if(!item.isAvailable()) {
				continue;
			}
			Container container = item.getContainer();
			// weight
			long boxWeight = container.getMaxLoadWeight();
			if(boxWeight > maxContainerLoadWeight) {
				maxContainerLoadWeight = boxWeight;
			}
		}
	}

	/**
	 * Return a list of containers which can potentially hold the boxes within the provided count
	 *
	 * @param boxes    list of boxes
	 * @param maxCount maximum number of possible containers
	 * @return list of containers
	 */

	protected List<Integer> getContainers(List<Stackable> boxes, int maxCount) {
		long volume = 0;
		long weight = 0;

		for (Stackable box : boxes) {
			// volume
			volume += box.getVolume();

			// weight
			weight += box.getWeight();
		}
		
		// sanity check - cheap rough estimate volume
		BigInteger totalAvailableVolumeForMaxContainer = BigInteger.valueOf(maxContainerLoadVolume).multiply(BigInteger.valueOf(maxCount));
		if(totalAvailableVolumeForMaxContainer.compareTo(BigInteger.valueOf(volume)) < 0) {
			// constrained at volume even if using only the biggest container
			return Collections.emptyList();
		}

		// sanity check - cheap rough estimate weight
		BigInteger totalAvailableWeightForMaxContainer = BigInteger.valueOf(maxContainerLoadWeight).multiply(BigInteger.valueOf(maxCount));
		if(totalAvailableWeightForMaxContainer.compareTo(BigInteger.valueOf(weight)) < 0) {
			// constrained at weight even if using only the biggest container
			return Collections.emptyList();
		}

		// sanity check - exact values for volume
		BigInteger totalAvailableVolume = calculateMaxVolume(maxCount);
		if(totalAvailableVolume.compareTo(BigInteger.valueOf(volume)) < 0) {
			// constrained by volume
			return Collections.emptyList();
		}

		// sanity check - exact values for weight
		BigInteger totalAvailableWeight = calculateMaxWeight(maxCount);
		if(totalAvailableWeight.compareTo(BigInteger.valueOf(weight)) < 0) {
			// constrained by weight
			return Collections.emptyList();
		}

		List<Integer> result = new ArrayList<>(containerItems.size());

		if(maxCount == 1) {

			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = containerItems.get(i);
				if(!item.isAvailable()) {
					continue;
				}

				// this container must be able to load all boxes
				Container container = item.getContainer();

				if(container.getMaxLoadVolume() < volume) {
					continue;
				}
				if(container.getMaxLoadWeight() < weight) {
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

			for (Stackable box : boxes) {
				// volume
				long boxVolume = box.getVolume();
				if(boxVolume < minVolume) {
					minVolume = boxVolume;
				}

				// weight
				long boxWeight = box.getWeight();
				if(boxWeight < minWeight) {
					minWeight = boxWeight;
				}
			}
			
			BigInteger totalAvailableVolumeMinusBiggestContainer = totalAvailableWeight.subtract(BigInteger.valueOf(maxContainerLoadVolume));
			BigInteger totalAvailableWeightMinusBiggestContainer = totalAvailableVolume.subtract(BigInteger.valueOf(maxContainerLoadWeight));

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

				// santiy-check use of this container
				// corner case: can we exchange the largest available container for the current and still have enough weight / volume?
				BigInteger maxAvailabaleVolumeWithThisContainer = totalAvailableVolumeMinusBiggestContainer.add(BigInteger.valueOf(container.getMaxLoadVolume()));
				if(maxAvailabaleVolumeWithThisContainer.compareTo(BigInteger.valueOf(volume)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}
				
				BigInteger maxAvailabaleWeightWithThisContainer = totalAvailableWeightMinusBiggestContainer.add(BigInteger.valueOf(container.getMaxLoadWeight()));
				if(maxAvailabaleWeightWithThisContainer.compareTo(BigInteger.valueOf(weight)) < 0) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

				// must be able to load at least one
				if(!canLoadAtLeastOne(container, boxes)) {
					continue;
				}
				result.add(i);
			}
		}

		return result;
	}

	protected BigInteger calculateMaxVolume(int maxCount) {
		List<ContainerItem> availableContainers = new ArrayList<>(containerItems.size());
		for (ContainerItem item : containerItems) {
			if(!item.isAvailable()) {
				continue;
			}
			availableContainers.add(item);
		}
		
		Collections.sort(availableContainers, ContainerItem.MAX_LOAD_VOLUME_COMPARATOR);
		
		BigInteger volume = BigInteger.valueOf(0);
		for(int i = availableContainers.size() - 1; i >= 0 && maxCount > 0; i--) {
			ContainerItem containerItem = availableContainers.get(i);
			int count = Math.min(maxCount, containerItem.getCount());
			
			BigInteger max = BigInteger.valueOf(containerItem.getContainer().getMaxLoadVolume()).multiply(BigInteger.valueOf(count));
			
			volume = volume.add(max);
			
			maxCount -= count;
		}
		
		return volume;
	}

	protected BigInteger calculateMaxWeight(int maxCount) {
		List<ContainerItem> availableContainers = new ArrayList<>(containerItems.size());
		for (ContainerItem item : containerItems) {
			if(!item.isAvailable()) {
				continue;
			}
			availableContainers.add(item);
		}
		
		Collections.sort(availableContainers, ContainerItem.MAX_LOAD_WEIGHT_COMPARATOR);
		
		BigInteger weight = BigInteger.valueOf(0);
		for(int i = availableContainers.size() - 1; i >= 0 && maxCount > 0; i--) {
			ContainerItem containerItem = availableContainers.get(i);
			int count = Math.min(maxCount, containerItem.getCount());

			BigInteger max = BigInteger.valueOf(containerItem.getContainer().getMaxLoadWeight()).multiply(BigInteger.valueOf(count));
			
			weight = weight.add(max);
			
			maxCount -= count;
		}
		
		return weight;
	}

	protected boolean canLoadAtLeastOne(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if(containerBox.canLoad(box)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean canLoadAll(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if(!containerBox.canLoad(box)) {
				return false;
			}
		}
		return true;
	}

	public void accept(int index) {
		ContainerItem item = containerItems.get(index);

		item.consume();

		// do we need to adjust limits?
		if(!item.isAvailable()) {
			Container container = item.getContainer();

			if(container.getMaxLoadVolume() == maxContainerLoadVolume) {
				calculateMaxLoadVolume();
			}
			if(container.getMaxLoadWeight() == maxContainerLoadWeight) {
				calculateMaxLoadWeight();
			}
		}
	}

}
