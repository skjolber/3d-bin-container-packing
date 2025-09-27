package com.github.skjolber.packing.api.packager.control.manifest;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items vs container and possibly in-flight box vs box filtering.
 */

public interface ManifestControlsBuilder {

	ManifestControlsBuilder withPoints(PointSource points);

	ManifestControlsBuilder withBoxItems(BoxItemSource input);
	
	ManifestControlsBuilder withBoxItemGroups(BoxItemGroupSource input);
	
	ManifestControlsBuilder withContainer(Container container);
	
	ManifestControlsBuilder withStack(Stack stack);
	
	ManifestControls build();

}
