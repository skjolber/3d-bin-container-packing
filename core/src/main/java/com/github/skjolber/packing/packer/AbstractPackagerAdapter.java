package com.github.skjolber.packing.packer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.PackResult;

public abstract class AbstractPackagerAdapter<T extends IntermediatePackagerResult> implements PackagerAdapter<T> {

	protected final List<CompositeContainerItem> containerItems;

	protected long maxContainerLoadVolume = 0;
	protected long maxContainerLoadWeight = 0;

	public AbstractPackagerAdapter(List<CompositeContainerItem> items) {
		this.containerItems = items;

		calculateMaxLoadVolume();
		calculateMaxLoadWeight();
	}

	private void calculateMaxLoadVolume() {
		maxContainerLoadVolume = 0;

		for (CompositeContainerItem packContainerItem: containerItems) {
			
			ContainerItem item = packContainerItem.getContainerItem();
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
		maxContainerLoadWeight = 0;

		for (CompositeContainerItem packContainerItem: containerItems) {
			ContainerItem item = packContainerItem.getContainerItem();
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
	
	protected List<Integer> getContainers(List<BoxItem> boxes, int maxCount) {
		long totalVolume = 0;
		long totalWeight = 0;

		for (BoxItem box : boxes) {
			// volume
			totalVolume += box.getVolume();

			// weight
			totalWeight += box.getWeight();
		}

		if(maxContainerLoadVolume * maxCount < totalVolume || maxContainerLoadWeight * maxCount < totalWeight) {
			// no containers will work at current count
			return Collections.emptyList();
		}

		List<Integer> list = new ArrayList<>(containerItems.size());

		if(maxCount == 1) {

			containers: 
			for (int i = 0; i < containerItems.size(); i++) {
				ContainerItem item = containerItems.get(i).getContainerItem();
				if(!item.isAvailable()) {
					continue;
				}

				Container container = item.getContainer();

				if(container.getMaxLoadVolume() < totalVolume) {
					continue;
				}
				if(container.getMaxLoadWeight() < totalWeight) {
					continue;
				}

				for (BoxItem box : boxes) {
					if(!container.canLoad(box.getBox())) {
						continue containers;
					}
				}
				list.add(i);
			}

		} else {
			long minVolume = Long.MAX_VALUE;
			long minWeight = Long.MAX_VALUE;

			for (BoxItem box : boxes) {
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
				ContainerItem item = containerItems.get(i).getContainerItem();

				if(!item.isAvailable()) {
					continue;
				}

				Container container = item.getContainer();

				if(container.getMaxLoadVolume() < minVolume || container.getMaxLoadWeight() < minWeight) {
					// this container cannot even fit a single box
					continue;
				}

				if(container.getMaxLoadVolume() + maxContainerLoadVolume * (maxCount - 1) < totalVolume || container.getMaxLoadWeight() + maxContainerLoadWeight * (maxCount - 1) < totalWeight) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

				if(!canLoadAtLeastOneBox(container, boxes)) {
					continue;
				}
				list.add(i);
			}
		}

		return list;
	}
	

	protected List<Integer> getGroupContainers(List<BoxItemGroup> boxes, int maxCount) {
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

		if(maxContainerLoadVolume * maxCount < totalBoxVolume || maxContainerLoadWeight * maxCount < totalBoxWeight) {
			// no containers will work at current count
			return Collections.emptyList();
		}

		List<Integer> list = new ArrayList<>(containerItems.size());

		if(maxCount == 1) {

			// check if everything can fit in the same container
			
			containers: 
			for (int i = 0; i < containerItems.size(); i++) {
				CompositeContainerItem packContainerItem = containerItems.get(i);
				
				ContainerItem item = packContainerItem.getContainerItem();
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
			for (int i = 0; i < containerItems.size(); i++) {
				CompositeContainerItem packContainerItem = containerItems.get(i);

				ContainerItem item = packContainerItem.getContainerItem();
				
				if(!item.isAvailable()) {
					continue;
				}

				Container container = item.getContainer();

				if(container.getMaxLoadVolume() < minGroupVolume || container.getMaxLoadWeight() < minGroupWeight) {
					// this container cannot even fit a single group
					continue;
				}

				if(container.getMaxLoadVolume() + maxContainerLoadVolume * (maxCount - 1) < totalBoxVolume || container.getMaxLoadWeight() + maxContainerLoadWeight * (maxCount - 1) < totalBoxWeight) {
					// this container cannot be used even together with all biggest boxes
					continue;
				}

				if(!canLoadAtLeastOneGroup(container, boxes)) {
					continue;
				}
				list.add(i);
			}
		}

		return list;
	}
	
	protected boolean canLoadAtLeastOneBox(Container containerBox, List<BoxItem> boxes) {
		
		for (BoxItem boxItem : boxes) {
			Box box = boxItem.getBox();
			if(containerBox.canLoad(box)) {
				return true;
			}
		}
		return false;
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
		
		Container container = item.getContainer();
		
		return new Container(container.getId(), container.getDescription(), 
				container.getDx(), container.getDy(), container.getDz(), 
				
				container.getEmptyWeight(), 
				
				container.getLoadDx(), container.getLoadDy(), container.getLoadDz(), 
				
				container.getMaxLoadWeight(), stack);
	}
	
	@Override
	public ContainerItem getContainerItem(int index) {
		return containerItems.get(index).getContainerItem();
	}

}
