package com.github.skjolber.packing.api.packager.control.manifest;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.BoxItemGroupSource;
import com.github.skjolber.packing.api.packager.BoxItemSource;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Builder scaffold.
 * 
 * This covers initial filtering of box items and possibly in-flight filtering.
 */

@SuppressWarnings("unchecked")
public abstract class AbstractManifestControlsBuilder<B extends AbstractManifestControlsBuilder<B>> implements ManifestControlsBuilder<B> {

	protected Stack stack;
	protected Container container;
	protected BoxItemSource items;
	protected BoxItemGroupSource groups;
	protected PointSource points;
	
	public B withBoxItemGroups(BoxItemGroupSource groups) {
		this.groups = groups;
		return (B)this;
	}
	
	public B withPoints(PointSource points) {
		this.points = points;
		return (B)this;
	}

	public B withBoxItems(BoxItemSource input) {
		this.items = input;
		return (B)this;
	}
	
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	public abstract ManifestControls build();

}
