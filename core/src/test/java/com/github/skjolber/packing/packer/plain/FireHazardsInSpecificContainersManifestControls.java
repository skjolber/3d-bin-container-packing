package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.RemainingBoxItem;
import com.github.skjolber.packing.api.packager.RemainingBoxItemGroup;
import com.github.skjolber.packing.api.packager.control.manifest.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.point.PointSource;

public class FireHazardsInSpecificContainersManifestControls implements ManifestControls {

	public static class Builder extends AbstractManifestControlsBuilder<Builder> {

		@Override
		public ManifestControls build() {
			
			BoxItemGroupSource groups = items.getGroups();			
			
			for(int i = 0; i < groups.size(); i++) {
				RemainingBoxItemGroup boxItemGroup = groups.get(i);
				
				if(isFireHazard(boxItemGroup)) {
					groups.remove(i);
					i--;
				}
			}
			return new FireHazardsInSpecificContainersManifestControls(container, items, points, stack);
		}

		private boolean isFireHazard(RemainingBoxItemGroup boxItemGroup) {
			for(int i = 0; i < boxItemGroup.size(); i++) {
				RemainingBoxItem item = boxItemGroup.get(i);
				if(isFireHazard(item)) {
					return true;
				}
			}
			return false;
		}

		private boolean isFireHazard(RemainingBoxItem item) {
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

	public FireHazardsInSpecificContainersManifestControls(Container container, BoxItemSource boxItems, PointSource points, Stack stack) {
		this.container = container;
		this.boxItems = boxItems;
		this.stack = stack;
		this.points = points;
	}

}
