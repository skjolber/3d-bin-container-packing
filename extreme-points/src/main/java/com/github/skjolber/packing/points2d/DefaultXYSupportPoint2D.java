package com.github.skjolber.packing.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Point2D;
import com.github.skjolber.packing.api.XSupportPoint2D;
import com.github.skjolber.packing.api.YSupportPoint2D;

public class DefaultXYSupportPoint2D<P extends Placement2D> extends Point2D<P> implements XSupportPoint2D, YSupportPoint2D {

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

	public Point2D<P> clone(int maxX, int maxY) {
		return new DefaultXYSupportPoint2D<P>(minX, minY, maxX, maxY, xSupport, ySupport);
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
	public List<P> getPlacements2D() {
		List<P> list = new ArrayList<>();
		list.add(xSupport);
		list.add(ySupport);
		return list;
	}
	
	@Override
	public Point2D<P> moveY(int y, int maxX, int maxY) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultYSupportPoint2D<P>(minX, y, maxX, maxY, ySupport);
		}
		return new DefaultPoint2D<P>(minX, y, maxX, maxY);
	}
	
	@Override
	public Point2D<P> moveX(int x, int maxX, int maxY) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXSupportPoint2D<P>(x, minY, maxX, maxY, xSupport);
		}
		return new DefaultPoint2D<>(x, minY, maxX, maxY);
	}

	@Override
	public Point2D<P> moveX(int x, int maxX, int maxY, P ySupport) {
		if(x <= xSupport.getAbsoluteEndX()) {
			return new DefaultXYSupportPoint2D<P>(x, minY, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultYSupportPoint2D<P>(x, minY, maxX, maxY, ySupport);
	}

	@Override
	public Point2D<P> moveY(int y, int maxX, int maxY, P xSupport) {
		if(y <= ySupport.getAbsoluteEndY()) {
			return new DefaultXYSupportPoint2D<P>(minX, y, maxX, maxY, xSupport, ySupport);
		}
		return new DefaultXSupportPoint2D<P>(minX, y, maxX, maxY, xSupport);

	}	
	
}
