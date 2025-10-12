package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointCalculator;
import com.github.skjolber.packing.api.point.PointSource;

public abstract class AbstractComparatorPlacementControls<T extends Placement> extends AbstractPlacementControls<T> {

	protected Comparator<T> placementComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, PointCalculator pointCalculator, Container container, Stack stack,
			BoxPriority priority, Comparator<T> placementComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, pointCalculator, container, stack, priority);
		
		this.placementComparator = placementComparator;
		this.boxItemComparator = boxItemComparator;
	}

	public T getPlacement(int offset, int length) {
		T result = null;
		
		long maxPointArea = pointCalculator.getMaxArea();
		
		// max volume and weight should already be accounted for by packager
		
		for(int i = offset; i < length; i++) {
			BoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();
			
			if(priority == BoxPriority.NONE) {
				// a negative integer, zero, or a positive integer as the 
				// first argument is less than, equal to, or greater than the
			    // second.
				if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem) >= 0) {
					continue;
				}
			}
			
			PointSource points = pointControls.getPoints(boxItem);

			for (Point point3d : points) {
				for (BoxStackValue stackValue : box.getStackValues()) {
					if(stackValue.getArea() > maxPointArea) {
						continue;
					}
		
					if(!point3d.fits3D(stackValue)) {
						continue;
					}
					T placementResult = createPlacement(point3d, stackValue);
					if(placementResult == null) {
						continue;
					}
					if(result != null && placementComparator.compare(result, placementResult) >= 0) {
						continue;
					}
					
					result = placementResult;
				} 
			}
			
			if(priority == BoxPriority.CRONOLOGICAL) {
				// even if null
				break;
			}
			if(priority == BoxPriority.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				break;
			}
			
		}
		return result;
	}

	protected abstract T createPlacement(Point point3d, BoxStackValue stackValue);

}
