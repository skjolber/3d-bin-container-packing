package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;

public class DefaultXYSupportPoint2D<P extends Placement2D & Serializable> extends SimplePoint2D<P> implements XSupportPoint2D, YSupportPoint2D {

	private static final long serialVersionUID = 1L;
	private final P xSupport;
	private final P ySupport;

	public DefaultXYSupportPoint2D(int minX, int minY, int maxX, int maxY, P xSupport, P ySupport) {
		super(minX, minY, maxX, maxY);

		if(minX < 0) {
			throw new RuntimeException();
		}
		if(minY < 0) {
			throw new RuntimeException();
		}
		if(maxX < 0) {
			throw new RuntimeException();
		}
		if(maxY < 0) {
			throw new RuntimeException();
		}

		this.xSupport = xSupport;
		this.ySupport = ySupport;
	}

	@Override
	public boolean isYSupport(int y) {
		return ySupport.getAbsoluteY() <= y && y <= ySupport.getAbsoluteEndY();
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

	public int getYSupportMinY() {
		return ySupport.getAbsoluteY();
	}

	public int getYSupportMaxY() {
		return ySupport.getAbsoluteEndY();
	}

	@Override
	public String toString() {
		return "DefaultXYSupportPoint2D [" + minX + "x" + minY + " " + maxX + "x" + maxY
				+ ", xSupportMinX=" + getXSupportMinX() + ", xSupportMaxX=" + getXSupportMaxX()
				+ ", ySupportMinY=" + getYSupportMinY() + ", ySupportMaxY=" + getYSupportMaxY() + "]";
	}

	public SimplePoint2D<P> clone(int maxX, int maxY) {
		return new DefaultXYSupportPoint2D<P>(minX, minY, maxX, maxY, xSupport, ySupport);
	}

	@Override
	public int getSupportedMaxY() {
		return ySupport.getAbsoluteEndY();
	}
	
	@Override
	public int getSupportedMinY() {
		return ySupport.getAbsoluteY();
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
	public SimplePoint2D<P> moveY(int y) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D<P>(minX, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D<P>(minX, y, maxX, maxY);
	}

	@Override
	public SimplePoint2D<P> moveX(int x) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D<P>(x, minY, maxX, maxY, xSupport);
		}
		return new DefaultPoint2D<>(x, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D<P> moveX(int x, P ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D<P>(x, minY, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D<P>(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public SimplePoint2D<P> moveY(int y, P xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D<P>(minX, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D<P>(minX, y, maxX, maxY, xSupport);

	}

}
