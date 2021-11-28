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

public class FastBruteForcePackager extends AbstractBruteForcePackager<Point3D> {

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

	public FastBruteForcePackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	@Override
	public BruteForcePackagerResultBuilder newResultBuilder() {
		return new BruteForcePackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}

	public int packStackPlacement(List<StackPlacement> placements, PermutationRotationIterator rotator, Container container, int placementIndex, BooleanSupplier interrupt) {
		// pack as many items as possible from placementIndex
		
		ContainerStackValue[] stackValues = container.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		StackConstraint constraint = containerStackValue.getConstraint();
		
		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		
		int maxLoadWeight = containerStackValue.getMaxLoadWeight();
		
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
			
			List<Point3D> points = extremePoints3D.getValues();
			
			// TODO brute force in 3d point dimension too
			// a recursive algorithm is perhaps appropriate since the number of boxes is limited
			// so there 
			
			int bestPointIndex = -1;
			for(int k = 0; k < points.size(); k++) {
				Point3D point3d = points.get(k);
				if(!point3d.fits3D(stackValue)) {
					continue;
				}
				if(constraint != null && !constraint.supports(stack, stackable, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
					continue;
				}

				if(bestPointIndex != -1) {
					Point3D bestPoint = points.get(bestPointIndex);
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
			
			Point3D point3d = points.get(bestPointIndex);
			
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
