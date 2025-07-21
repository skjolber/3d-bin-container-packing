package com.github.skjolber.packing.packer;

import java.util.Comparator;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.AbstractIntermediatePlacementResultBuilder;
import com.github.skjolber.packing.api.packager.IntermediatePlacementResult;

public abstract class AbstractComparatorIntermediatePlacementResultBuilder<T extends IntermediatePlacementResult, B extends AbstractComparatorIntermediatePlacementResultBuilder<T, B>> extends AbstractIntermediatePlacementResultBuilder<T, B> {

	protected Comparator<T> intermediatePlacementResultComparator;
	protected Comparator<BoxItem> boxItemComparator;

	public B withBoxItemComparator(Comparator<BoxItem> boxItemComparator) {
		this.boxItemComparator = boxItemComparator;
		return (B)this;
	}
	
	public B withIntermediatePlacementResultComparator(Comparator<T> comparator) {
		this.intermediatePlacementResultComparator = comparator;
		return (B)this;
	}
	
	@Override
	public T build() {
		T result = null;
		
		long maxPointArea = extremePoints.getMaxArea();		
		long maxPointVolume = extremePoints.getMaxVolume();
		
		long maxWeight = container.getMaxLoadWeight() - stack.getWeight();
		
		for(int i = boxItemsStartIndex; i < boxItemsEndIndex; i++) {
			BoxItem boxItem = boxItems.get(i);
			
			Box box = boxItem.getBox();
			
			if(box.getVolume() > maxPointVolume) {
				continue;
			}
			
			if(box.getMinimumArea() > maxPointArea) {
				continue;
			}
			
			if(box.getWeight() > maxWeight) {
				continue;
			}
			
			// a negative integer, zero, or a positive integer as the 
			// first argument is less than, equal to, or greater than the
		    // second.
			if(result != null && boxItemComparator.compare(result.getBoxItem(), boxItem) >= 0) {
				continue;
			}

			FilteredPoints points = pointControls.getFilteredPoints(boxItem);

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

		}
		return result;
	}

	protected abstract T createIntermediatePlacementResult(int index, Point point, BoxStackValue stackValue);

}
