package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.iterator.PermutationRotation;
import com.github.skjolber.packing.iterator.PermutationRotationIterator;
import com.github.skjolber.packing.points3d.ExtremePoints3D;
import com.github.skjolber.packing.points3d.Point3D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class BruteForcePackager extends AbstractBruteForcePackager<Point3D> {

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
		
		public BruteForcePackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			return new BruteForcePackager(containers, checkpointsPerDeadlineCheck);
		}	
	}

	private static class Iterator {
		private List<StackPlacement> placements;
		private PermutationRotationIterator rotator;
		private StackConstraint constraint;
		private Stack stack;
		private BooleanSupplier interrupt;	
	}
	
	public BruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}

	public int packStackPlacement(List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, int placementIndex, BooleanSupplier interrupt) {
		if (placements.isEmpty()) {
			return -1;
		}

		// pack as many items as possible from placementIndex
		
		ContainerStackValue[] stackValues = container.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz(), true);
		
		int maxLoadWeight = containerStackValue.getMaxLoadWeight();
		
		Iterator iterator = new Iterator();
		
		iterator.constraint = containerStackValue.getConstraint();
		iterator.interrupt = interrupt;
		iterator.placements = placements;
		iterator.stack = container.getStack();
		iterator.rotator = rotator;
		
		ExtremePoints3D<StackPlacement> bestExtremePoints3D = packStackPlacement(iterator, placementIndex, maxLoadWeight, extremePoints3D);
		
		if(bestExtremePoints3D == null) {
			return -1;
		}
		
		List<StackPlacement> best = bestExtremePoints3D.getPlacements();
		
		for(int i = 0; i < best.size(); i++) {
			placements.set(i, best.get(i));
		}
		return best.size();
		
	}

	private ExtremePoints3D<StackPlacement> packStackPlacement(Iterator iterator, int placementIndex, int maxLoadWeight, ExtremePoints3D<StackPlacement> extremePoints3D) {
		if (iterator.interrupt.getAsBoolean()) {
			// fit2d below might have returned due to deadline
			return null;
		}

		PermutationRotation permutationRotation = iterator.rotator.get(placementIndex);
		
		Stackable stackable = permutationRotation.getStackable();
		if (stackable.getWeight() > maxLoadWeight) {
			return extremePoints3D;
		}
		
		Stack stack = iterator.stack;
		if(iterator.constraint != null && !iterator.constraint.accepts(stack, stackable)) {
			return extremePoints3D;
		}

		StackPlacement placement = iterator.placements.get(placementIndex);
		StackValue stackValue = permutationRotation.getValue();

		maxLoadWeight -= stackable.getWeight();

		ExtremePoints3D<StackPlacement> best = null;

		List<Point3D> points = extremePoints3D.getValues();
		for(int k = 0; k < points.size(); k++) {
			Point3D point3d = points.get(k);
			if(!point3d.fits3D(stackValue)) {
				continue;
			}
			if(iterator.constraint != null && !iterator.constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
				continue;
			}

			placement.setStackable(stackable);
			placement.setStackValue(stackValue);
			placement.setX(point3d.getMinX());
			placement.setY(point3d.getMinY());
			placement.setZ(point3d.getMinZ());

			if(placementIndex + 1 >= iterator.rotator.length()) {
				extremePoints3D.add(k, placement);
				stack.add(placement);
				
				return extremePoints3D;
			}

			ExtremePoints3D<StackPlacement> clone = extremePoints3D.clone();

			clone.add(k, placement);
			stack.add(placement);

			ExtremePoints3D<StackPlacement> packStackPlacement = packStackPlacement(iterator, placementIndex + 1, maxLoadWeight, clone);
			if(packStackPlacement != null) {
				if(packStackPlacement.getPlacements().size() >= iterator.rotator.length()) {
					return packStackPlacement;
				}
				
				if(best == null) {
					best = packStackPlacement;
				} else if(best.getPlacements().size() < packStackPlacement.getPlacements().size()) {
					stack.remove(best.getPlacements().get(best.getPlacements().size() -1));
					best = packStackPlacement;
				} else {
					stack.remove(placement);
				}
			}
		}
		
		return best;
	}



}
 