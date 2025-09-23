package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxPriority;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.AbstractPlacementControls;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.PointControls;

public abstract class AbstractComparatorPlacementControls<T extends Placement> extends AbstractPlacementControls<T> {

	protected Comparator<T> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public AbstractComparatorPlacementControls(BoxItemSource boxItems, int boxItemsStartIndex, int boxItemsEndIndex,
			PointControls pointControls, ExtremePoints extremePoints, Container container, Stack stack,
			BoxPriority priority, Comparator<T> intermediatePlacementResultComparator, Comparator<BoxItem> boxItemComparator) {
		super(boxItems, boxItemsStartIndex, boxItemsEndIndex, pointControls, extremePoints, container, stack, priority);
		
		this.intermediatePlacementResultComparator = intermediatePlacementResultComparator;
		this.boxItemComparator = boxItemComparator;
	}

	public T getPlacement(int offset, int length) {
		T result = null;
		
		long maxPointArea = extremePoints.getMaxArea();
		
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
					T intermediatePlacementResult = createIntermediatePlacementResult(i, point3d, stackValue);
					
					if(result != null && intermediatePlacementResultComparator.compare(result, intermediatePlacementResult) >= 0) {
						continue;
					}
					
					result = intermediatePlacementResult;
				} 
			}
			
			if(priority == BoxPriority.CRONOLOGICAL) {
				// even if null
				return result;
			}
			if(priority == BoxPriority.CRONOLOGICAL_ALLOW_SKIPPING && result != null) {
				return result;
			}
			
		}
		return result;
	}


	protected abstract T createIntermediatePlacementResult(int i, Point point3d, BoxStackValue stackValue);

}
