package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.api.ep.YSupportPoint2D;

public class DefaultYSupportPoint2D<P extends Placement2D & Serializable> extends Point2D<P> implements YSupportPoint2D {

	private static final long serialVersionUID = 1L;
	/** range constrained to current minX */
	private final P ySupport;

	public DefaultYSupportPoint2D(int minX, int minY, int maxX, int maxY, P ySupport) {
		super(minX, minY, maxX, maxY);
		this.ySupport = ySupport;
	}

	@Override
	public boolean isYSupport(int y) {
		return ySupport.getAbsoluteY() <= y && y <= ySupport.getAbsoluteEndY();
	}

	public int getYSupportMinY() {
		return ySupport.getAbsoluteY();
	}

	public int getYSupportMaxY() {
		return ySupport.getAbsoluteEndY();
	}

	@Override
	public String toString() {
		return "DefaultYSupportPoint2D [" + minX + "x" + minY + " " + maxX + "x" + maxY
				+ ", ySupportMinY=" + getYSupportMinY() + ", ySupportMaxY=" + getYSupportMaxY() + "]";
	}

	public Point2D<P> clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D<>(minX, minY, maxX, maxY, ySupport);
	}

	@Override
	public Placement2D getYSupport() {
		return ySupport;
	}

	@Override
	public Point2D<P> moveY(int y) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D<P>(minX, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D<P>(minX, y, maxX, maxY);
	}

	@Override
	public Point2D<P> moveY(int y, P xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D<>(minX, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D<>(minX, y, maxX, maxY, xSupport);
	}

	@Override
	public Point2D<P> moveX(int x, P ySupport) {
		return new DefaultYSupportPoint2D<>(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public Point2D<P> moveX(int x) {
		return new DefaultPoint2D<>(x, minY, maxX, maxY);
	}

}
