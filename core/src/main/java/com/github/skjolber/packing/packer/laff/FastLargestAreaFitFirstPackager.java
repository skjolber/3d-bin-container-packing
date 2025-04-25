package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.comparator.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;
import com.github.skjolber.packing.ep.points2d.Point2D;
import com.github.skjolber.packing.ep.points2d.SimplePoint2D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResult;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container. Only places boxes along the floor of each level.
 * <br>
 * <br>
 * Thread-safe implementation. The input boxes must however only be used in a single thread at a time.
 */

public class FastLargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {

	public static boolean betterAsFirst(Box stackable1, Point2D point1, BoxStackValue stackValue1, Box stackable2, Point2D point2, BoxStackValue stackValue2) {
		if(stackValue1.getArea() == stackValue2.getArea()) {
			if(stackValue1.getVolume() == stackValue2.getVolume()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());

				return distance2 < distance1; // closest is better
			}
			return stackValue1.getVolume() < stackValue2.getVolume(); // larger volume is better 

		}
		return stackValue1.getArea() < stackValue2.getArea(); // larger area is better
	};

	public static boolean betterAsNext(Box stackable1, Point2D point1, BoxStackValue stackValue1, Box stackable2, Point2D point2, BoxStackValue stackValue2) {
		if(stackable2.getVolume() == stackable1.getVolume()) {
			if(stackValue1.getArea() == stackValue2.getArea()) {
				// closest distance to a wall is better

				int distance1 = Math.min(point1.getDx() - stackValue1.getDx(), point1.getDy() - stackValue1.getDy());
				int distance2 = Math.min(point2.getDx() - stackValue2.getDx(), point2.getDy() - stackValue2.getDy());

				return distance2 < distance1; // closest is better
			}
			return stackValue2.getArea() < stackValue1.getArea(); // smaller is better
		}
		return stackable2.getVolume() > stackable1.getVolume(); // larger volume is better 
	};

	
	public static LargestAreaFitFirstPackagerBuilder newBuilder() {
		return new LargestAreaFitFirstPackagerBuilder();
	}

	public static class LargestAreaFitFirstPackagerBuilder extends AbstractPackagerBuilder<FastLargestAreaFitFirstPackager, LargestAreaFitFirstPackagerBuilder> {

		public FastLargestAreaFitFirstPackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new FastLargestAreaFitFirstPackager(comparator);
		}
	}

	public FastLargestAreaFitFirstPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}

	public DefaultPackResult pack2(List<Box> stackables, Container targetContainer, int containerIndex, PackagerInterruptSupplier interrupt) {
		List<Box> remainingStackables = new ArrayList<>(stackables);

		LevelStack stack = new LevelStack(targetContainer);

		List<Box> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= targetContainer.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.collect(Collectors.toList());

		ExtremePoints2D extremePoints2D = new ExtremePoints2D(targetContainer.getLoadDx(), targetContainer.getLoadDy());

		extremePoints2D.setMinArea(getMinStackableArea(scopedStackables));

		int levelOffset = 0;

		while (!scopedStackables.isEmpty() && levelOffset < targetContainer.getLoadDz()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			int maxWeight = stack.getFreeWeightLoad();
			int maxHeight = targetContainer.getLoadDz() - levelOffset;

			// there is only point, spanning the free space in the level
			SimplePoint2D firstPoint = extremePoints2D.getValue(0);

			int bestFirstIndex = -1;
			BoxStackValue bestFirstStackValue = null;
			Box bestFirstBox = null;

			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Box box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(bestFirstBox != null && !FIRST_STACKABLE_FILTER.filter(bestFirstBox, box)) {
					continue;
				}
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getDz() > maxHeight) {
						continue;
					}
					if(!firstPoint.fits2D(stackValue)) {
						continue;
					}
					if(bestFirstStackValue != null && !betterAsFirst(bestFirstBox, firstPoint, bestFirstStackValue, box, firstPoint, stackValue)) {
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
			Box stackable = scopedStackables.remove(bestFirstIndex);
			remainingStackables.remove(stackable);

			Container levelStackValue = stack.getContainerStackValue(bestFirstStackValue.getDz());
			Stack levelStack = new Stack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, bestFirstStackValue, 0, 0, 0);

			levelStack.add(first);

			int levelHeight = levelStackValue.getDz();

			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints2D.reset(targetContainer.getLoadDx(), targetContainer.getLoadDy(), levelHeight);

			extremePoints2D.add(0, first);

			while (!extremePoints2D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				if(interrupt.getAsBoolean()) {
					// fit2d below might have returned due to deadline

					return null;
				}

				long maxPointArea = extremePoints2D.getMaxArea();
				long maxPointVolume = maxPointArea * levelHeight;

				int bestPointIndex = -1;
				int bestIndex = -1;
				BoxStackValue bestStackValue = null;
				Box bestStackable = null;

				for (int i = 0; i < scopedStackables.size(); i++) {
					Box box = scopedStackables.get(i);
					if(box.getVolume() > maxPointVolume) {
						continue;
					}
					if(box.getWeight() > maxRemainingLevelWeight) {
						continue;
					}
					if(maxPointArea < box.getMinimumArea()) {
						continue;
					}

					if(bestStackValue != null && !DEFAULT_STACKABLE_FILTER.filter(bestStackable, box)) {
						continue;
					}
					for (BoxStackValue stackValue : box.getStackValues()) {
						if(stackValue.getArea() > maxPointArea) {
							continue;
						}
						if(levelHeight < stackValue.getDz()) {
							continue;
						}

						// pick the best point / stackable combination
						int pointCount = extremePoints2D.getValueCount();
						for (int k = 0; k < pointCount; k++) {
							SimplePoint2D point2d = extremePoints2D.getValue(k);
							if(point2d.getArea() < stackValue.getArea()) {
								continue;
							}

							if(!point2d.fits2D(stackValue)) {
								continue;
							}

							if(bestIndex != -1 && !betterAsNext(bestStackable, extremePoints2D.getValue(bestPointIndex), bestStackValue, box, point2d, stackValue)) {
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

				Box remove = scopedStackables.remove(bestIndex);
				remainingStackables.remove(remove);

				SimplePoint2D point = extremePoints2D.getValue(bestPointIndex);

				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), 0);
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

			extremePoints2D.reset(targetContainer.getLoadDx(), targetContainer.getLoadDy(), -1);
		}

		return new DefaultPackResult(new Container(targetContainer.getId(), targetContainer.getDescription(), 
				targetContainer.getDx(), targetContainer.getDy(),targetContainer.getDz(),
				
				targetContainer.getEmptyWeight(), 

				targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz(),
				targetContainer.getMaxLoadWeight(),
				stack),
				
				stack, remainingStackables.isEmpty(), containerIndex);
	}

}
