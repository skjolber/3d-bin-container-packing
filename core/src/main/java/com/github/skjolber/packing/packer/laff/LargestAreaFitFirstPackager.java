package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultContainerStackValue;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.StackValuePointFilter;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.points3d.ExtremePoints3D;
import com.github.skjolber.packing.points3d.Point3D;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br><br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class LargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager<Point3D> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder {

		private List<Container> containers;
		private int checkpointsPerDeadlineCheck = 1;
		private LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> configurationBuilderFactory;

		public LargestAreaFitFirstPackagerBuilder setConfigurationBuilderFactory(LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> configurationBuilder) {
			this.configurationBuilderFactory = configurationBuilder;
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
		
		public LargestAreaFitFirstPackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(configurationBuilderFactory == null) {
				configurationBuilderFactory = new DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory<>();
			}
			return new LargestAreaFitFirstPackager(containers, checkpointsPerDeadlineCheck, configurationBuilderFactory);
		}	
	}

	public LargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point3D, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck, factory);
	}

	public LargestAreaFitFirstPackagerResult pack(List<Stackable> stackables, Container targetContainer, BooleanSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);
		
		ContainerStackValue[] stackValues = targetContainer.getStackValues();
		
		ContainerStackValue containerStackValue = stackValues[0];
		
		StackConstraint constraint = containerStackValue.getConstraint();
		
		LevelStack stack = new LevelStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter( s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter( s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints3D<StackPlacement> extremePoints3D = new ExtremePoints3D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz());

		LargestAreaFitFirstPackagerConfiguration<Point3D> configuration = factory.newBuilder().withContainer(targetContainer).withExtremePoints(extremePoints3D).withStack(stack).build();
		
		StackableFilter firstFilter = configuration.getFirstStackableFilter();
		StackValuePointFilter<Point3D> firstStackValuePointComparator = configuration.getFirstStackValuePointFilter();

		int levelOffset = 0;

		while(!scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}
			
			int maxWeight = stack.getFreeWeightLoad();

			Point3D firstPoint = extremePoints3D.getValue(0);
			
			int firstIndex = -1;
			StackValue firstStackValue = null;
			Stackable firstBox = null;
			
			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}
				if(firstBox != null && !firstFilter.filter(firstBox, box)) {
					continue;
				}
				for (StackValue stackValue : box.getStackValues()) {
					if(!stackValue.fitsInside3D(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), containerStackValue.getLoadDz())) {
						continue;
					}
					if(firstStackValue != null && !firstStackValuePointComparator.accept(firstBox, firstPoint, firstStackValue, box, firstPoint, stackValue)) {
						continue;
					}
					
					if(constraint != null && !constraint.supports(stack, box, stackValue, 0, 0, 0)) {
						continue;
					}
					firstIndex = i;
					firstStackValue = stackValue;
					firstBox = box;
				}
			}

			if(firstIndex == -1) {
				break;
			}
			Stackable stackable = scopedStackables.remove(firstIndex);
			remainingStackables.remove(stackable);
			
			DefaultContainerStackValue levelStackValue = stack.getContainerStackValue(firstStackValue.getDz());
			Stack levelStack = new DefaultStack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, firstStackValue, 0, 0, 0, -1, -1, firstPoint.getPlacements3D());

			levelStack.add(first);
			
			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints3D.reset(containerStackValue.getDx(), containerStackValue.getDy(), firstStackValue.getDz());
			extremePoints3D.add(0, first);
			
			StackableFilter nextFilter = configuration.getNextStackableFilter();
			StackValuePointFilter<Point3D> nextStackValuePointComparator = configuration.getNextStackValuePointFilter();
			
			while(!extremePoints3D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				long maxPointVolume = extremePoints3D.getMaxVolume();
				long maxPointArea = extremePoints3D.getMaxArea();

				int bestPointIndex = -1;
				int bestIndex = -1;
				StackValue bestStackValue = null;
				Stackable bestStackable = null;
				
				List<Point3D> points = extremePoints3D.getValues();
				for (int i = 0; i < scopedStackables.size(); i++) {
					Stackable box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(constraint != null && !constraint.accepts(stack, box)) {
						continue;
					}

					if(bestStackValue != null && !nextFilter.filter(bestStackable, box)) {
						continue;
					}
					for (StackValue stackValue : box.getStackValues()) {
						if(stackValue.getArea() > maxPointArea) {
							continue;
						}
						if(firstStackValue.getDz() < stackValue.getDz()) {
							continue;
						}
						
						for(int k = 0; k < points.size(); k++) {
							Point3D point3d = points.get(k);
							if(!point3d.fits3D(stackValue)) {
								continue;
							}
							if(bestIndex != -1 && !nextStackValuePointComparator.accept(bestStackable, points.get(bestPointIndex), bestStackValue, box, point3d, stackValue)) {
								continue;
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
				
				Stackable remove = scopedStackables.remove(bestIndex);
				remainingStackables.remove(remove);

				Point3D point = extremePoints3D.getValue(bestPointIndex);
				
				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ(), -1, -1, point.getPlacements3D());
				levelStack.add(stackPlacement);
				extremePoints3D.add(bestPointIndex, stackPlacement);

				maxRemainingLevelWeight -= remove.getWeight();
				
			}
			
			for (StackPlacement stackPlacement : levelStack.getPlacements()) {
				stackPlacement.setZ(levelOffset);
			}
			
			levelOffset += firstStackValue.getDz();
			
			if(levelOffset >= containerStackValue.getDz()) {
				break;
			}
			extremePoints3D.reset(containerStackValue.getDx(), containerStackValue.getDy(), containerStackValue.getDz() - levelOffset);
		}
		
		return new LargestAreaFitFirstPackagerResult(stack, new DefaultContainer(targetContainer.getName(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack), remainingStackables.isEmpty());
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
