package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;

public class DefaultPoint2D<P extends Placement2D & Serializable> extends Point2D<P> {

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

	public Point2D<P> clone(int maxX, int maxY) {
		return new DefaultPoint2D<>(minX, minY, maxX, maxY);
	}

	@Override
	public Point2D<P> moveX(int x) {
		return new DefaultPoint2D<>(x, minY, maxX, maxY);
	}

	@Override
	public Point2D<P> moveY(int y) {
		return new DefaultPoint2D<>(minX, y, maxX, maxY);
	}

	@Override
	public Point2D<P> moveX(int x, P ySupport) {
		return new DefaultYSupportPoint2D<>(x, minY, maxY, maxY, ySupport);
	}

	@Override
	public Point2D<P> moveY(int y, P xSupport) {
		return new DefaultXSupportPoint2D<>(minX, y, maxX, maxX, xSupport);
	}

}
