package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.StackPlacement;

public class DefaultYSupportPoint2D extends SimplePoint2D implements YSupportPoint2D {

	private static final long serialVersionUID = 1L;
	/** range constrained to current minX */
	private final StackPlacement ySupport;

	public DefaultYSupportPoint2D(int minX, int minY, int maxX, int maxY, StackPlacement ySupport) {
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

	public Point2D clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D(minX, minY, maxX, maxY, ySupport);
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
	public SimplePoint2D moveY(int y) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D(minX, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D(minX, y, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveY(int y, StackPlacement xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D(minX, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D(minX, y, maxX, maxY, xSupport);
	}

	@Override
	public SimplePoint2D moveX(int x, StackPlacement ySupport) {
		return new DefaultYSupportPoint2D(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public SimplePoint2D moveX(int x) {
		return new DefaultPoint2D(x, minY, maxX, maxY);
	}

}
