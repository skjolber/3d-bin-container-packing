package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.PointSource;
import com.github.skjolber.packing.api.packager.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.ManifestControls;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;

public class FireHazardsInSpecificContainersBoxItemGroupListener implements ManifestControls {

	public static class Builder extends AbstractManifestControlsBuilder<Builder> {

		@Override
		public ManifestControls build() {
			
			BoxItemGroupSource groups = items.getGroups();			
			
			for(int i = 0; i < groups.size(); i++) {
				BoxItemGroup boxItemGroup = groups.get(i);
				
				if(isFireHazard(boxItemGroup)) {
					groups.remove(i);
					i--;
				}
			}
			return new FireHazardsInSpecificContainersBoxItemGroupListener(container, items, points, stack);
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
	protected final BoxItemSource boxItems;
	protected final Stack stack;
	protected final PointSource points;

	public FireHazardsInSpecificContainersBoxItemGroupListener(Container container, BoxItemSource boxItems, PointSource points, Stack stack) {
		this.container = container;
		this.boxItems = boxItems;
		this.stack = stack;
		this.points = points;
	}

}
