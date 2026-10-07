package com.github.skjolber.packing.packer.plain;

import java.util.function.Predicate;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.point.PointSource;

public class NoLightersWithPetrolManifestControls implements ManifestControls {

	public static class Builder extends AbstractManifestControlsBuilder<Builder> {

		@Override
		public ManifestControls build() {
			return new NoLightersWithPetrolManifestControls(container, items, points, stack);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static ManifestControlsBuilderFactory newFactory() {
		return () -> NoLightersWithPetrolManifestControls.newBuilder();
	}
	
	protected final Container container;
	protected final BoxItemSource items;
	protected final Stack stack;

	protected boolean lighter = false;
	protected boolean petrol = false;

	public NoLightersWithPetrolManifestControls(Container container, BoxItemSource items, PointSource filteredPoints, Stack stack) {
		this.container = container;
		this.items = items;
		this.stack = stack;
	}

	@Override
	public void accepted(BoxItem group) {
		// do nothing
		
		if(!lighter) {
			lighter = isLighter(group);
			
			if(lighter) {
				// remove boxes which contain petrol
				remove(this::isPetrol);
			}
		}
		if(!petrol) {
			petrol = isPetrol(group);
			
			if(petrol) {
				// remove groups which contain matches
				
				remove(this::isLighter);
			}
		}
	}

	private void remove(Predicate<BoxItem> test) {
		for(int i = 0; i < items.size(); i++) {
			BoxItem item = items.get(i);
			
			if(test.test(item)) {
				items.remove(i);
				i--;
			}
		}
	}

	private boolean isPetrol(BoxItem item) {
		return item.getBox().getId().startsWith("petrol-");
	}

	private boolean isLighter(BoxItem item) {
		return item.getBox().getId().startsWith("lighter-");
	}

}
