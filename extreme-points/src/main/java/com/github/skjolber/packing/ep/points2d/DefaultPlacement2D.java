package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultPlacement2D implements Placement2D, Serializable {

	private static final long serialVersionUID = 1L;

	protected final int x;
	protected final int y;
	protected final int endX;
	protected final int endY;

	public DefaultPlacement2D(int x, int y, int endX, int endY) {
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

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + x + "x" + y + ", " + endX + "x" + endY + "]";
	}

	@Override
	public boolean intersects2D(Placement2D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > endX || point.getAbsoluteEndY() < y || point.getAbsoluteY() > endY);
	}

}
