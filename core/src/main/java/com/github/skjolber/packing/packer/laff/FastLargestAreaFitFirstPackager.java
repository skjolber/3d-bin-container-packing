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
import com.github.skjolber.packing.api.PackResultComparator;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackConstraint;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.StackableFilter;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.StackValuePointFilter;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResult;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes must however only be used in a single thread at a time.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager<Point2D<StackPlacement>> {

	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder extends AbstractPackagerBuilder<FastLargestAreaFitFirstPackager, LargestAreaFitFirstPackagerBuilder> {

		private LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D<StackPlacement>, ?> configurationBuilderFactory;

		public LargestAreaFitFirstPackagerBuilder setConfigurationBuilderFactory(LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D<StackPlacement>, ?> configurationBuilder) {
			this.configurationBuilderFactory = configurationBuilder;
			return this;
		}

		public FastLargestAreaFitFirstPackager build() {
			if(containers == null) {
				throw new IllegalStateException("Expected containers");
			}
			if(configurationBuilderFactory == null) {
				configurationBuilderFactory = new DefaultLargestAreaFitFirstPackagerConfigurationBuilderFactory<>();
			}
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new FastLargestAreaFitFirstPackager(containers, checkpointsPerDeadlineCheck, packResultComparator, configurationBuilderFactory);
		}
	}

	public FastLargestAreaFitFirstPackager(List<Container> containers, int checkpointsPerDeadlineCheck, PackResultComparator packResultComparator,
			LargestAreaFitFirstPackagerConfigurationBuilderFactory<Point2D<StackPlacement>, ?> factory) {
		super(containers, checkpointsPerDeadlineCheck, packResultComparator, factory);
	}

	public DefaultPackResult pack(List<Stackable> stackables, Container targetContainer, BooleanSupplier interrupt) {
		List<Stackable> remainingStackables = new ArrayList<>(stackables);

		ContainerStackValue[] stackValues = targetContainer.getStackValues();

		ContainerStackValue containerStackValue = stackValues[0];

		StackConstraint constraint = containerStackValue.getConstraint();

		LevelStack stack = new LevelStack(containerStackValue);

		List<Stackable> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= containerStackValue.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.filter(s -> constraint == null || constraint.canAccept(s))
				.collect(Collectors.toList());

		ExtremePoints2D<StackPlacement> extremePoints2D = new ExtremePoints2D<>(containerStackValue.getLoadDx(), containerStackValue.getLoadDy());

		extremePoints2D.setMinArea(getMinStackableArea(scopedStackables));

		LargestAreaFitFirstPackagerConfiguration<Point2D<StackPlacement>> configuration = factory.newBuilder().withContainer(targetContainer).withExtremePoints(extremePoints2D).withStack(stack)
				.build();

		StackableFilter firstFilter = configuration.getFirstStackableFilter();
		StackValuePointFilter<Point2D<StackPlacement>> firstStackValuePointComparator = configuration.getFirstStackValuePointFilter();

		int levelOffset = 0;

		while (!scopedStackables.isEmpty() && levelOffset < containerStackValue.getLoadDz()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			int maxWeight = stack.getFreeWeightLoad();
			int maxHeight = containerStackValue.getLoadDz() - levelOffset;

			// there is only point, spanning the free space in the level
			Point2D<StackPlacement> firstPoint = extremePoints2D.getValue(0);

			int bestFirstIndex = -1;
			StackValue bestFirstStackValue = null;
			Stackable bestFirstBox = null;

			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Stackable box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(constraint != null && !constraint.accepts(stack, box)) {
					continue;
				}
				if(bestFirstBox != null && !firstFilter.filter(bestFirstBox, box)) {
					continue;
				}
				for (StackValue stackValue : box.getStackValues()) {
					if(stackValue.getDz() > maxHeight) {
						continue;
					}
					if(!firstPoint.fits2D(stackValue)) {
						continue;
					}
					if(constraint != null && !constraint.supports(stack, box, stackValue, 0, 0, levelOffset)) {
						continue;
					}
					if(bestFirstStackValue != null && !firstStackValuePointComparator.accept(bestFirstBox, firstPoint, bestFirstStackValue, box, firstPoint, stackValue)) {
						continue;
					}
					bestFirstIndex = i;
					bestFirstStackValue = stackValue;
					bestFirstBox = box;
				}
			}

			if(bestFirstIndex == -1) {
				break;
			}
			Stackable stackable = scopedStackables.remove(bestFirstIndex);
			remainingStackables.remove(stackable);

			DefaultContainerStackValue levelStackValue = stack.getContainerStackValue(bestFirstStackValue.getDz());
			Stack levelStack = new DefaultStack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, bestFirstStackValue, 0, 0, 0, -1, -1);

			levelStack.add(first);

			int levelHeight = levelStackValue.getDz();

			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints2D.reset(containerStackValue.getDx(), containerStackValue.getDy(), levelHeight);

			extremePoints2D.add(0, first);

			StackableFilter nextFilter = configuration.getNextStackableFilter();
			StackValuePointFilter<Point2D<StackPlacement>> nextStackValuePointComparator = configuration.getNextStackValuePointFilter();

			while (!extremePoints2D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					// fit2d below might have returned due to deadline

					return null;
				}

				long maxPointArea = extremePoints2D.getMaxArea();
				long maxPointVolume = maxPointArea * levelHeight;

				int bestPointIndex = -1;
				int bestIndex = -1;
				StackValue bestStackValue = null;
				Stackable bestStackable = null;

				for (int i = 0; i < scopedStackables.size(); i++) {
					Stackable box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(maxPointArea < box.getMinimumArea()) {
						continue;
					}

					if(bestStackValue != null && !nextFilter.filter(bestStackable, box)) {
						continue;
					}
					for (StackValue stackValue : box.getStackValues()) {
						if(stackValue.getArea() > maxPointArea) {
							continue;
						}
						if(levelHeight < stackValue.getDz()) {
							continue;
						}

						// pick the best point / stackable combination
						int pointCount = extremePoints2D.getValueCount();
						for (int k = 0; k < pointCount; k++) {
							Point2D<StackPlacement> point2d = extremePoints2D.getValue(k);
							if(point2d.getArea() < stackValue.getArea()) {
								continue;
							}

							if(!point2d.fits2D(stackValue)) {
								continue;
							}

							if(bestIndex != -1 && !nextStackValuePointComparator.accept(bestStackable, extremePoints2D.getValue(bestPointIndex), bestStackValue, box, point2d, stackValue)) {
								continue;
							}
							if(constraint != null && !constraint.supports(stack, box, stackValue, point2d.getMinX(), point2d.getMinY(), levelOffset)) {
								continue;
							}

							bestPointIndex = k;
							bestIndex = i;
							bestStackValue = stackValue;
							bestStackable = stackable;
						}
					}
				}

				if(bestIndex == -1) {
					break;
				}

				Stackable remove = scopedStackables.remove(bestIndex);
				remainingStackables.remove(remove);

				Point2D<StackPlacement> point = extremePoints2D.getValue(bestPointIndex);

				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), 0, -1, -1);
				levelStack.add(stackPlacement);
				extremePoints2D.add(bestPointIndex, stackPlacement);

				if(bestStackValue.getArea() == extremePoints2D.getMinAreaLimit()) {
					extremePoints2D.setMinArea(getMinStackableArea(scopedStackables));
				}

				maxRemainingLevelWeight -= remove.getWeight();

			}

			// move boxes up 
			for (StackPlacement stackPlacement : levelStack.getPlacements()) {
				stackPlacement.setZ(levelOffset); // all z positions are zero and thus omitted
			}

			levelOffset += levelHeight;

			extremePoints2D.reset(containerStackValue.getLoadDx(), containerStackValue.getLoadDy(), -1);
		}

		return new DefaultPackResult(new DefaultContainer(targetContainer.getId(), targetContainer.getDescription(), targetContainer.getVolume(), targetContainer.getEmptyWeight(), stackValues, stack),
				stack, remainingStackables.isEmpty());
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withCheckpointsPerDeadlineCheck(checkpointsPerDeadlineCheck).withPackager(this);
	}
}
