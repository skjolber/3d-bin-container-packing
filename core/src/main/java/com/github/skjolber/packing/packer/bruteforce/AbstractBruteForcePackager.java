package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.PackResult;
import com.github.skjolber.packing.points2d.Point2D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractBruteForcePackager<P extends Point2D> extends AbstractPackager<BruteForcePackagerResultBuilder> {

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param configurationBuilder 
	 * @param checkpointsPerDeadlineCheck 
	 */
	public AbstractBruteForcePackager(List<Container> containers) {
		this(containers, 1);
	}

	/**
	 * Constructor
	 *
	 * @param containers list of containers
	 * @param footprintFirst start with box which has the largest footprint. If not, the highest box is first.
	 * @param rotate3D whether boxes can be rotated in all three directions (two directions otherwise)
	 * @param binarySearch if true, the packager attempts to find the best box given a binary search. Upon finding a container that can hold the boxes, given time, it also tries to find a better match.
	 */

	public AbstractBruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	private class BruteForceAdapter implements Adapter {
		// instead of placing boxes, work with placements
		// this very much reduces the number of objects created
		// performance gain is something like 25% over the box-centric approach
	
		private List<StackPlacement> placements;
		private DefaultPermutationRotationIterator[] iterators;
		private List<Container> containers;
		private final BooleanSupplier interrupt;

		public BruteForceAdapter(List<StackableItem> stackableItems, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			
			iterators = new DefaultPermutationRotationIterator[containers.size()];
			for (int i = 0; i < containers.size(); i++) {
				Container container = containers.get(i);
				
				StackValue[] stackValues = container.getStackValues();
				
				iterators[i] = new DefaultPermutationRotationIterator(new Dimension(stackValues[0].getDx(), stackValues[0].getDy(), stackValues[0].getDz()), stackableItems);
			}
			
			int count = 0;
			for (DefaultPermutationRotationIterator iterator : iterators) {
				count += iterator.length();
			}

			this.placements = getPlacements(count);
			
			this.interrupt = interrupt;
		}

		@Override
		public PackResult attempt(int i) {
			if(iterators[i].length() == 0) {
				return EMPTY_PACK_RESULT;
			}
			
			return AbstractBruteForcePackager.this.pack(placements, containers.get(i), iterators[i], interrupt);
		}

		@Override
		public Container accepted(PackResult result) {
			BruteForcePackagerResult<P> bruteForceResult = (BruteForcePackagerResult<P>) result;

			Container container = bruteForceResult.getContainer();

			if (placements.size() > bruteForceResult.getPlacementCount()) {
				int[] permutations = bruteForceResult.getPermutationRotationIteratorForState().getPermutations();
				List<Integer> p = new ArrayList<>(bruteForceResult.getPlacementCount());
				for (int i = 0; i < bruteForceResult.getPlacementCount(); i++) {
					p.add(permutations[i]);
				}
				for (PermutationRotationIterator it : iterators) {
					if (it == bruteForceResult.getPermutationRotationIteratorForState()) {
						it.removePermutations(bruteForceResult.getPlacementCount());
					} else {
						it.removePermutations(p);
					}
				}
				placements = placements.subList(bruteForceResult.getPlacementCount(), this.placements.size());
			} else {
				placements = Collections.emptyList();
			}

			return container;
		}

		@Override
		public boolean hasMore(PackResult result) {
			BruteForcePackagerResult<P> bruteForceResult = (BruteForcePackagerResult<P>) result;
			return placements.size() > bruteForceResult.getPlacementCount();
		}
	}
	
	@Override
	protected Adapter adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new BruteForceAdapter(boxes, containers, interrupt);
	}

	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new StackPlacement());
		}
		return placements;
	}

	public BruteForcePackagerResult<P> pack(List<StackPlacement> placements, Container targetContainer, DefaultPermutationRotationIterator rotator, BooleanSupplier interrupt) {

		// XXX
		Container holder = new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), targetContainer.getStackValues(), new DefaultStack());

		BruteForcePackagerResult<P> result = new BruteForcePackagerResult<P>(holder, rotator);

		// iterator over all permutations
		do {
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			int index = 0;
			do {
				int count = packStackPlacement(placements, rotator, holder, index, interrupt);
				if (count == Integer.MIN_VALUE) {
					return null; // timeout
				}
				if (count == placements.size()) {
					result.setState(placements, count, rotator.getState());
					
					return result;
				} else if (count > 0) {
					// continue search, but see if this is the best fit so far
					if (count > result.getPlacementCount()) {
						result.setState(placements, count, rotator.getState());
					}
				}

				int diff = rotator.nextRotation();
				if(diff == -1) {
					// no more rotations, continue to next permutation
					holder.getStack().clear();

					break;
				}
				
				index = 0;
				holder.getStack().clear();
				
			} while (true);
		} while (rotator.nextPermutation() != -1);

		return result;
	}

	protected int packStackPlacement(List<StackPlacement> items, Container container, DefaultPermutationRotationIterator iterator) {
		return packStackPlacement(items, iterator, container, 0, BooleanSupplierBuilder.NOOP);
	}

	protected abstract int packStackPlacement(List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, int placementIndex, BooleanSupplier interrupt);
	
}
