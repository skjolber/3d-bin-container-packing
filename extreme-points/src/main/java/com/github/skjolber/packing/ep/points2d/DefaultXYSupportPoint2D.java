package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;

import com.github.skjolber.packing.api.StackPlacement;

public class DefaultXYSupportPoint2D extends SimplePoint2D implements XSupportPoint2D, YSupportPoint2D {

	private static final long serialVersionUID = 1L;
	private final StackPlacement xSupport;
	private final StackPlacement ySupport;

	public DefaultXYSupportPoint2D(int minX, int minY, int maxX, int maxY, StackPlacement xSupport, StackPlacement ySupport) {
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

	public SimplePoint2D clone(int maxX, int maxY) {
		return new DefaultXYSupportPoint2D(minX, minY, maxX, maxY, xSupport, ySupport);
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
	public SimplePoint2D moveY(int y) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D(minX, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D(minX, y, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveX(int x) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D(x, minY, maxX, maxY, xSupport);
		}
		return new DefaultPoint2D(x, minY, maxX, maxY);
	}

	@Override
	public SimplePoint2D moveX(int x, StackPlacement ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D(x, minY, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public SimplePoint2D moveY(int y, StackPlacement xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D(minX, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D(minX, y, maxX, maxY, xSupport);

	}

}
