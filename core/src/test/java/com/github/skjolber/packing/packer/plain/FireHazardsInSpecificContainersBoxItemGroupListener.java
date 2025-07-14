package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.FilteredPoints;
import com.github.skjolber.packing.api.packager.AbstractBoxItemGroupControlsBuilder;
import com.github.skjolber.packing.api.packager.BoxItemGroupControls;
import com.github.skjolber.packing.api.packager.DefaultFilteredBoxItems;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public class FireHazardsInSpecificContainersBoxItemGroupListener implements BoxItemGroupControls {

	public static class Builder extends AbstractBoxItemGroupControlsBuilder<Builder> {

		@Override
		public BoxItemGroupControls build() {
			
			for(int i = 0; i < groups.size(); i++) {
				BoxItemGroup boxItemGroup = groups.get(i);
				
				if(isFireHazard(boxItemGroup)) {
					groups.remove(i);
					i--;
				}
			}
			return new FireHazardsInSpecificContainersBoxItemGroupListener(container, groups, points, stack);
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
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	protected final Container container;
	protected final FilteredBoxItemGroups groups;
	protected final Stack stack;
	protected final FilteredPoints points;

	public FireHazardsInSpecificContainersBoxItemGroupListener(Container container, FilteredBoxItemGroups groups, FilteredPoints points, Stack stack) {
		this.container = container;
		this.groups = groups;
		this.stack = stack;
		this.points = points;
	}

	@Override
	public void accepted(BoxItemGroup group) {
		// do nothing
	}

	@Override
	public void declined(BoxItemGroup group) {
		// do nothing
	}

	@Override
	public FilteredBoxItemGroups getFilteredBoxItemGroups() {
		return groups;
	}

}
