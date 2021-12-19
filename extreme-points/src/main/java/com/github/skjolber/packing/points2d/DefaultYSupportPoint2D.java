package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;

public class DefaultYSupportPoint2D extends Point2D implements YSupportPoint2D  {

	/** range constrained to current minX */
	private final Placement2D ySupport;
	
	public DefaultYSupportPoint2D(int minX, int minY, int maxX, int maxY, Placement2D ySupport) {
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
	
	@Override
	public boolean isYEdge(int y) {
		return ySupport.getAbsoluteEndY() == y - 1;
	}
	
	public Point2D clone(int maxX, int maxY) {
		return new DefaultYSupportPoint2D(minX, minY, maxX, maxY, ySupport);
	}

	@Override
	public Placement2D getYSupport() {
		return ySupport;
	}

	@Override
	public List<Placement2D> getPlacements2D() {
		List<Placement2D> list = new ArrayList<>();
		list.add(ySupport);
		return list;
	}
	
	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D(x, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D(x, y, maxY, maxY);
	}

	@Override
	public Point2D moveY(int x, int y, int maxX, int maxY, Placement2D xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D(x, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D(x, y, maxX, maxY, xSupport);
	}	
	
	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY, Placement2D ySupport) {
		return new DefaultYSupportPoint2D(x, y, maxX, maxY, ySupport);
	}

	@Override
	public Point2D moveX(int x, int y, int maxX, int maxY) {
		return new DefaultPoint2D(x, y, maxY, maxY);
	}



}
