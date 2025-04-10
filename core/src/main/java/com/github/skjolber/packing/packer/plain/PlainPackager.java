package com.github.skjolber.packing.packer.plain;

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
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractPlainPackager {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		public PlainPackager build() {
			if(packResultComparator == null) {
				packResultComparator = new DefaultPackResultComparator();
			}
			return new PlainPackager(packResultComparator);
		}
	}

	public PlainPackager(PackResultComparator packResultComparator) {
		super(packResultComparator);
	}

	public DefaultPackResult pack(List<Box> stackables, Container targetContainer, int index, PackagerInterruptSupplier interrupt) {
		List<Box> remainingStackables = new ArrayList<>(stackables);

		Stack stack = new Stack();

		List<Box> scopedStackables = stackables
				.stream()
				.filter(s -> s.getVolume() <= targetContainer.getMaxLoadVolume() && s.getWeight() <= targetContainer.getMaxLoadWeight())
				.collect(Collectors.toList());

		ExtremePoints3D extremePoints3D = new ExtremePoints3D(targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));

		int maxRemainingWeight = targetContainer.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && maxRemainingWeight > 0 && !scopedStackables.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline

				return null;
			}

			long maxPointVolume = extremePoints3D.getMaxVolume();
			long maxPointArea = extremePoints3D.getMaxArea();

			int bestPointIndex = -1;
			int bestIndex = -1;
			
			long bestPointSupportPercent = -1L;

			BoxStackValue bestStackValue = null;
			Box bestStackable = null;

			int currentPointsCount = extremePoints3D.getValueCount();
			for (int i = 0; i < scopedStackables.size(); i++) {
				Box box = scopedStackables.get(i);
				if(box.getVolume() > maxPointVolume) {
					continue;
				}
				if(box.getWeight() > maxRemainingWeight) {
					continue;
				}

				if(bestStackable != null && !isBetter(bestStackable, box)) {
					continue;
				}

				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
					if(stackValue.getVolume() > maxPointVolume) {
						continue;
					}

					for (int k = 0; k < currentPointsCount; k++) {
						Point point3d = extremePoints3D.getValue(k);

						if(!point3d.fits3D(stackValue)) {
							continue;
						}

						long pointSupportPercent; // cache for costly measurement
						if(bestIndex != -1) {
							Point bestPoint = extremePoints3D.getValue(bestPointIndex);
							
							if(point3d.getMinZ() > bestPoint.getMinZ()) {
								continue;
							}
							
							pointSupportPercent = calculateXYSupportPercent(extremePoints3D, point3d, stackValue);
							
							if(point3d.getMinZ() == bestPoint.getMinZ()) {
								if(pointSupportPercent < bestPointSupportPercent) {
									continue;
								}
							
								if(stackValue.getArea() <= bestStackValue.getArea()) {
									continue;
								}
							}
						} else {
							pointSupportPercent = calculateXYSupportPercent(extremePoints3D, point3d, stackValue);
						}
						
						bestPointSupportPercent = pointSupportPercent;
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

			Point point = extremePoints3D.getValue(bestPointIndex);

			StackPlacement stackPlacement = new StackPlacement(bestStackable, bestStackValue, point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints3D.add(bestPointIndex, stackPlacement);

			if(!scopedStackables.isEmpty()) {
				boolean minArea = bestStackValue.getArea() == extremePoints3D.getMinAreaLimit();
				boolean minVolume = extremePoints3D.getMinVolumeLimit() == bestStackable.getVolume();
				if(minArea && minVolume) {
					extremePoints3D.setMinimumAreaAndVolumeLimit(getMinStackableArea(scopedStackables), getMinStackableVolume(scopedStackables));
				} else if(minArea) {
					extremePoints3D.setMinimumAreaLimit(getMinStackableArea(scopedStackables));
				} else if(minVolume) {
					extremePoints3D.setMinimumVolumeLimit(getMinStackableVolume(scopedStackables));
				}
			}

			maxRemainingWeight -= bestStackable.getWeight();
		}

		return new DefaultPackResult(new Container(targetContainer.getId(), targetContainer.getDescription(), 
				targetContainer.getDx(), targetContainer.getDy(),targetContainer.getDz(),
				
				targetContainer.getEmptyWeight(), 

				targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz(),
				targetContainer.getMaxLoadWeight(),
				stack, targetContainer.getBoxItemListenerBuilderSupplier()),
				
				stack, remainingStackables.isEmpty(), index);		
	}

	protected long calculateXYSupportPercent(ExtremePoints3D extremePoints3D, Point referencePoint, BoxStackValue stackValue) {
		long sum = 0;

		int minX = referencePoint.getMinX();
		int minY = referencePoint.getMinY();
		
		int maxX = minX + stackValue.getDx() - 1; // inclusive
		int maxY = minY + stackValue.getDy() - 1; // inclusive
		
		long max = (maxX - minX + 1) * (maxY - minY + 1);
		
		int z = referencePoint.getMinZ() - 1;
		
		List<StackPlacement> placements = extremePoints3D.getPlacements();
		for(StackPlacement stackPlacement : placements) {
			if(stackPlacement.getAbsoluteEndZ() == z) {
				
				// calculate the common area
				// check too far
				if(stackPlacement.getAbsoluteX() > maxX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteY() > maxY) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndX() < minX) {
					continue;
				}
				
				if(stackPlacement.getAbsoluteEndY() < minY) {
					continue;
				}
				
				// placement can support the stack value
				
				// |           
				// |           |---------|
				// |           |         | 
				// |    |-----------|    |
				// |    |      |xxxx|    |
				// |    |      -----|----|
				// |    |           |
				// |    |-----------| 
				// |
				// --------------------------------
				
			    int x1 = Math.max(stackPlacement.getAbsoluteX(), minX);
			    int y1 = Math.max(stackPlacement.getAbsoluteY(), minY);
			 
			    // gives top-right point
			    // of intersection rectangle
			    int x2 = Math.min(stackPlacement.getAbsoluteEndX(), maxX);
			    int y2 = Math.min(stackPlacement.getAbsoluteEndY(), maxY);
				
			    long intersect = (x2 - x1 + 1) * (y2 - y1 + 1);
			    
			    sum += intersect;
			    
			    if(sum == max) {
			    	break;
			    }
			}
		}
		
		return (sum * 100) / stackValue.getArea();
	}

	protected boolean isBetter(Box referenceStackable, Box potentiallyBetterStackable) {
		// ****************************************
		// * Prefer the highest volume
		// ****************************************

		if(referenceStackable.getVolume() == potentiallyBetterStackable.getVolume()) {
			return referenceStackable.getWeight() < potentiallyBetterStackable.getWeight();
		}
		return referenceStackable.getVolume() < potentiallyBetterStackable.getVolume();
	}

	@Override
	public PlainPackagerResultBuilder newResultBuilder() {
		return new PlainPackagerResultBuilder().withPackager(this);
	}
}
