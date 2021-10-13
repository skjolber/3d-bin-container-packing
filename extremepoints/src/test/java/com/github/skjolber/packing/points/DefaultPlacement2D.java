package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultPlacement2D implements Placement2D {

	protected final int x;
	protected final int y;
	protected final int endX;
	protected final int endY;
	
	public DefaultPlacement2D(int x, int y, int endX, int endY) {
		super();
		this.x = x;
		this.y = y;
		this.endX = endX;
		this.endY = endY;
	}

	@Override
	public int getAbsoluteX() {
		return x;
	}

	@Override
	public int getAbsoluteY() {
		return y;
	}

	@Override
	public int getAbsoluteEndX() {
		return endX;
	}

	@Override
	public int getAbsoluteEndY() {
		return endY;
	}
	
	public boolean intersects(Placement2D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > endX || point.getAbsoluteEndY() < y || point.getAbsoluteY() > endY);
	}

}
