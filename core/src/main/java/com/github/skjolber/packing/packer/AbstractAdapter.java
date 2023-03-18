package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackResult;
import com.github.skjolber.packing.api.Stackable;

public abstract class AbstractAdapter<T extends PackResult> implements Adapter<T> {
	
	protected final List<ContainerItem> containerItems;

	protected long maxContainerLoadVolume = Long.MIN_VALUE;
	protected long maxContainerLoadWeight = Long.MIN_VALUE;

	public AbstractAdapter(List<ContainerItem> items) {
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
	 * @param boxes      list of boxes
	 * @param maxCount      maximum number of possible containers
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

		if(maxContainerLoadVolume * maxCount < volume || maxContainerLoadWeight * maxCount < weight) {
			// no containers will work at current count
			return Collections.emptyList();
		}

		List<Integer> list = new ArrayList<>(containerItems.size());

		if(maxCount == 1) {
			
			containers: 
			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = containerItems.get(i);
				if(!item.isAvailable()) {
					continue;
				}
				
				Container container = item.getContainer();
				
				if(container.getMaxLoadVolume() < volume) {
					continue;
				}
				if(container.getMaxLoadWeight() < weight) {
					continue;
				}

				for (Stackable box : boxes) {
					if(!container.canLoad(box)) {
						continue containers;
					}
				}
				list.add(i);
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

				if(container.getMaxLoadVolume() + maxContainerLoadVolume * (maxCount - 1) < volume || container.getMaxLoadWeight() + maxContainerLoadWeight * (maxCount - 1) < weight) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

				if(!canLoadAtLeastOne(container, boxes)) {
					continue;
				}
				list.add(i);
			}			
		}

		return list;
	}
	
	private boolean canLoadAtLeastOne(Container containerBox, List<Stackable> boxes) {
		for (Stackable box : boxes) {
			if(containerBox.canLoad(box)) {
				return true;
			}
		}
		return false;
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
