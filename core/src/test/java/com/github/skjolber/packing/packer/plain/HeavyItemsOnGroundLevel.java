package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ep.AbstractFilteredPointsBuilder;
import com.github.skjolber.packing.api.ep.DefaultFilteredPoints;
import com.github.skjolber.packing.api.ep.EmptyFilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderFactory;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.api.packager.AbstractBoxItemControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.DefaultBoxItemControls;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class HeavyItemsOnGroundLevel extends DefaultBoxItemControls {

	public static class Builder extends AbstractBoxItemControlsBuilder<Builder> {

		private int maxWeight = -1;
		
		public Builder withMaxWeight(int maxCount) {
			this.maxWeight = maxCount;
			return this;
		}

		@Override
		public BoxItemControls build() {
			
			if(maxWeight == -1) {
				throw new IllegalStateException("Expected max weight limit");
			}
			
			return new HeavyItemsOnGroundLevel(items, points, maxWeight);
		}


	}
	
	protected final int maxWeight;

	public HeavyItemsOnGroundLevel(FilteredBoxItems filteredBoxItems, FilteredPoints filteredPoints, int maxWeight) {
		super(filteredBoxItems, filteredPoints);
		this.maxWeight = maxWeight;
	}

	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final BoxItemControlsBuilderFactory newFactory(int maxWeight) {
		return () -> HeavyItemsOnGroundLevel.newBuilder().withMaxWeight(maxWeight);
	}

	@Override
	public FilteredPoints getPoints(BoxItem boxItem) {
		if(boxItem.getBox().getWeight() > maxWeight) {
			List<Point> values = new ArrayList<>();

			for(int i = 0; i < filteredPoints.size(); i++) {
				Point point = filteredPoints.get(i);
				if(point.getMinZ() == 0) {
					values.add(point);
				}
			}
			
			return new DefaultFilteredPoints(values);
		}
		
		if(isHeavyBoxes()) {
			return EmptyFilteredPoints.getInstance();
		}
		
		return filteredPoints;
	}

	private boolean isHeavyBoxes() {
		for(int i = 0; i < filteredBoxItems.size(); i++) {
			if(filteredBoxItems.get(i).getBox().getWeight() > maxWeight) {
				return true;
			}
		}
		return false;
	}	
	
}
