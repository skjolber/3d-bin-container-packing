package com.github.skjolber.packing.packer.bruteforce;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;

public abstract class AbstractBruteForcePackagerAdapter extends AbstractPackagerAdapter<BruteForceIntermediatePackagerResult> {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected BoxItem[] boxItems;
	protected FilteredBoxItems filteredBoxItems;
	
	public AbstractBruteForcePackagerAdapter(List<CompositeContainerItem> items, List<BoxItem> boxItems) {
		super(items);
		
		this.boxes = new Box[boxItems.size()];
		this.boxesRemaining = new int[boxItems.size()];
		this.boxItems = new BoxItem[boxItems.size()];
		
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);

			this.boxItems[i] = boxItem;
			this.boxes[i] = boxItem.getBox();
			this.boxesRemaining[i] = boxItem.getCount();
		}
		
		this.filteredBoxItems = new DefaultFilteredBoxItems(boxItems);
	} 
	
	protected void removeInventory(List<Integer> p) {
		// remove adapter inventory
		for (Integer remove : p) {
			boxesRemaining[remove]--;
			
			boxItems[remove].decrement();
		}
		filteredBoxItems.removeEmpty();
	}
	
	@Override
	public List<Integer> getContainers(int maxCount) {
		return getContainers(filteredBoxItems, maxCount);
	}	
	
	public static boolean hasAtLeastOneContainerForEveryBox(BoxItemPermutationRotationIterator[] iterators, int size) {
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
	
	

}
