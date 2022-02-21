package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. 
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractPlainPackager<Point3D<StackPlacement>> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;

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
		
		public PlainPackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			return new PlainPackager(containers, checkpointsPerDeadlineCheck);
		}	
	}

	public PlainPackager(List<Container> containers, int checkpointsPerDeadlineCheck) {
		super(containers, checkpointsPerDeadlineCheck);
	}

	public PlainPackagerResult pack(List<Stackable> stackables, Container targetContainer, BooleanSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);
		
		ContainerStackValue[] stackValues = targetContainer.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		StackConstraint constraint = containerStackValue.getConstraint();
		
		DefaultStack stack = new DefaultStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter( s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter( s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));

		int maxRemainingWeight = containerStackValue.getMaxLoadWeight();

		while(!extremePoints3D.isEmpty() && maxRemainingWeight > 0 && !scopedStackables.isEmpty()) {
			long maxPointVolume = extremePoints3D.getMaxVolume();
			long maxPointArea = extremePoints3D.getMaxArea();

			int bestPointIndex = -1;
			int bestIndex = -1;
			
			StackValue bestStackValue = null;
			Stackable bestStackable = null;
			
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getVolume() > maxPointVolume) {
					continue;
				}
				if(box.getWeight() > maxRemainingWeight) {
					continue;
				}
				
				if(bestStackable != null && isBetter(bestStackable, box)) {
					continue;
				}
				
				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}

				for (StackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
					if(stackValue.getVolume() > maxPointVolume) {
						continue;
					}
					
					int currentPointsCount = extremePoints3D.getValueCount();
					for(int k = 0; k < currentPointsCount; k++) {
						Point3D<StackPlacement> point3d = extremePoints3D.getValue(k);
						
						if(!point3d.fits3D(stackValue)) {
							continue;
						}
						
						// ********************************************************************************
						// * Prefer the tightest placement, i.e. waste as little as possible
						// ********************************************************************************
						if(bestIndex != -1) {
							if(!isBetter(bestStackable, extremePoints3D.getValue(bestPointIndex), bestStackValue, box, point3d, stackValue)) {
								continue;
							}

						}
						if(constraint != null && !constraint.supports(stack, box, stackValue, point3d.getMinX(), point3d.getMinY(), point3d.getMinZ())) {
							continue;
						}
						bestPointIndex = k;
						bestIndex = i;
						bestStackValue = stackValue;
						bestStackable = box;
					}
				}
			}
			
			if(bestIndex == -1) {
				break;
			}
			
			scopedStackables.remove(bestIndex);
			remainingStackables.remove(bestStackable);

			Point3D<StackPlacement> point = extremePoints3D.getValue(bestPointIndex);
			
			StackPlacement stackPlacement = new StackPlacement(bestStackable, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ(), -1, -1, point.getPlacements3D());
			stack.add(stackPlacement);
			extremePoints3D.add(bestPointIndex, stackPlacement);

			maxRemainingWeight -= bestStackable.getWeight();
		}
		
		return new PlainPackagerResult(stack, new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack), remainingStackables.isEmpty());
	}

	protected boolean isBetter(Stackable bestStackable, Point3D<StackPlacement> bestPoint, StackValue bestStackValue, Stackable candidateBox, Point3D<StackPlacement> candidatePoint, StackValue candidateStackValue) {
		// ********************************************
		// * Prefer point with lowest volume remaining
		// ********************************************
		long bestRemaining = bestPoint.getVolume() - bestStackValue.getVolume();
		long candidateRemaining = candidatePoint.getVolume() - candidateStackValue.getVolume();
		
		return candidateRemaining < bestRemaining;

	}

	protected boolean isBetter(Stackable bestStackable, Stackable box) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************
		
		if(bestStackable.getVolume() == box.getVolume()) {
			return bestStackable.getWeight() > box.getWeight();
		}
		return bestStackable.getVolume() > box.getVolume();
	}

	@Override
	public PlainPackagerResultBuilder newResultBuilder() {
		return new PlainPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
