package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.packager.IntermediatePackagerResult;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;

public abstract class AbstractSingleThreadedBruteForceBoxItemSession extends AbstractBruteForceBoxItemSession {

	protected BoxItemPermutationRotationIterator[] containerIterators;
	protected Placement[] stackPlacements;
	protected int stackPlacementCount;
	/** Whether the boxes have load limits: then the placements track loads */
	protected final boolean load;
	protected final PackagerInterruptSupplier interrupt;

	public AbstractSingleThreadedBruteForceBoxItemSession(List<RemainingBoxItem> boxItems, List<ContainerItem> containers,
			int containerCount, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt, boolean load) {
		super(boxItems, containers, containerCount);
		this.interrupt = interrupt;
		this.containerIterators = containerIterators;
		
		int maxIteratorLength = 0;
		for (BoxItemPermutationRotationIterator iterator : containerIterators) {
			maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
		}
		
		int count = 0;
		for(int i = 0; i < boxItems.size(); i++) {
			RemainingBoxItem stackableItem = boxItems.get(i);
			count += stackableItem.getCount();
		}
		
		this.load = load;
		this.stackPlacements = BruteForcePackager.getPlacements(count, load);
		this.stackPlacementCount = count;
	}

	protected AbstractSingleThreadedBruteForceBoxItemSession(AbstractSingleThreadedBruteForceBoxItemSession source, boolean load) {
		super(source);
		this.interrupt = source.interrupt;
		this.containerIterators = new BoxItemPermutationRotationIterator[source.containerIterators.length];
		for(int i = 0; i < containerIterators.length; i++) {
			this.containerIterators[i] = ((DefaultBoxItemPermutationRotationIterator) source.containerIterators[i]).fork();
		}
		this.stackPlacementCount = source.stackPlacementCount;
		this.load = load;
		this.stackPlacements = BruteForcePackager.getPlacements(stackPlacementCount, load);
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
			
			Container container = packagerContainerItems.toContainer(resolveContainerItem(bruteForceResult), stack);
						
			int size = stack.size();
			if(stackPlacementCount > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators
	
				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();
	
				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}
				
				// remove session inventory
				removeInventory(p);
	
				for (BoxItemPermutationRotationIterator it : containerIterators) {
					it.removePermutations(p);
				}
				
				stackPlacementCount = BruteForcePackager.removeFirstPlacements(stackPlacements, size, stackPlacementCount);
			} else {
				stackPlacementCount = 0;
			}
	
			return container;
		} else {
			Stack stack = result.getStack();
			Container container = packagerContainerItems.toContainer(resolveContainerItem(result), stack);
			List<Integer> permutations = getLocalIndexes(stack);

			removeInventory(permutations);
			for (BoxItemPermutationRotationIterator iterator : containerIterators) {
				iterator.removePermutations(permutations);
			}
			stackPlacementCount = BruteForcePackager.removeFirstPlacements(stackPlacements, permutations.size(), stackPlacementCount);
			return container;
		}
	}

	@Override
	public int countRemainingBoxes() {
		return stackPlacementCount;
	}

}
