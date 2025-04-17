package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemGroupListenerBuilder;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;

public class MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener implements BoxItemGroupListener {

	public static class Builder extends BoxItemGroupListenerBuilder<Builder> {

		private int maxCount = -1;
		
		public Builder withMaxCount(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}
		
		@Override
		public BoxItemGroupListener build() {
			if(maxCount == -1) {
				throw new IllegalStateException("Expected max count");
			}
			return new MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener(container, groups, stack, maxCount);
		}

	}
	
	protected final Container container;
	protected final FilteredBoxItemGroups groups;
	protected final Stack stack;
	protected final int maxCount;
	protected int count = 0;

	public MaxFireHazardBoxItemGroupsPerContainerBoxItemGroupListener(Container container, FilteredBoxItemGroups groups, Stack stack, int maxCount) {
		this.container = container;
		this.groups = groups;
		this.stack = stack;
		this.maxCount = maxCount;
	}

	@Override
	public void packedBoxItemGroup(int index) {
		// do nothing
		
		BoxItemGroup removed = groups.remove(index);
		if(isFireHazard(removed)) {
			count++;
			
			if(count >= maxCount) {
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
		return item.getBox().getId().startsWith("fire-");
	}
}
