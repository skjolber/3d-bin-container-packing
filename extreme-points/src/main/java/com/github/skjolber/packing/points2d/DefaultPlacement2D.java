package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultPlacement2D implements Placement2D {

	protected final int x;
	protected final int y;
	protected final int endX;
	protected final int endY;
	
	protected final List<DefaultPlacement2D> supports;

	public DefaultPlacement2D(int x, int y, int endX, int endY) {
		this(x, y, endX, endY, new ArrayList<>());
	}

	public DefaultPlacement2D(int x, int y, int endX, int endY, List<DefaultPlacement2D> supports) {
		super();
		this.x = x;
		this.y = y;
		this.endX = endX;
		this.endY = endY;
		
		this.supports = supports;
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

	@Override
	public List<? extends Placement2D> getSupports2D() {
		return supports;
	}
}
