package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;
import com.github.skjolber.packing.packer.BoxItemsContainerItemsCalculator;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.ControlledContainerItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public abstract class AbstractBruteForceBoxItemPackagerAdapter extends AbstractPackagerAdapter {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected BoxItem[] boxItems;
	protected final List<BoxItem> initialBoxItems;

	public AbstractBruteForceBoxItemPackagerAdapter(List<BoxItem> boxItems, List<ControlledContainerItem> containers,
			int containerCount) {
		this(boxItems, new BoxItemsContainerItemsCalculator(containers, containerCount, boxItems));
	}

	protected AbstractBruteForceBoxItemPackagerAdapter(List<BoxItem> boxItems,
			ContainerItemsCalculator containerItemsCalculator) {
		super(containerItemsCalculator);
		this.initialBoxItems = copyBoxItems(boxItems);
		
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

	protected AbstractBruteForceBoxItemPackagerAdapter(AbstractBruteForceBoxItemPackagerAdapter source) {
		super(source);
		this.initialBoxItems = copyBoxItems(source.initialBoxItems);
		this.boxes = source.boxes.clone();
		this.boxesRemaining = source.boxesRemaining.clone();
		this.boxItems = new BoxItem[source.boxItems.length];
		for(int i = 0; i < boxItems.length; i++) {
			if(source.boxItems[i] != null) {
				boxItems[i] = source.boxItems[i].clone();
			}
		}
	}

	@Override
	public ControlledContainerItem getContainerItem(int index) {
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
	public List<Integer> getContainers() {
		return packagerContainerItems.getContainers(getRemainingBoxItems()).getContainerIndexes();
	}

	@Override
	public List<BoxItem> getRemainingBoxItems() {
		List<BoxItem> remainingBoxItems = new ArrayList<>(boxItems.length);
		for(int i = 0; i < boxItems.length; i++) {
			BoxItem boxItem = boxItems[i];
			if(boxItem != null && !boxItem.isEmpty()) {
				remainingBoxItems.add(boxItem);
			}
		}
		return remainingBoxItems;
	}

	@Override
	public long getRemainingVolume() {
		long volume = 0L;
		for(int i = 0; i < boxes.length; i++) {
			volume = Math.addExact(volume, Math.multiplyExact(boxes[i].getVolume(), boxesRemaining[i]));
		}
		return volume;
	}

	@Override
	public long getRemainingWeight() {
		long weight = 0L;
		for(int i = 0; i < boxes.length; i++) {
			weight = Math.addExact(weight, Math.multiplyExact((long)boxes[i].getWeight(), boxesRemaining[i]));
		}
		return weight;
	}

	protected BruteForceIntermediatePackagerResult copy(ControlledContainerItem peek, IntermediatePackagerResult result, int index) {
		if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {
			return new BruteForceIntermediatePackagerResult(peek, result.getStack(), index, bruteForceResult.getIterator(), bruteForceResult.isCalculateLoads());
		}
		throw new IllegalStateException();
	}

	@Override
	public int countRemainingBoxItemGroups() {
		return -1;
	}

	@Override
	public List<BoxItemGroup> getRemainingBoxItemGroups() {
		return null;
	}

}
