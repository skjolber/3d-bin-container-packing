package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.Priority;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * This implementation tries all permutations, rotations and points.
 * <br>
 * <br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class BruteForcePackager extends AbstractBruteForcePackager {

	// implementation notes:
	//  * new BoxItemControlsBuilder / BoxItemGroupControls per permutation 
	//
	// placement results:
	//  * comparing box items is not necessary
	//  * comparing groups is not necessary
	//  * finding best point is not necessary
	//
	// filtering:
	// boxes: (resets for each permutation)
	//  * box items
	//  * box item groups 
	//
	// points: (resets for each box)
	//  * finding best point is not necessary
	//  * filtering available points per box item is necessary
	//
	// strategy:
	//  * find first result (current implementation)
	//  * find best result according to comparator (future implementation)
	//
	
	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}

	public static class BruteForcePackagerBuilder extends AbstractPackagerBuilder<BruteForcePackager, BruteForcePackagerBuilder> {

		protected IntermediatePackagerResultComparator comparator;
		
		public BruteForcePackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new BruteForcePackager(comparator);
		}
	}

	private class BruteForceAdapter extends AbstractBruteForcePackagerAdapter {

		private final BoxItemPermutationRotationIterator[] containerIterators;
		private final PackagerInterruptSupplier interrupt;
		private final ExtremePoints3DStack extremePoints;
		private List<StackPlacement> stackPlacements;

		public BruteForceAdapter(List<CompositeContainerItem> containerItems, List<BoxItem> boxItems, BoxItemPermutationRotationIterator[] containerIterators, PackagerInterruptSupplier interrupt) {
			super(containerItems, boxItems);
			
			this.containerIterators = containerIterators;
			this.interrupt = interrupt;
			
			int maxIteratorLength = 0;
			for (BoxItemPermutationRotationIterator iterator : containerIterators) {
				maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
			}
			
			int count = 0;
			for(int i = 0; i < boxItems.size(); i++) {
				BoxItem stackableItem = boxItems.get(i);
				count += stackableItem.getCount();
			}
			
			this.stackPlacements = getPlacements(count);

			this.extremePoints = new ExtremePoints3DStack(maxIteratorLength + 1);
			this.extremePoints.reset(1, 1, 1);
		}

		@Override
		public Container accept(BruteForceIntermediatePackagerResult bruteForceResult) {
			bruteForceResult.markDirty();
			Stack stack = bruteForceResult.getStack();
			
			Container container = super.toContainer(bruteForceResult.getContainerItem(), stack);
						
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
		}

		@Override
		public BruteForceIntermediatePackagerResult attempt(int i, BruteForceIntermediatePackagerResult best) throws PackagerInterruptedException {
			if(containerIterators[i].length() == 0) {
				return null;
			}
			return BruteForcePackager.this.pack(extremePoints, stackPlacements, containerItems.get(i).getContainerItem(), i, containerIterators[i], interrupt);
		}

		@Override
		public int countRemainingBoxes() {
			return stackPlacements.size();
		}

	}

	public BruteForcePackager(IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);
	}

	@Override
	protected BruteForceAdapter adapter(List<BoxItem> boxItems, Priority priority, List<CompositeContainerItem> containers, PackagerInterruptSupplier interrupt) {
		BoxItemPermutationRotationIterator[] containerIterators = new DefaultBoxItemPermutationRotationIterator[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			CompositeContainerItem compositeContainerItem = containers.get(i);
			ContainerItem containerItem = compositeContainerItem.getContainerItem();
			Container container = containerItem.getContainer();

			Dimension dimension = new Dimension(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());

			containerIterators[i] = DefaultBoxItemPermutationRotationIterator
					.newBuilder()
					.withLoadSize(dimension)
					.withBoxItems(boxItems)
					.withMaxLoadWeight(container.getMaxLoadWeight())
					.build();
		}
		
		// check that all boxes fit in one or more container(s)
		// otherwise do not attempt packaging
		if(!AbstractBruteForcePackagerAdapter.hasAtLeastOneContainerForEveryBox(containerIterators, boxItems.size())) {
			return null;
		}
		
		return new BruteForceAdapter(containers, boxItems, containerIterators,  interrupt);
	}

	@Override
	protected PackagerAdapter<BruteForceIntermediatePackagerResult> groupAdapter(List<BoxItemGroup> boxes, List<CompositeContainerItem> containers, Priority order, PackagerInterruptSupplier interrupt) {
		return null;
	}

	@Override
	protected boolean acceptAsFull(BruteForceIntermediatePackagerResult result, Container holder) {
		return result.getLoadVolume() == holder.getMaxLoadVolume();
	}

}
