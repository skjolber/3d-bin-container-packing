package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultXSupportPoint2D extends Point2D implements XSupportPoint2D {

	/** range constrained to current minY */
	private final Placement2D xSupport;
	
	public DefaultXSupportPoint2D(int minX, int minY, int maxX, int maxY, Placement2D xSupport) {
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
	public boolean isXEdge(int x) {
		return xSupport.getAbsoluteEndX() == x - 1;
	}

	@Override
	public String toString() {
		return "DefaultXSupportPoint2D [" +  + minX + "x" + minY + " " + maxX + "x" + maxY 
				+ ", xSupportMinX=" + getXSupportMinX() + ", xSupportMaxX=" + getXSupportMaxX() + "]";
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultXSupportPoint2D(minX, minY, maxX, maxY, xSupport);
	}

	@Override
	public Placement2D getXSupport() {
		return xSupport;
	}
	
	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(xSupport);
		return list;
	}

	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D(x, y, maxX, maxY, xSupport);
		}
		return new DefaultPoint2D(x, y, maxX, maxY);
	}

	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY, Placement2D ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D(x, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D(x, y, maxX, maxY, ySupport);
	}
	
	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY) {
		return new DefaultPoint2D(x, y, maxX, maxY);
	}

	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY, Placement2D xSupport) {
		return new DefaultXSupportPoint2D(x, y, maxX, maxY, xSupport);
	}	
}
