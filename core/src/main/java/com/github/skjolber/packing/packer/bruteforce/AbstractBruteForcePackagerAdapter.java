package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackagerAdapter;

public abstract class AbstractBruteForcePackagerAdapter extends AbstractPackagerAdapter<BruteForcePackagerResult> {

	// keep inventory over all of the iterators here
	protected Stackable[] stackables;
	protected int[] stackablesRemaining;
	
	public AbstractBruteForcePackagerAdapter(List<ContainerItem> items, List<BoxItem> stackableItems) {
		super(items);
		
		stackables = new Stackable[stackableItems.size()];
		stackablesRemaining = new int[stackableItems.size()];
		
		for(int i = 0; i < stackableItems.size(); i++) {
			BoxItem stackableItem = stackableItems.get(i);
			
			stackables[i] = stackableItem.getStackable();
			stackablesRemaining[i] = stackableItem.getCount();
		}
	} 
	
	protected void removeInventory(List<Integer> p) {
		// remove adapter inventory
		for (Integer remove : p) {
			stackablesRemaining[remove]--;
		}
	}
	
	@Override
	public List<Integer> getContainers(int maxCount) {
		List<Stackable> boxes = new ArrayList<>();
		
		for (int i = 0; i < stackables.length; i++) {
			for(int k = 0; k < stackablesRemaining[i]; k++) {
				boxes.add(stackables[i]);
			}
		}

		return getContainers(boxes, maxCount);
	}	
	
	public static boolean hasAtLeastOneContainerForEveryStackable(DefaultPermutationRotationIterator[] iterators, int size) {
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
