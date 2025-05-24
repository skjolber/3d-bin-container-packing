package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;

public abstract class AbstractBruteForcePackagerAdapter extends AbstractPackagerAdapter<BruteForceIntermediatePackagerResult> {

	// keep inventory over all of the iterators here
	protected Box[] boxes;
	protected int[] boxesRemaining;
	protected List<BoxItem> boxItems;
	
	public AbstractBruteForcePackagerAdapter(List<CompositeContainerItem> items, List<BoxItem> boxItems) {
		super(items);
		
		boxes = new Box[boxItems.size()];
		boxesRemaining = new int[boxItems.size()];
		
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem stackableItem = boxItems.get(i);
			
			boxes[i] = stackableItem.getBox();
			boxesRemaining[i] = stackableItem.getCount();
		}
		
		this.boxItems = new ArrayList<>(boxItems);
	} 
	
	protected void removeInventory(List<Integer> p) {
		// remove adapter inventory
		for (Integer remove : p) {
			boxesRemaining[remove]--;
		}
	}
	
	@Override
	public List<Integer> getContainers(int maxCount) {
		return getContainers(boxItems, maxCount);
	}	
	
	public static boolean hasAtLeastOneContainerForEveryBox(DefaultPermutationRotationIterator[] iterators, int size) {
		// check that all boxes fit in one or more container(s)
		// otherwise do not attempt packaging
		boolean[] containerChecklist = new boolean[size]; 
		for (DefaultPermutationRotationIterator iterator : iterators) {
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
