package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultXYSupportPoint2D extends Point2D implements XSupportPoint2D, YSupportPoint2D {

	private final Placement2D xSupport;
	private final Placement2D ySupport;
		
	public DefaultXYSupportPoint2D(int minX, int minY, int maxX, int maxY, Placement2D xSupport, Placement2D ySupport) {
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
	public boolean isYEdge(int y) {
		return ySupport.getAbsoluteEndY() == y - 1;
	}

	@Override
	public boolean isXEdge(int x) {
		return xSupport.getAbsoluteEndX() == x - 1;
	}

	@Override
	public String toString() {
		return "DefaultXYSupportPoint2D [" + minX + "x" + minY + " " + maxX + "x" + maxY 
				+ ", xSupportMinX=" + getXSupportMinX() + ", xSupportMaxX=" + getXSupportMaxX()
				+ ", ySupportMinY=" + getYSupportMinY() + ", ySupportMaxY=" + getYSupportMaxY() + "]";
	}

	public Point2D clone(int maxX, int maxY) {
		return new DefaultXYSupportPoint2D(minX, minY, maxX, maxY, xSupport, ySupport);
	}
	
	@Override
	public Placement2D getYSupport() {
		return ySupport;
	}

	@Override
	public Placement2D getXSupport() {
		return xSupport;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(xSupport);
		list.add(ySupport);
		return list;
	}

}
