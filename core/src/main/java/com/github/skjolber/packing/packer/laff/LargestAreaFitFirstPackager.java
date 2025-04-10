package com.github.skjolber.packing.packer.laff;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.PackResultComparator;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultPackResult;
import com.github.skjolber.packing.packer.DefaultPackResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class LargestAreaFitFirstPackager extends AbstractLargestAreaFitFirstPackager {
	
	public static boolean betterAsFirst(Box stackable1, Point point1, BoxStackValue stackValue1, Box stackable2, Point point2, BoxStackValue stackValue2) {
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

	public static boolean betterAsNext(Box stackable1, Point point1, BoxStackValue stackValue1, Box stackable2, Point point2, BoxStackValue stackValue2) {
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

	public static class LargestAreaFitFirstPackagerBuilder extends AbstractPackagerBuilder<LargestAreaFitFirstPackager, LargestAreaFitFirstPackagerBuilder> {

		public LargestAreaFitFirstPackager build() {
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new LargestAreaFitFirstPackager(packResultComparator);
		}
	}

	public LargestAreaFitFirstPackager(PackResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public DefaultPackResult pack(List<Box> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt) {
		List<Box> remainingStackables = new ArrayList<>(stackables);

		LevelStack stack = new LevelStack(targetContainer);

		List<Box> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= targetContainer.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.collect(Collectors.toList());

		ExtremePoints3D extremePoints3D = new ExtremePoints3D(targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));

		int levelOffset = 0;

		while (!scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			int maxWeight = stack.getFreeWeightLoad();

			// there is only point, spanning the free space in the level
			Point firstPoint = extremePoints3D.getValue(0);

			int firstIndex = -1;
			BoxStackValue firstStackValue = null;
			Box firstBox = null;

			// pick the box with the highest area
			for (int i = 0; i < scopedStackables.size(); i++) {
				Box box = scopedStackables.get(i);
				if(box.getWeight() > maxWeight) {
					continue;
				}
				if(firstBox != null && !FIRST_STACKABLE_FILTER.filter(firstBox, box)) {
					continue;
				}
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(!firstPoint.fits3D(stackValue)) {
						continue;
					}
					if(firstStackValue != null && !betterAsFirst(firstBox, firstPoint, firstStackValue, box, firstPoint, stackValue)) {
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
			Box stackable = scopedStackables.remove(firstIndex);
			remainingStackables.remove(stackable);

			Container levelStackValue = stack.getContainerStackValue(firstStackValue.getDz());
			Stack levelStack = new Stack();
			stack.add(levelStack);

			StackPlacement first = new StackPlacement(stackable, firstStackValue, 0, 0, 0);

			levelStack.add(first);

			int maxRemainingLevelWeight = levelStackValue.getMaxLoadWeight() - stackable.getWeight();

			extremePoints3D.reset(targetContainer.getLoadDx(), targetContainer.getLoadDy(), firstStackValue.getDz());
			extremePoints3D.add(0, first);

			while (!extremePoints3D.isEmpty() && maxRemainingLevelWeight > 0 && !scopedStackables.isEmpty()) {
				long maxPointVolume = extremePoints3D.getMaxVolume();
				long maxPointArea = extremePoints3D.getMaxArea();

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

					if(bestStackValue != null && !DEFAULT_STACKABLE_FILTER.filter(bestStackable, box)) {
						continue;
					}
					for (BoxStackValue stackValue : box.getStackValues()) {
						if(stackValue.getArea() > maxPointArea) {
							continue;
						}
						if(firstStackValue.getDz() < stackValue.getDz()) {
							continue;
						}

						int currentPointsCount = extremePoints3D.getValueCount();
						for (int k = 0; k < currentPointsCount; k++) {
							Point point3d = extremePoints3D.getValue(k);

							if(!point3d.fits3D(stackValue)) {
								continue;
							}
							if(bestIndex != -1 && !betterAsNext(bestStackable, extremePoints3D.getValue(bestPointIndex), bestStackValue, box, point3d, stackValue)) {
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

				Box remove = scopedStackables.remove(bestIndex);
				remainingStackables.remove(remove);

				Point point = extremePoints3D.getValue(bestPointIndex);

				StackPlacement stackPlacement = new StackPlacement(remove, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ());
				levelStack.add(stackPlacement);
				extremePoints3D.add(bestPointIndex, stackPlacement);

				boolean minArea = bestStackValue.getArea() == extremePoints3D.getMinAreaLimit();
				boolean minVolume = extremePoints3D.getMinVolumeLimit() == bestStackable.getVolume();
				if(minArea && minVolume) {
					extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));
				} else if(minArea) {
					extremePoints3D.setMinimumAreaLimit(getMinStackableArea(scopedStackables));
				} else if(minVolume) {
					extremePoints3D.setMinimumVolumeLimit(getMinStackableVolume(scopedStackables));
				}

				maxRemainingLevelWeight -= remove.getWeight();
			}

			// move boxes up
			for (StackPlacement stackPlacement : levelStack.getPlacements()) {
				stackPlacement.setZ(levelOffset + stackPlacement.getAbsoluteZ());
			}

			levelOffset += firstStackValue.getDz();

			int remainingDz = targetContainer.getLoadDz() - levelOffset;
			if(remainingDz == 0) {
				break;
			}
			extremePoints3D.reset(targetContainer.getLoadDx(), targetContainer.getLoadDy(), remainingDz);
		}

		return new DefaultPackResult(new Container(targetContainer.getId(), targetContainer.getDescription(), 
				targetContainer.getDx(), targetContainer.getDy(),targetContainer.getDz(),
				
				targetContainer.getEmptyWeight(), 

				targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz(),
				targetContainer.getMaxLoadWeight(),
				stack, targetContainer.getBoxItemListenerBuilderSupplier()),
				
				stack, remainingStackables.isEmpty(), index);
	}

	@Override
	public LargestAreaFitFirstPackagerResultBuilder newResultBuilder() {
		return new LargestAreaFitFirstPackagerResultBuilder().withPackager(this);
	}
}
