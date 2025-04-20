package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupListener;
import com.github.skjolber.packing.api.packager.BoxItemGroupListenerBuilder;
import com.github.skjolber.packing.api.packager.FilteredBoxItemGroups;

public class FireHazardsInSpecificContainersBoxItemGroupListener implements BoxItemGroupListener {

	public static class Builder extends BoxItemGroupListenerBuilder<Builder> {

		@Override
		public BoxItemGroupListener build() {
			
			for(int i = 0; i < groups.size(); i++) {
				BoxItemGroup boxItemGroup = groups.get(i);
				
				if(isFireHazard(boxItemGroup)) {
					groups.remove(i);
					i--;
				}
			}
			return new FireHazardsInSpecificContainersBoxItemGroupListener(container, groups, stack);
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
	
	protected final Container container;
	protected final FilteredBoxItemGroups groups;
	protected final Stack stack;

	public FireHazardsInSpecificContainersBoxItemGroupListener(Container container, FilteredBoxItemGroups groups, Stack stack) {
		this.container = container;
		this.groups = groups;
		this.stack = stack;
	}

	@Override
	public void accepted(BoxItemGroup group) {
		// do nothing
	}

}
