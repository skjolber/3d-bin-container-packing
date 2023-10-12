package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationState;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;
import com.github.skjolber.packing.packer.PackagerAdapter;

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

	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}

	public static class BruteForcePackagerBuilder extends AbstractPackagerBuilder<BruteForcePackager, BruteForcePackagerBuilder> {

		public BruteForcePackager build() {
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new BruteForcePackager(checkpointsPerDeadlineCheck, packResultComparator);
		}
	}

	private class BruteForceAdapter extends AbstractBruteForcePackagerAdapter {

		private final ContainerStackValue[] containerStackValue;
		private final DefaultPermutationRotationIterator[] iterators;
		private final PackagerInterruptSupplier interrupt;
		private final ExtremePoints3DStack extremePoints3D;
		private List<StackPlacement> stackPlacements;

		public BruteForceAdapter(List<ContainerItem> containers, ContainerStackValue[] containerStackValue, DefaultPermutationRotationIterator[] iterators, List<StackableItem> stackableItems, PackagerInterruptSupplier interrupt) {
			super(containers, stackableItems);
			
			this.containerStackValue = containerStackValue;
			this.iterators = iterators;
			this.interrupt = interrupt;
			
			int maxIteratorLength = 0;
			for (DefaultPermutationRotationIterator iterator : iterators) {
				maxIteratorLength = Math.max(maxIteratorLength, iterator.length());
			}
			
			int stackableCount = 0;
			for(int i = 0; i < stackableItems.size(); i++) {
				StackableItem stackableItem = stackableItems.get(i);
				stackableCount += stackableItem.getCount();
			}
			
			this.stackPlacements = getPlacements(stackableCount);

			this.extremePoints3D = new ExtremePoints3DStack(1, 1, 1, maxIteratorLength + 1);
		}

		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult best) {
			if(iterators[i].length() == 0) {
				return BruteForcePackagerResult.EMPTY;
			}
			// TODO break if this container cannot beat the existing best result
			return BruteForcePackager.this.pack(extremePoints3D, stackPlacements, containerItems.get(i).getContainer(), i, containerStackValue[i], iterators[i], interrupt);
		}

		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			super.accept(bruteForceResult.getContainerItemIndex());

			Container container = bruteForceResult.getContainer();
			Stack stack = container.getStack();

			int size = stack.getSize();
			if(stackPlacements.size() > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators

				PermutationRotationState state = bruteForceResult.getPermutationRotationIteratorForState();

				int[] permutations = state.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}

				for (PermutationRotationIterator it : iterators) {
					it.removePermutations(p);
				}
				
				// remove adapter inventory
				removeInventory(p);
				
				stackPlacements = stackPlacements.subList(size, this.stackPlacements.size());
			} else {
				stackPlacements = Collections.emptyList();
			}

			return container;
		}

	}

	public BruteForcePackager(int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		super(checkpointsPerDeadlineCheck, packResultComparator);
	}

	@Override
	protected PackagerAdapter<BruteForcePackagerResult> adapter(List<StackableItem> stackableItems, List<ContainerItem> containers, PackagerInterruptSupplier interrupt) {
		DefaultPermutationRotationIterator[] iterators = new DefaultPermutationRotationIterator[containers.size()];
		ContainerStackValue[] containerStackValue = new ContainerStackValue[containers.size()];

		for (int i = 0; i < containers.size(); i++) {
			ContainerItem containerItem = containers.get(i);
			Container container = containerItem.getContainer();

			ContainerStackValue stackValue = container.getStackValues()[0];

			containerStackValue[i] = stackValue;

			StackConstraint constraint = stackValue.getConstraint();

			Dimension dimension = new Dimension(stackValue.getLoadDx(), stackValue.getLoadDy(), stackValue.getLoadDz());

			iterators[i] = DefaultPermutationRotationIterator
					.newBuilder()
					.withLoadSize(dimension)
					.withStackableItems(stackableItems)
					.withMaxLoadWeight(stackValue.getMaxLoadWeight())
					.withFilter(stackable -> constraint == null || constraint.canAccept(stackable))
					.build();
		}
		
		// check that all boxes fit in one or more container(s)
		// otherwise do not attempt packaging
		if(!AbstractBruteForcePackagerAdapter.hasAtLeastOneContainerForEveryStackable(iterators, stackableItems.size())) {
			return null;
		}
		
		return new BruteForceAdapter(containers, containerStackValue, iterators, stackableItems, interrupt);
	}

}
