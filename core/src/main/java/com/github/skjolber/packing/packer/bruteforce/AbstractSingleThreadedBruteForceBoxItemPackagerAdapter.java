package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.ContainerItemsCalculator;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;

public abstract class AbstractSingleThreadedBruteForceBoxItemPackagerAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

	protected final BoxItemPermutationRotationIterator[] containerIterators;
	protected List<Placement> stackPlacements;
	protected final PackagerInterruptSupplier interrupt;

	public AbstractSingleThreadedBruteForceBoxItemPackagerAdapter(List<BoxItem> boxItems, ContainerItemsCalculator packagerContainerItems, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
		super(boxItems, packagerContainerItems);
		this.interrupt = interrupt;
		this.containerIterators = containerIterators;
		
		int maxIteratorLength = 0;
		for (BoxItemPermutationRotationIterator iterator : containerIterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		int count = 0;
		for(int i = 0; i < boxItems.size(); i++) {
			BoxItem stackableItem = boxItems.get(i);
			count += stackableItem.getCount();
		}
		
		this.stackPlacements = BruteForcePackager.getPlacements(count);
	}
	
	protected int getMaxIteratorLength() {
		int maxIteratorLength = 0;
		for (BoxItemPermutationRotationIterator iterator : containerIterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		return maxIteratorLength;
	}
	
	@Override
	public Container accept(IntermediatePackagerResult result) {
		
		if(result instanceof BruteForceIntermediatePackagerResult bruteForceResult) {

			bruteForceResult.markDirty();
			Stack stack = bruteForceResult.getStack();
			
			Container container = packagerContainerItems.toContainer(bruteForceResult.getContainerItem(), stack);
						
			int size = stack.size();
			if(stackPlacements.size() > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators
	
				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
	
				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}
				
				// remove adapter inventory
				removeInventory(p);
	
				for (BoxItemPermutationRotationIterator it : containerIterators) {
					it.removePermutations(p);
				}
				
				stackPlacements = stackPlacements.subList(size, this.stackPlacements.size());
			} else {
				stackPlacements = Collections.emptyList();
			}
	
			return container;
		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public int countRemainingBoxes() {
		return stackPlacements.size();
	}

}