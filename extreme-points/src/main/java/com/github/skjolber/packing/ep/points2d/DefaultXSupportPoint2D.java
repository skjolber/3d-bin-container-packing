package com.github.skjolber.packing.ep.points2d;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;

public class DefaultXSupportPoint2D extends SimplePoint2D implements XSupportPoint2D {

	private static final long serialVersionUID = 1L;
	/** range constrained to current minY */
	private final StackPlacement xSupport;

	public DefaultXSupportPoint2D(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, StackPlacement xSupport) {
		super(minX, minY, minZ, maxX, maxY, maxZ);
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

	public Point2D clone(int maxX, int maxY) {
		return new DefaultXSupportPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport);
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
	public SimplePoint2D moveX(int x) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D(x, minY, minZ, maxX, maxY, maxZ, xSupport);
		}
		return new DefaultPoint2D(x, minY, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveX(int x, StackPlacement ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D(x, minY, minZ, maxX, maxY, maxZ, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D(x, minY, minZ, maxX, maxY, maxZ, ySupport);
	}

	@Override
	public SimplePoint2D moveY(int y) {
		return new DefaultPoint2D(minX, y, minZ, maxX, maxY, maxZ);
	}

	@Override
	public SimplePoint2D moveY(int y, StackPlacement xSupport) {
		return new DefaultXSupportPoint2D(minX, y, minZ, maxX, maxY, maxZ, xSupport);
	}

	@Override
	public Point clone(int maxX, int maxY, int maxZ) {
		return new DefaultXSupportPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport);
	}

	@Override
	public SimplePoint2D clone() {
		return new DefaultXSupportPoint2D(minX, minY, minZ, maxX, maxY, maxZ, xSupport);
	}

}
