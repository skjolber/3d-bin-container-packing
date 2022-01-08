package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.Point3D;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement. So it does not try all possible placements.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class FastBruteForcePackager extends AbstractPackager<FastBruteForcePackagerResult, FastBruteForcePackagerResultBuilder> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;

		public LargestAreaFitFirstPackagerBuilder withContainers(List<Container> containers) {
			this.containers = containers;
			return this;
		}

		public LargestAreaFitFirstPackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
			this.checkpointsPerDeadlineCheck = n;
			return this;
		}
		
		public FastBruteForcePackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			return new FastBruteForcePackager(containers, checkpointsPerDeadlineCheck);
		}	
	}
	
	private class FastBruteForceAdapter implements Adapter<FastBruteForcePackagerResult> {

		private DefaultPermutationRotationIterator[] iterators;
		private List<Container> containers;
		private final BooleanSupplier interrupt;
		private MemoryExtremePoints3D extremePoints3D;
		private List<StackPlacement> stackPlacements;

		public FastBruteForceAdapter(List<StackableItem> stackableItems, List<Container> containers, BooleanSupplier interrupt) {
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
			
			this.extremePoints3D = new MemoryExtremePoints3D(1, 1, 1);
		}

		@Override
		public FastBruteForcePackagerResult attempt(int i, FastBruteForcePackagerResult best) {
			if(iterators[i].length() == 0) {
				return FastBruteForcePackagerResult.EMPTY;
			}
			
			// TODO break if this container cannot beat the existing best result
			
			return FastBruteForcePackager.this.pack(extremePoints3D, stackPlacements, containers.get(i), iterators[i], interrupt);
		}

		@Override
		public Container accept(FastBruteForcePackagerResult bruteForceResult) {
			Container container = bruteForceResult.getContainer();
			Stack stack = container.getStack();
			
			int size = stack.getSize();
			if (stackPlacements.size() > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators
				
				DefaultPermutationRotationIterator iterator = bruteForceResult.getPermutationRotationIteratorForState();
				
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
		

	public FastBruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	@Override
	public FastBruteForcePackagerResultBuilder newResultBuilder() {
		return new FastBruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}

	
	@Override
	protected Adapter<FastBruteForcePackagerResult> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new FastBruteForceAdapter(boxes, containers, interrupt);
	}
	

	static List<StackPlacement> getPlacements(int size) {
		// each box will at most have a single placement with a space (and its remainder).
		List<StackPlacement> placements = new ArrayList<>(size);

		for (int i = 0; i < size; i++) {
			placements.add(new StackPlacement());
		}
		return placements;
	}


	public FastBruteForcePackagerResult pack(MemoryExtremePoints3D extremePoints, List<StackPlacement> stackPlacements, Container targetContainer, DefaultPermutationRotationIterator rotator, BooleanSupplier interrupt) {
		Container holder = new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), targetContainer.getStackValues(), new DefaultStack());
		
		FastBruteForcePackagerResult result = new FastBruteForcePackagerResult(holder, rotator);

		// iterator over all permutations
		do {
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			do {
				int count = packStackPlacement(extremePoints, stackPlacements, rotator, holder, interrupt);
				if (count == Integer.MIN_VALUE) {
					return null; // timeout
				}
				if (count == rotator.length()) {
					// best possible result for this container
					result.setState(extremePoints.getPoints(), rotator.getState(), stackPlacements.subList(0, count), count == stackPlacements.size());
					
					return result;
				} else if (count > 0) {
					// continue search, but see if this is the best fit so far
					if (count > result.getSize()) {
						result.setState(extremePoints.getPoints(), rotator.getState(), stackPlacements.subList(0, count), count == stackPlacements.size());
					}
				}

				holder.getStack().clear();

				int diff = rotator.nextRotation();
				if(diff == -1) {
					// no more rotations, continue to next permutation
					break;
				}
			} while (true);
		} while (rotator.nextPermutation() != -1);

		return result;
	}

	protected int packStackPlacement(MemoryExtremePoints3D extremePoints, List<StackPlacement> placements, PermutationRotationIterator iterator, Container container) {
		return packStackPlacement(extremePoints, placements, iterator, container, BooleanSupplierBuilder.NOOP);
	}

	public int packStackPlacement(MemoryExtremePoints3D extremePoints3D, List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, BooleanSupplier interrupt) {
		// pack as many items as possible from placementIndex
		ContainerStackValue[] stackValues = container.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		extremePoints3D.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());

		StackConstraint constraint = containerStackValue.getConstraint();
		
		int maxLoadWeight = containerStackValue.getMaxLoadWeight();

		int placementIndex = 0;
		while (placementIndex < rotator.length()) {
			if (interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				return Integer.MIN_VALUE;
			}
			PermutationRotation permutationRotation = rotator.get(placementIndex);
			
			Stackable stackable = permutationRotation.getStackable();
			if (stackable.getWeight() > maxLoadWeight) {
				break;
			}
			
			Stack stack = container.getStack();
			if(constraint != null && !constraint.accepts(stack, stackable)) {
				break;
			}

			StackPlacement placement = placements.get(placementIndex);
			
			StackValue stackValue = permutationRotation.getValue();
			
			List<Point3D<StackPlacement>> points = extremePoints3D.getValues();
			
			// TODO brute force in 3d point dimension too
			// a recursive algorithm is perhaps appropriate since the number of boxes is limited
			// so there 
			
			int bestPointIndex = -1;
			for(int k = 0; k < points.size(); k++) {
				Point3D<StackPlacement> point3d = points.get(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}
				if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point3D<StackPlacement> bestPoint = points.get(bestPointIndex);
					if(bestPoint.getArea() < point3d.getArea()) {
						continue;
					} else if(bestPoint.getArea() == point3d.getArea() && bestPoint.getVolume() < point3d.getVolume()) {
						continue;
					}
				}
				bestPointIndex = k;
			}
			
			if(bestPointIndex == -1) { // interrupted
				break;
			}
			
			Point3D<StackPlacement> point3d = points.get(bestPointIndex);
			
			placement.setStackable(stackable);
			placement.setStackValue(stackValue);
			placement.setX(point3d.getMinX());
			placement.setY(point3d.getMinY());
			placement.setZ(point3d.getMinZ());
			
			extremePoints3D.add(bestPointIndex, placement);
			
			maxLoadWeight -= stackable.getWeight();
			
			stack.add(placement);
			
			placementIndex++;
		}
		
		return placementIndex;
	}


}
