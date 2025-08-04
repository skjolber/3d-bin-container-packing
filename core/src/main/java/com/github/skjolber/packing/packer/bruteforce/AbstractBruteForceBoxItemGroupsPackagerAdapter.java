package com.github.skjolber.packing.packer.bruteforce;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.packer.DefaultContainerItemsCalculator;
import com.github.skjolber.packing.packer.PackagerAdapter;

public abstract class AbstractBruteForceBoxItemGroupsPackagerAdapter implements PackagerAdapter<BruteForceIntermediatePackagerResult> {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected BoxItem[] boxItems;
	protected List<BoxItemGroup> boxItemGroups;
	
	protected final PackagerInterruptSupplier interrupt;
	protected final BoxPriority priority;
	protected final DefaultContainerItemsCalculator packagerContainerItems;

	public AbstractBruteForceBoxItemGroupsPackagerAdapter(List<BoxItemGroup> boxItemGroups, List<BoxItem> boxItems, BoxPriority priority, DefaultContainerItemsCalculator packagerContainerItems, PackagerInterruptSupplier interrupt) {
		this.boxItemGroups = boxItemGroups;
		this.packagerContainerItems = packagerContainerItems;
		this.priority = priority;
		this.interrupt = interrupt;
		
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
	
	public int getFirstBoxItemIndexForGroup(int groupIndex) {
		int index = 0;
		for(int i = 0; i < groupIndex; i++) {
			index += boxItemGroups.get(index).size();
		}
		return index;
	}

	protected void removeInventory(List<Integer> p) {
		// remove adapter inventory
		for (Integer remove : p) {
			boxesRemaining[remove]--;
			
			boxItems[remove].decrement();
		}
	}

	public static boolean hasAtLeastOneContainerForEveryGroup(BoxItemPermutationRotationIterator[] iterators, int size) {
		// check that all boxes fit in one or more container(s)
		// otherwise do not attempt packaging
		boolean[] containerChecklist = new boolean[size]; 
		for (BoxItemPermutationRotationIterator iterator : iterators) {
			int[] fits = iterator.getPermutations();
			for(int fit : fits) {
				containerChecklist[fit] = true;
			}
		}
		
		for(int i = 0; i < containerChecklist.length; i++) {
			if(!containerChecklist[i]) {
				// so the result can never be complete, since at least one box does not fit in any of the containers
				return false;
			}
		}
		return true;
	}
	
	@Override
	public List<Integer> getContainers(int maxCount) {
		
		List<BoxItem> remainingBoxItems = new ArrayList<>(boxItems.length);
		for(int i = 0; i < boxItems.length; i++) {
			BoxItem boxItem = boxItems[i];
			if(boxItem != null) {
				remainingBoxItems.add(boxItem);
			}
		}
		
		return packagerContainerItems.getContainers(remainingBoxItems, maxCount);
	}
	
	

}
