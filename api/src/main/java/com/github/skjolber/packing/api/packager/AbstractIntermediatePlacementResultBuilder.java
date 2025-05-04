package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.FilteredPointsBuilderFactory;

public abstract class AbstractIntermediatePlacementResultBuilder<R extends IntermediatePlacementResult, B extends AbstractIntermediatePlacementResultBuilder<R, B>> implements IntermediatePlacementResultBuilder<R, B> {

	protected BoxItemControls controls;
	protected ExtremePoints extremePoints;
	protected Container container;
	protected Stack stack;
	
	@Override
	public B withBoxItemControls(BoxItemControls controls) {
		this.controls = controls;
		return (B)this;
	}
	
	public B withStack(Stack stack) {
		this.stack = stack;
		return (B)this;
	}
	
	@Override
	public B withContainer(Container container) {
		this.container = container;
		return (B)this;
	}

	@Override
	public B withExtremePoints(ExtremePoints extremePoints) {
		this.extremePoints = extremePoints;
		return (B)this;
	}

}
