package com.github.skjolber.packing.api;

import java.util.List;

public class SupportStackPlacement extends StackPlacement implements SupportPlacement3D {

	private static final long serialVersionUID = 1L;

	protected List<VerticalSupport> bottomSupports;

	protected List<VerticalSupport> topSupports;

	protected List<HorizontalSupport> leftSupports;

	protected List<HorizontalSupport> rightSupports;

	protected List<HorizontalSupport> frontSupports;

	protected List<HorizontalSupport> rearSupports;

	public SupportStackPlacement() {
		super();
	}

	public SupportStackPlacement(Stackable stackable, StackValue value, int x, int y, int z) {
		super(stackable, value, x, y, z);
	}

	@Override
	public List<VerticalSupport> getBottomSupports() {
		return bottomSupports;
	}

	@Override
	public List<VerticalSupport> getTopSupports() {
		return topSupports;
	}

	@Override
	public List<HorizontalSupport> getLeftSupports() {
		return leftSupports;
	}

	@Override
	public List<HorizontalSupport> getRightSupports() {
		return rightSupports;
	}

	@Override
	public List<HorizontalSupport> getFrontSupports() {
		return frontSupports;
	}

	@Override
	public List<HorizontalSupport> getRearSupports() {
		return rearSupports;
	}
	
}
