package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.packager.AbstractBoxItemControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemControls;
import com.github.skjolber.packing.api.packager.BoxItemControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener implements BoxItemControls {

	public static final String KEY = "fireHazard";

	public static class Builder extends AbstractBoxItemControlsBuilder<Builder> {

		private int maxCount = -1;
		
		public Builder withMaxCount(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}
		
		@Override
		public BoxItemControls build() {
			if(maxCount == -1) {
				throw new IllegalStateException("Expected max count");
			}
			return new MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener(container, items, points, stack, maxCount);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	protected final Container container;
	protected final FilteredBoxItems boxItems;
	protected final Stack stack;
	protected final int maxCount;
	protected int count = 0;
	protected final FilteredPoints points;

	public MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener(Container container, FilteredBoxItems boxItems, FilteredPoints points, Stack stack, int maxCount) {
		this.container = container;
		this.boxItems = boxItems;
		this.stack = stack;
		this.maxCount = maxCount;
		this.points = points;
	}

	@Override
	public void attemptSuccess(BoxItemGroup group) {
		// do nothing
		
		if(isFireHazard(group)) {
			count++;
			
			if(count >= maxCount) {
				
				FilteredBoxItemGroups groups = boxItems.getGroups();
				
				// remove other fire hazards
				for(int i = 0; i < groups.size(); i++) {
					BoxItemGroup boxItemGroup = groups.get(i);
					
					if(isFireHazard(boxItemGroup)) {
						groups.remove(i);
						i--;
					}
				}
			}
		}
	}

	private boolean isFireHazard(BoxItemGroup boxItemGroup) {
		for(int i = 0; i < boxItemGroup.size(); i++) {
			BoxItem item = boxItemGroup.get(i);
			if(isFireHazard(item)) {
				return true;
			}
		}
		return false;
	}

	private boolean isFireHazard(BoxItem item) {
		Box box = item.getBox();
		
		Boolean b = box.getProperty(KEY);
		return b != null && b;
	}
	
	public static BoxItemControlsBuilderFactory newFactory(int i) {
		return () -> newBuilder().withMaxCount(i);
	}
}
