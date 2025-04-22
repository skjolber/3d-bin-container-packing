package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.ep.AbstractFilteredPointsBuilder;
import com.github.skjolber.packing.api.ep.DefaultFilteredPoints;
import com.github.skjolber.packing.api.ep.EmptyFilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderSupplier;
import com.github.skjolber.packing.api.ep.Point;

public class HeavyItemsOnGroundLevel extends DefaultFilteredPoints {

	public static class Builder extends AbstractFilteredPointsBuilder<Builder> {

		private int maxWeight = -1;
		
		public Builder withMaxWeight(int maxCount) {
			this.maxWeight = maxCount;
			return this;
		}

		@Override
		public FilteredPoints build() {
			
			if(maxWeight == -1) {
				throw new IllegalStateException("Expected max count");
			}
			
			if(boxItem.getBox().getWeight() > maxWeight) {
				List<Point> values = new ArrayList<>();

				for(int i = 0; i < points.size(); i++) {
					Point point = points.get(i);
					if(point.getMinZ() == 0) {
						values.add(point);
					}
				}
				
				return new HeavyItemsOnGroundLevel(values);
			}
			
			if(isHeavyBoxes()) {
				return EmptyFilteredPoints.getInstance();
			}
			
			return points;
		}

		private boolean isHeavyBoxes() {
			for(int i = 0; i < items.size(); i++) {
				if(items.get(i).getBox().getWeight() > maxWeight) {
					return true;
				}
			}
			return false;
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final FilteredPointsBuilderSupplier newSupplier(int maxWeight) {
		return () -> HeavyItemsOnGroundLevel.newBuilder().withMaxWeight(maxWeight);
	}

	public HeavyItemsOnGroundLevel(List<Point> values) {
		this.values = values;
	}
	
	
	
}
