package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;

public abstract class AbstractBruteForceBoxItemPackagerAdapter implements PackagerAdapter<BruteForceIntermediatePackagerResult> {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected BoxItem[] boxItems;
	
	protected final ContainerItemsCalculator packagerContainerItems;

	public AbstractBruteForceBoxItemPackagerAdapter(List<BoxItem> boxItems, ContainerItemsCalculator packagerContainerItems) {
		this.packagerContainerItems = packagerContainerItems;
		
		this.boxes = new Box[boxItems.size()];
		this.boxesRemaining = new int[boxItems.size()];
		this.boxItems = new BoxItem[boxItems.size()];
		
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);
			this.boxItems[i] = boxItem;
			this.boxes[i] = boxItem.getBox();
			this.boxesRemaining[i] = boxItem.getCount();
		}
	} 
	
	@Override
	public ContainerItem getContainerItem(int index) {
		return packagerContainerItems.getContainerItem(index);
	}

	protected void removeInventory(List<Integer> p) {
		// remove adapter inventory
		for (Integer remove : p) {
			boxesRemaining[remove]--;
			boxItems[remove].decrement();
			if(boxItems[remove].isEmpty()) {
				boxItems[remove] = null;
			}
		}
	}

	@Override
	public List<Integer> getContainers(int maxCount) {
		List<BoxItem> remainingBoxItems = new ArrayList<>(boxItems.length);
		for(int i = 0; i < boxItems.length; i++) {
			BoxItem boxItem = boxItems[i];
			if(boxItem != null && !boxItem.isEmpty()) {
				remainingBoxItems.add(boxItem);
			}
		}
		return packagerContainerItems.getContainers(remainingBoxItems, maxCount);
	}
}
