package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;

public class DefaultXSupportPoint2D<P extends Placement2D & Serializable> extends SimplePoint2D<P> implements XSupportPoint2D {

	private static final long serialVersionUID = 1L;
	/** range constrained to current minY */
	private final P xSupport;

	public DefaultXSupportPoint2D(int minX, int minY, int maxX, int maxY, P xSupport) {
		super(minX, minY, maxX, maxY);
		this.xSupport = xSupport;
	}

	@Override
	public boolean isXSupport(int x) {
		return xSupport.getAbsoluteX() <= x && x <= xSupport.getAbsoluteEndX();
	}

	public int getXSupportMinX() {
		return xSupport.getAbsoluteX();
	}

	public int getXSupportMaxX() {
		return xSupport.getAbsoluteEndX();
	}

	@Override
	public String toString() {
		return "DefaultXSupportPoint2D [" + +minX + "x" + minY + " " + maxX + "x" + maxY
				+ ", xSupportMinX=" + getXSupportMinX() + ", xSupportMaxX=" + getXSupportMaxX() + "]";
	}

	public Point2D<P> clone(int maxX, int maxY) {
		return new DefaultXSupportPoint2D<>(minX, minY, maxX, maxY, xSupport);
	}

	@Override
	public int getSupportedMaxX() {
		return xSupport.getAbsoluteEndX();
	}
	
	@Override
	public int getSupportedMinX() {
		return xSupport.getAbsoluteX();
	}
	
	@Override
	public SimplePoint2D<P> moveX(int x) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D<>(x, minY, maxX, maxY, xSupport);
		}
		return new DefaultPoint2D<>(x, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D<P> moveX(int x, P ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D<>(x, minY, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D<>(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public SimplePoint2D<P> moveY(int y) {
		return new DefaultPoint2D<>(minX, y, maxX, maxY);
	}

	@Override
	public SimplePoint2D<P> moveY(int y, P xSupport) {
		return new DefaultXSupportPoint2D<>(minX, y, maxX, maxY, xSupport);
	}
}
