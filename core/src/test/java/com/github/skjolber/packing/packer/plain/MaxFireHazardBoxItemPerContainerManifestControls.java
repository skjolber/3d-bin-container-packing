package com.github.skjolber.packing.packer.plain;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.packager.control.manifest.AbstractManifestControlsBuilder;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControls;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.point.PointSource;

public class MaxFireHazardBoxItemPerContainerManifestControls implements ManifestControls {

	public static final String KEY = "fireHazard";
	
	public static class Builder extends AbstractManifestControlsBuilder<Builder> {

		private int maxCount = -1;
		
		public Builder withMaxCount(int maxCount) {
			this.maxCount = maxCount;
			return this;
		}
		
		@Override
		public ManifestControls build() {
			if(maxCount == -1) {
				throw new IllegalStateException("Expected max count");
			}
			return new MaxFireHazardBoxItemPerContainerManifestControls(container, items, points, stack, maxCount);
		}

	}
	
	public static final Builder newBuilder() {
		return new Builder();
	}
	
	public static final ManifestControlsBuilderFactory newFactory(int maxCount) {
		return () -> new Builder().withMaxCount(maxCount);
	}
	
	protected final Container container;
	protected final BoxItemSource items;
	protected final Stack stack;
	protected final int maxCount;
	protected int count = 0;

	public MaxFireHazardBoxItemPerContainerManifestControls(Container container, BoxItemSource items, PointSource points, Stack stack, int maxCount) {
		this.container = container;
		this.items = items;
		this.stack = stack;
		this.maxCount = maxCount;
	}

	@Override
	public void accepted(BoxItem boxItem) {
		// do nothing
		
		if(isFireHazard(boxItem)) {
			count++;
			
			if(count >= maxCount) {
				removeFireHazards();
			}
		}
	}

	private void removeFireHazards() {
		for(int i = 0; i < items.size(); i++) {
			if(isFireHazard(items.get(i))) {
				items.remove(i);
				i--;
			}
		}
	}

	private boolean isFireHazard(BoxItem item) {
		Box box = item.getBox();
		
		Boolean b = box.getProperty(KEY);
		return b != null && b;
	}
	
}
