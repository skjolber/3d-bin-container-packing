package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemGroupPermutationRotationIterator;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.DefaultContainerItemsCalculator;

public abstract class AbstractSingleThreadedBruteForceBoxItemGroupPackagerAdapter extends AbstractBruteForceBoxItemPackagerAdapter {

	protected final BoxItemGroupPermutationRotationIterator[] containerIterators;
	protected final ExtremePoints3DStack extremePoints;
	protected List<StackPlacement> stackPlacements;
	protected List<BoxItemGroup> boxItemGroups;

	public AbstractSingleThreadedBruteForceBoxItemGroupPackagerAdapter(List<BoxItem> boxItems, List<BoxItemGroup> boxItemGroups, BoxPriority priority, DefaultContainerItemsCalculator packagerContainerItems, BoxItemGroupPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
		super(boxItems, priority, packagerContainerItems, interrupt);
		
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

		this.extremePoints = new ExtremePoints3DStack(maxIteratorLength + 1);
		this.extremePoints.reset(1, 1, 1);
		
		this.boxItemGroups = boxItemGroups;
	}

	@Override
	public Container accept(BruteForceIntermediatePackagerResult bruteForceResult) {
		bruteForceResult.markDirty();
		Stack stack = bruteForceResult.getStack();
		
		Container container = packagerContainerItems.toContainer(bruteForceResult.getContainerItem(), stack);
					
		int size = stack.size();
		if(stackPlacements.size() > size) {
			// this result does not consume all placements
			// remove consumed items from the iterators

			PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
			
			// TODO only handles groups in order.
			
			List<Integer> removedGroups = new ArrayList<>();
			int wholeGroupBoxCount = 0;
			for(int i = 0; i < boxItemGroups.size(); i++) {
				BoxItemGroup boxItemGroup = boxItemGroups.get(i);
				
				int groupBoxCount = boxItemGroup.getBoxCount();
				if(size < wholeGroupBoxCount + groupBoxCount) {
					// the last group was not successful
					break;
				}
				
				removedGroups.add(i);
				
				wholeGroupBoxCount += groupBoxCount;
				
				if(wholeGroupBoxCount == size) {
					break;
				}
			}
			
			int[] permutations = state.getPermutations();
			List<Integer> p = new ArrayList<>(wholeGroupBoxCount);
			for (int i = 0; i < wholeGroupBoxCount; i++) {
				p.add(permutations[i]);
			}
			
			// remove adapter inventory
			removeInventory(p);

			for (BoxItemGroupPermutationRotationIterator it : containerIterators) {
				it.removeGroups(removedGroups);
			}
			
			boxItemGroups = boxItemGroups.subList(removedGroups.size(), this.boxItemGroups.size());
			stackPlacements = stackPlacements.subList(wholeGroupBoxCount, this.stackPlacements.size());
		} else {
			stackPlacements = Collections.emptyList();
			boxItemGroups = Collections.emptyList();
		}

		return container;
	}

	@Override
	public int countRemainingBoxes() {
		return stackPlacements.size();
	}

}
