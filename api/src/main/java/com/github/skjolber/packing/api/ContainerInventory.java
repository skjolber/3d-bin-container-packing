package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContainerInventory {
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {
		
		private List<ContainerInventoryItem> items = new ArrayList<>();

		public Builder withUnlimited(Container ... containers) {
			for(Container container : containers) {
				items.add(new ContainerInventoryItem(container, Integer.MAX_VALUE, items.size()));
			}
			return this;
		}
		
		public Builder withUnlimited(List<Container> containers) {
			for(Container container : containers) {
				items.add(new ContainerInventoryItem(container, Integer.MAX_VALUE, items.size()));
			}
			return this;
		}
		
		public Builder withUnlimited(Container container) {
			items.add(new ContainerInventoryItem(container, Integer.MAX_VALUE, items.size()));
			return this;
		}

		public Builder withLimited(Container container, int limit) {
			items.add(new ContainerInventoryItem(container, limit, items.size()));
			return this;
		}

		public ContainerInventory build() {
			return new ContainerInventory(items);
		}
	}

	private final List<ContainerInventoryItem> items;

	protected long maxContainerLoadVolume = Long.MIN_VALUE;
	protected long maxContainerLoadWeight = Long.MIN_VALUE;

	public ContainerInventory(List<ContainerInventoryItem> items) {
		this.items = items;
		
		calculateMaxLoadVolume();
		calculateMaxLoadWeight();
	}

	private void calculateMaxLoadVolume() {
		maxContainerLoadVolume = Long.MIN_VALUE;

		for (ContainerInventoryItem item : items) {
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

		for (ContainerInventoryItem item : items) {
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

	public List<ContainerInventoryItem> getItems() {
		return items;
	}
	
	/**
	 * Return a list of containers which can potentially hold the boxes within the provided count
	 *
	 * @param boxes      list of boxes
	 * @param containers list of containers
	 * @param maxCount      maximum number of possible containers
	 * @return list of containers
	 */
	
	public List<ContainerInventoryItem> getItems(List<Stackable> boxes, int maxCount) {
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

		List<ContainerInventoryItem> list = new ArrayList<>(items.size());

		if(maxCount == 1) {
			containers: for (ContainerInventoryItem item : items) {
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
				list.add(item);
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

			for (ContainerInventoryItem item : items) {
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

				list.add(item);
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

	public void accept(ContainerInventoryItem item) {
		item.consume();
		
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
