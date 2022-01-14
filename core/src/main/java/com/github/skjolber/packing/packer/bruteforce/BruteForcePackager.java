package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.Adapter;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. 
 * This implementation tries all permutations, rotations and points.
 * <br><br>
 * Note: The brute force algorithm uses a recursive algorithm. It is not intended for more than 10 boxes.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class BruteForcePackager extends AbstractBruteForcePackager {

	public static BruteForcePackagerBuilder newBuilder() {
		return new BruteForcePackagerBuilder();
	}

	public static class BruteForcePackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;

		public BruteForcePackagerBuilder withContainers(Container ...  containers) {
			if(this.containers == null) {
				this.containers = new ArrayList<>();
			}
			for (Container container : containers) {
				this.containers.add(container);
			}
			return this;
		}
		
		public BruteForcePackagerBuilder withContainers(List<Container> containers) {
			this.containers = containers;
			return this;
		}

		public BruteForcePackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
			this.checkpointsPerDeadlineCheck = n;
			return this;
		}
		
		public BruteForcePackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			return new BruteForcePackager(containers, checkpointsPerDeadlineCheck);
		}	
	}
	
	private class BruteForceAdapter implements Adapter<BruteForcePackagerResult> {

		private DefaultPermutationRotationIterator[] iterators;
		private List<Container> containers;
		private final BooleanSupplier interrupt;
		private ExtremePoints3DStack extremePoints3D;
		private List<StackPlacement> stackPlacements;

		public BruteForceAdapter(List<StackableItem> stackableItems, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.iterators = new DefaultPermutationRotationIterator[containers.size()];
			
			for (int i = 0; i < containers.size(); i++) {
				Container container = containers.get(i);
				
				StackValue[] stackValues = container.getStackValues();
				
				iterators[i] = new DefaultPermutationRotationIterator(new Dimension(stackValues[0].getDx(), stackValues[0].getDy(), stackValues[0].getDz()), stackableItems);
			}

			this.interrupt = interrupt;
			
			int count = 0;
			for (DefaultPermutationRotationIterator iterator : iterators) {
				count = Math.max(count, iterator.length());
			}

			this.stackPlacements = getPlacements(count);
			
			this.extremePoints3D = new ExtremePoints3DStack(1, 1, 1, count + 1);
			this.extremePoints3D.setMinVolume(getMinStackableItemVolume(stackableItems));
		}

		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult best) {
			if(iterators[i].length() == 0) {
				return BruteForcePackagerResult.EMPTY;
			}
			// TODO break if this container cannot beat the existing best result
			
			return BruteForcePackager.this.pack(extremePoints3D, stackPlacements, containers.get(i), iterators[i], interrupt);
		}

		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			Container container = bruteForceResult.getContainer();
			if (!bruteForceResult.containsLastStackable()) {
				// this result does not consume all placements
				// remove consumed items from the iterators
				
				int size = container.getStack().getSize();
				
				PermutationRotationIterator iterator = bruteForceResult.getPermutationRotationIteratorForState();
				
				int[] permutations = iterator.getPermutations();
				List<Integer> p = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					p.add(permutations[i]);
				}
				
				for (PermutationRotationIterator it : iterators) {
					if (it == bruteForceResult.getPermutationRotationIteratorForState()) {
						it.removePermutations(size);
					} else {
						it.removePermutations(p);
					}
				}
				stackPlacements = stackPlacements.subList(size, this.stackPlacements.size());
			} else {
				stackPlacements = Collections.emptyList();
			}

			return container;
		}
	}

	public BruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	@Override
	protected Adapter<BruteForcePackagerResult> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new BruteForceAdapter(boxes, containers, interrupt);
	}

}
