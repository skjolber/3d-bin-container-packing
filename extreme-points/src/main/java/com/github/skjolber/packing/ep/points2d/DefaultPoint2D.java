package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultPoint2D extends SimplePoint2D {

	private static final long serialVersionUID = 1L;

	public DefaultPoint2D(int minX, int minY, int maxX, int maxY) {
		super(minX, minY, maxX, maxY);
	}

	@Override
	public boolean isXSupport(int x) {
		return false;
	}

	@Override
	public boolean isYSupport(int y) {
		return false;
	}

	public SimplePoint2D clone(int maxX, int maxY) {
		return new DefaultPoint2D(minX, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveX(int x) {
		return new DefaultPoint2D(x, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveY(int y) {
		return new DefaultPoint2D(minX, y, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveX(int x, StackPlacement ySupport) {
		return new DefaultYSupportPoint2D(x, minY, maxY, maxY, ySupport);
	}

	@Override
	public SimplePoint2D moveY(int y, StackPlacement xSupport) {
		return new DefaultXSupportPoint2D(minX, y, maxX, maxX, xSupport);
	}

	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new DefaultPoint2D(minX, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D clone() {
		return new DefaultPoint2D(minX, minY, maxX, maxY);
	}

}
