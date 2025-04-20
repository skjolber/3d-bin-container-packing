package com.github.skjolber.packing.packer.plain;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilder;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemListener;
import com.github.skjolber.packing.api.packager.CompositeContainerItem;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.ep.points3d.MarkResetExtremePoints3D;
import com.github.skjolber.packing.packer.AbstractPackagerBuilder;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResult;
import com.github.skjolber.packing.packer.DefaultIntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.IntermediatePackagerResultComparator;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * Selects the box with the highest volume first, then places it into the point with the lowest volume.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */

public class PlainPackager extends AbstractPlainPackager {

	public static class PlainPlacementResult extends PlacementResult {

		protected long bestPointSupportPercent = -1;

		public PlainPlacementResult(BoxItem boxItem, BoxStackValue stackValue, Point point, long bestPointSupportPercent) {
			super(boxItem, stackValue, point);
			
			this.bestPointSupportPercent = bestPointSupportPercent;
		}
	}
	
	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder extends AbstractPackagerBuilder<PlainPackager, Builder> {

		public PlainPackager build() {
			if(comparator == null) {
				comparator = new DefaultIntermediatePackagerResultComparator();
			}
			return new PlainPackager(comparator);
		}
	}

	public PlainPackager(IntermediatePackagerResultComparator comparator) {
		super(comparator);
	}
	
	@Override
	public DefaultIntermediatePackagerResult pack(List<BoxItem> boxes, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		Stack stack = new Stack();

		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container container = containerItem.getContainer();

		List<BoxItem> scopedBoxItems = boxes.stream().filter(s -> container.fitsInside(s)).collect(Collectors.toList());

		DefaultFilteredBoxItems filteredBoxItems = new DefaultFilteredBoxItems(scopedBoxItems);

		ExtremePoints3D extremePoints3D = new ExtremePoints3D(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemArea(filteredBoxItems), getMinBoxItemVolume(filteredBoxItems));

		BoxItemListener listener = compositeContainerItem.createBoxItemListener(container, stack, filteredBoxItems, extremePoints3D);

		boolean canContainLastBox = filteredBoxItems.size() == boxes.size();

		int remainingLoadWeight = container.getMaxLoadWeight();

		while (!extremePoints3D.isEmpty() && remainingLoadWeight > 0 && !filteredBoxItems.isEmpty()) {
			if(interrupt.getAsBoolean()) {
				// fit2d below might have returned due to deadline
				return null;
			}
			
			PlacementResult result = findBestPoint(filteredBoxItems, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderSupplier(), container, stack);
			if(result == null) {
				break;
			}

			Point point = result.point;
			
			StackPlacement stackPlacement = new StackPlacement(null, result.boxItem, result.stackValue, point.getMinX(), point.getMinY(), point.getMinZ());
			stack.add(stackPlacement);
			extremePoints3D.add(result.point.getIndex(), stackPlacement);

			result.boxItem.decrement();

			listener.accepted(result.boxItem);
			
			filteredBoxItems.clearEmpty();
	
			if(!filteredBoxItems.isEmpty()) {
				// check if we need to recalculate minimums
				boolean minArea = result.stackValue.getArea() == extremePoints3D.getMinAreaLimit();
				boolean minVolume = extremePoints3D.getMinVolumeLimit() == result.stackValue.getVolume();
				if(minArea && minVolume) {
					extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemArea(filteredBoxItems), getMinBoxItemVolume(filteredBoxItems));
				} else if(minArea) {
					extremePoints3D.setMinimumAreaLimit(getMinBoxItemArea(filteredBoxItems));
				} else if(minVolume) {
					extremePoints3D.setMinimumVolumeLimit(getMinBoxItemVolume(filteredBoxItems));
				}
			}

			remainingLoadWeight -= result.boxItem.getBox().getWeight();
		}
		
		return new DefaultIntermediatePackagerResult(compositeContainerItem.getContainerItem(), stack, canContainLastBox && filteredBoxItems.isEmpty());
	}
	
	public DefaultIntermediatePackagerResult pack(List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, CompositeContainerItem compositeContainerItem, PackagerInterruptSupplier interrupt) {
		ContainerItem containerItem = compositeContainerItem.getContainerItem();
		Container targetContainer = containerItem.getContainer();
		
		List<BoxItemGroup> scopedBoxItemGroups = getFitsInside(boxItemGroups, targetContainer);

		boolean canContainLastBox = scopedBoxItemGroups.size() == boxItemGroups.size();
		
		DefaultFilteredBoxItemGroups filteredBoxItemGroups = new DefaultFilteredBoxItemGroups(scopedBoxItemGroups);

		Stack stack = new Stack();

		MarkResetExtremePoints3D extremePoints3D = new MarkResetExtremePoints3D(targetContainer.getLoadDx(), targetContainer.getLoadDy(), targetContainer.getLoadDz());
		extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemGroupArea(scopedBoxItemGroups), getMinBoxItemGroupVolume(scopedBoxItemGroups));

		BoxItemGroupListener listener = compositeContainerItem.createBoxItemGroupListener(targetContainer, stack, filteredBoxItemGroups, extremePoints3D);

		groups:
		while (!extremePoints3D.isEmpty() && !filteredBoxItemGroups.isEmpty()) {
			int bestBoxItemGroupIndex = getBestBoxItemGroup(targetContainer, scopedBoxItemGroups, itemGroupOrder, extremePoints3D);
			if(bestBoxItemGroupIndex == -1) {
				break;
			}

			BoxItemGroup boxItemGroup = filteredBoxItemGroups.remove(bestBoxItemGroupIndex);

			extremePoints3D.mark();
			
			while(!boxItemGroup.isEmpty()) {
				
				DefaultFilteredBoxItems items = new DefaultFilteredBoxItems(boxItemGroup.getItems()); 
				
				PlacementResult bestPoint = findBestPoint(items, extremePoints3D, compositeContainerItem.getFilteredPointsBuilderSupplier(), targetContainer, stack);
				if(bestPoint == null) {
					// discard the whole group
					extremePoints3D.reset();
					
					continue groups;
				}
				
				boxItemGroup.decrement(bestPoint.boxItem.getIndex());

				StackPlacement stackPlacement = new StackPlacement(boxItemGroup, bestPoint.boxItem, bestPoint.stackValue, bestPoint.point.getMinX(), bestPoint.point.getMinY(), bestPoint.point.getMinZ());
				stack.add(stackPlacement);
				extremePoints3D.add(bestPoint.point, stackPlacement);

				if(!boxItemGroup.isEmpty()) {
					boolean minArea = bestPoint.stackValue.getArea() == extremePoints3D.getMinAreaLimit();
					boolean minVolume = extremePoints3D.getMinVolumeLimit() == bestPoint.stackValue.getVolume();
					if(minArea && minVolume) {
						extremePoints3D.setMinimumAreaAndVolumeLimit(getMinBoxItemGroupArea(scopedBoxItemGroups), getMinBoxItemGroupVolume(scopedBoxItemGroups));
					} else if(minArea) {
						extremePoints3D.setMinimumAreaLimit(getMinBoxItemGroupArea(scopedBoxItemGroups));
					} else if(minVolume) {
						extremePoints3D.setMinimumVolumeLimit(getMinBoxItemGroupVolume(scopedBoxItemGroups));
					}
				}

			}
			
			if(targetContainer.getMaxLoadWeight() < extremePoints3D.getUsedWeight()) {
				throw new RuntimeException();
			}
			
			// successfully stacked group
			
			listener.accepted(boxItemGroup);

		}
		
		return new DefaultIntermediatePackagerResult(null, stack, canContainLastBox && scopedBoxItemGroups.isEmpty());
	}

	private boolean isBetter(BoxItemGroup bestBoxItemGroup, BoxItemGroup group, Container targetContainer) {
		if(bestBoxItemGroup.getVolume() < group.getVolume()) {
			return true;
		}
		if(bestBoxItemGroup.getVolume() == group.getVolume()) {
			return bestBoxItemGroup.getWeight() < group.getWeight();
		}
		return false;
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

	public PlacementResult findBestPoint(FilteredBoxItems boxItems, ExtremePoints3D extremePoints3D, Supplier<FilteredPointsBuilder<?>> filteredPointsBuilderSupplier, Container container, Stack stack) {
		PlainPlacementResult result = null;
		
		long maxPointArea = extremePoints3D.getMaxArea();		
		long maxPointVolume = extremePoints3D.getMaxVolume();
		
		for (int i = 0; i < boxItems.size(); i++) {
			BoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();
			
			if(box.getVolume() > maxPointVolume) {
				continue;
			}
			
			if(box.getMinimumArea() > maxPointArea) {
				continue;
			}
			
			FilteredPoints points;
			if(filteredPointsBuilderSupplier == null) {
				points = extremePoints3D;
			} else {
				points = filteredPointsBuilderSupplier.get()
					.withBoxItems(boxItem)
					.withContainer(container)
					.withPoints(extremePoints3D)
					.withStack(stack)
					.build();
			}
			
			for (int k = 0; k < points.size(); k++) {
				Point point3d = points.get(k);

				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
		
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
	
					long pointSupportPercent; // cache for costly measurement
					if(result != null) {
						Point bestPoint = result.point;
						
						if(point3d.getMinZ() > bestPoint.getMinZ()) {
							continue;
						}
						
						pointSupportPercent = calculateXYSupportPercent(extremePoints3D, point3d, stackValue);
						
						if(point3d.getMinZ() == bestPoint.getMinZ()) {
							if(pointSupportPercent < result.bestPointSupportPercent) {
								continue;
							}
						
							if(stackValue.getArea() <= result.stackValue.getArea()) {
								continue;
							}
						}
					} else {
						pointSupportPercent = calculateXYSupportPercent(extremePoints3D, point3d, stackValue);
					}
					
					result = new PlainPlacementResult(boxItem, stackValue, point3d, pointSupportPercent);
				}
			}

		}
		return result;
	}
	

	public int getBestBoxItemGroup(Container container, List<BoxItemGroup> boxItemGroups, Order itemGroupOrder, ExtremePoints3D extremePoints3D) {
		long maxPointVolume = extremePoints3D.getMaxVolume();
		long maxPointArea = extremePoints3D.getMaxArea();

		long maxTotalPointVolume = container.getLoadVolume() -  extremePoints3D.getUsedVolume();
		
		long maxTotalWeight = container.getMaxLoadWeight() -  extremePoints3D.getUsedWeight();

		// can any of the groups be split?

		if(itemGroupOrder == Order.FIXED) {
			BoxItemGroup bestBoxItemGroup = boxItemGroups.get(0);

			if(bestBoxItemGroup.getVolume() > maxTotalPointVolume) {
				return -1;
			}
			if(bestBoxItemGroup.getWeight() > maxTotalWeight) {
				return -1;
			}
			for (int i = 0; i < bestBoxItemGroup.size(); i++) {
				BoxItem boxItem = bestBoxItemGroup.get(i);
				
				Box box = boxItem.getBox();
				if(box.getVolume() > maxPointVolume) {
					return -1;
				}

				if(box.getMinimumArea() > maxPointArea) {
					return -1;
				}
			}
			return 0;
		} else {
			BoxItemGroup bestBoxItemGroup = null;
			int bestIndex = -1;
			
			// find next best group
			bestGroupSearch:
			for (int l = 0; l < boxItemGroups.size(); l++) {
				BoxItemGroup group = boxItemGroups.get(l);
				
				if(group.getVolume() > maxTotalPointVolume) {
					continue;
				}
				if(group.getWeight() > maxTotalWeight) {
					continue;
				}
				for (int i = 0; i < group.size(); i++) {
					BoxItem boxItem = group.get(i);
					
					Box box = boxItem.getBox();
					if(box.getVolume() > maxPointVolume) {
						continue bestGroupSearch;
					}

					if(box.getMinimumArea() > maxPointArea) {
						continue bestGroupSearch;
					}
				}
				
				if(bestBoxItemGroup == null || isBetter(bestBoxItemGroup, group, container)) {
					bestBoxItemGroup = group;
					bestIndex = l;
				}
			}
			return bestIndex;
		}
	}
}
