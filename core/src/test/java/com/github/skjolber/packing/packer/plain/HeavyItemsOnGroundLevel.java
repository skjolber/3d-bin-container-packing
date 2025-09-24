package com.github.skjolber.packing.packer.plain;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.AbstractPointControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.DefaultPointSource;
import com.github.skjolber.packing.api.point.EmptyPointSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointSource;

public class HeavyItemsOnGroundLevel extends DefaultPointControls {

	public static class Builder extends AbstractPointControlsBuilder<Builder> {

		private int maxWeight = -1;
		
		public Builder withMaxWeight(int maxCount) {
			this.maxWeight = maxCount;
			return this;
		}

		@Override
		public PointControls build() {
			
			if(maxWeight == -1) {
				throw new IllegalStateException("Expected max weight limit");
			}
			
			return new HeavyItemsOnGroundLevel(items, points, maxWeight);
		}

	}
	
	protected final int maxWeight;
	protected BoxItemSource filteredBoxItems;

	public HeavyItemsOnGroundLevel(BoxItemSource filteredBoxItems, PointSource filteredPoints, int maxWeight) {
		super(filteredPoints);
		this.filteredBoxItems = filteredBoxItems;
		this.maxWeight = maxWeight;
	}

	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final PointControlsBuilderFactory newFactory(int maxWeight) {
		return () -> HeavyItemsOnGroundLevel.newBuilder().withMaxWeight(maxWeight);
	}

	@Override
	public PointSource getPoints(BoxItem boxItem) {
		if(boxItem.getBox().getWeight() > maxWeight) {
			List<Point> values = new ArrayList<>();

			for(int i = 0; i < filteredPoints.size(); i++) {
				Point point = filteredPoints.get(i);
				if(point.getMinZ() == 0) {
					values.add(point);
				}
			}
			
			return new DefaultPointSource(values);
		}
		
		if(isHeavyBoxes()) {
			// alternatively make point comparator perform the same
			return EmptyPointSource.getInstance();
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
