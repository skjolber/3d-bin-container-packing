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
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.iterator.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.Adapter;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. This implementation tries all
 * permutations and rotations, for each selecting the perceived best placement. So it does not try all possible placements (as i not all extreme-points)-
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class FastBruteForcePackager2 extends AbstractPackager<BruteForcePackagerResult, FastBruteForcePackagerResultBuilder> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;
		private PackResultComparator packResultComparator;

		public LargestAreaFitFirstPackagerBuilder withContainers(Container ...  containers) {
			if(this.containers == null) {
				this.containers = new ArrayList<>();
			}
			for (Container container : containers) {
				this.containers.add(container);
			}
			return this;
		}

		public LargestAreaFitFirstPackagerBuilder withContainers(List<Container> containers) {
			this.containers = containers;
			return this;
		}

		public LargestAreaFitFirstPackagerBuilder withCheckpointsPerDeadlineCheck(int n) {
			this.checkpointsPerDeadlineCheck = n;
			return this;
		}
		
		public LargestAreaFitFirstPackagerBuilder withPackResultComparator(PackResultComparator packResultComparator) {
			this.packResultComparator = packResultComparator;
			
			return this;
		}
		
		public FastBruteForcePackager2 build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new FastBruteForcePackager2(containers, checkpointsPerDeadlineCheck, packResultComparator);
		}	
	}
	
	private class FastBruteForceAdapter implements Adapter<BruteForcePackagerResult> {

		private final ContainerStackValue[] containerStackValue;
		private final DefaultPermutationRotationIterator[] iterators;
		private final List<Container> containers;
		private final BooleanSupplier interrupt;
		private final MemoryExtremePoints3D extremePoints3D;
		private List<StackPlacement> stackPlacements;

		public FastBruteForceAdapter(List<StackableItem> stackableItems, List<Container> containers, BooleanSupplier interrupt) {
			this.containers = containers;
			this.iterators = new DefaultPermutationRotationIterator[containers.size()];
			this.containerStackValue = new ContainerStackValue[containers.size()];
					
			for (int i = 0; i < containers.size(); i++) {
				Container container = containers.get(i);
				
				ContainerStackValue stackValue = container.getStackValues()[0];
				
				containerStackValue[i] = stackValue;
				iterators[i] = new DefaultPermutationRotationIterator(new Dimension(stackValue.getDx(), stackValue.getDy(), stackValue.getDz()), stackableItems);
			}

			this.interrupt = interrupt;
			
			int count = 0;
			for (DefaultPermutationRotationIterator iterator : iterators) {
				count = Math.max(count, iterator.length());
			}

			this.stackPlacements = getPlacements(count);
			
			this.extremePoints3D = new MemoryExtremePoints3D(1, 1, 1);
		}

		private List<StackPlacement> getPlacements(int size) {
			// each box will at most have a single placement with a space (and its remainder).
			List<StackPlacement> placements = new ArrayList<>(size);

			for (int i = 0; i < size; i++) {
				placements.add(new StackPlacement());
			}
			return placements;
		}
		
		@Override
		public BruteForcePackagerResult attempt(int i, BruteForcePackagerResult best) {
			if(iterators[i].length() == 0) {
				return BruteForcePackagerResult.EMPTY;
			}
			// TODO break if this container cannot beat the existing best result
			return FastBruteForcePackager2.this.pack(extremePoints3D, stackPlacements, containers.get(i), containerStackValue[i], iterators[i], interrupt);
		}

		@Override
		public Container accept(BruteForcePackagerResult bruteForceResult) {
			Container container = bruteForceResult.getContainer();
			Stack stack = container.getStack();
			
			int size = stack.getSize();
			if (stackPlacements.size() > size) {
				// this result does not consume all placements
				// remove consumed items from the iterators
				
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

	public FastBruteForcePackager2(List<Container> containers, int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator) {
		super(containers, checkpointsPerDeadlineCheck, packResultComparator);
	}

	@Override
	public FastBruteForcePackagerResultBuilder newResultBuilder() {
		throw new RuntimeException();
	}

	
	@Override
	protected Adapter<BruteForcePackagerResult> adapter(List<StackableItem> boxes, List<Container> containers, BooleanSupplier interrupt) {
		return new FastBruteForceAdapter(boxes, containers, interrupt);
	}
	
	public BruteForcePackagerResult pack(MemoryExtremePoints3D extremePoints, List<StackPlacement> stackPlacements, Container targetContainer, ContainerStackValue stackValue, DefaultPermutationRotationIterator rotator, BooleanSupplier interrupt) {
		Stack stack = new DefaultStack(stackValue);
		
		Container holder = new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), targetContainer.getStackValues(), stack);

		BruteForcePackagerResult bestResult = new BruteForcePackagerResult(holder, rotator);
		// optimization: compare pack results by looking only at count within the same permutation 
		BruteForcePackagerResult bestPermutationResult = new BruteForcePackagerResult(holder, rotator);

		// iterator over all permutations
		do {
			
			if (interrupt.getAsBoolean()) {
				return null;
			}
			// iterator over all rotations
			
			bestPermutationResult.reset();
			
			do {
				int count = packStackPlacement(extremePoints, stackPlacements, rotator, stack, interrupt);
				if (count == Integer.MIN_VALUE) {
					return null; // timeout
				}
				if (count == rotator.length()) {
					// best possible result for this container
					bestPermutationResult.setState(extremePoints.getPoints(), rotator.getState(), stackPlacements.subList(0, count), count == stackPlacements.size());
					
					return bestPermutationResult;
				} else if (count > 0) {
					// continue search, but see if this is the best fit so far
					// higher count implies higher volume and weight
					// since the items are the same within each permutation
					if (count > bestPermutationResult.getSize()) {
						bestPermutationResult.setState(extremePoints.getPoints(), rotator.getState(), stackPlacements.subList(0, count), count == stackPlacements.size());
					}
				}

				holder.getStack().clear();

				int diff = rotator.nextRotation();
				if(diff == -1) {
					// no more rotations, continue to next permutation
					break;
				}
			} while (true);
			
			if(!bestPermutationResult.isEmpty()) {
				// compare against other permutation's result

				if (bestResult.isEmpty() || packResultComparator.compare(bestResult, bestPermutationResult) == PackResultComparator.ARGUMENT_2_IS_BETTER) {
					// switch the two results for one another
					BruteForcePackagerResult tmp = bestResult;
					bestResult = bestPermutationResult;
					bestPermutationResult = tmp;
				}
			}
		} while (rotator.nextPermutation() != -1);
		
		return bestResult;
	}

	public int packStackPlacement(MemoryExtremePoints3D extremePoints3D, List<StackPlacement> placements, PermutationRotationIterator rotator, Stack stack, BooleanSupplier interrupt) {
		// pack as many items as possible from placementIndex
		ContainerStackValue containerStackValue = stack.getContainerStackValue();
		
		extremePoints3D.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(rotator.getMinStackableArea(), rotator.getMinStackableVolume());

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
			
			if(constraint != null && !constraint.accepts(stack, stackable)) {
				break;
			}

			StackPlacement placement = placements.get(placementIndex);
			
			StackValue stackValue = permutationRotation.getValue();
			
			int pointCount = extremePoints3D.getValueCount();
			
			int bestPointIndex = -1;
			for(int k = 0; k < pointCount; k++) {
				Point3D<StackPlacement> point3d = extremePoints3D.getValue(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}
				if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point3D<StackPlacement> bestPoint = extremePoints3D.getValue(bestPointIndex);
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
			
			Point3D<StackPlacement> point3d =  extremePoints3D.getValue(bestPointIndex);
			
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
