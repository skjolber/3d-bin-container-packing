package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.ExtremePoints;
import com.github.skjolber.packing.api.ep.Point2D;

public class GuillotineExtremePoints<P extends Placement2D> implements ExtremePoints<P, Point2D<P>> {

	protected int containerMaxX;
	protected int containerMaxY;
	
	protected final Point2DFlagList<P> values = new Point2DFlagList<>();
	protected final List<P> placements = new ArrayList<>();

	protected P containerPlacement;

	private long minArea = 0;

	public GuillotineExtremePoints(int dx, int dy) {
		setSize(dx, dy);
		addFirstPoint();
	}
	
	private void setSize(int dx, int dy) {
		this.containerMaxX = dx - 1;
		this.containerMaxY = dy - 1;

		this.containerPlacement = (P) new DefaultPlacement2D(0, 0, containerMaxX, containerMaxY);
	}

	private void addFirstPoint() {
		values.add(new DefaultXYSupportPoint2D<P>(0, 0, containerMaxX, containerMaxY, containerPlacement, containerPlacement));
	}
	
	public boolean add(int index, P placement) {
		return false;
	}
	
	public void setMinArea(long minArea) {
		this.minArea = minArea;
	}
	
	@Override
	public String toString() {
		return "ExtremePoints2D [width=" + containerMaxX + ", depth=" + containerMaxY + ", values=" + values + "]";
	}
	
	public List<P> getPlacements() {
		return placements;
	}

	public Point2D<P> getValue(int i) {
		return values.get(i);
	}
	
	public List<Point2D<P>> getValues() {
		return values.toList();
	}
	
	@Override
	public int getValueCount() {
		return values.size();
	}

	
	public int getMinY() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			
			if(point.getMinY() < values.get(min).getMinY()) {
				min = i;
			}
		}
		return min;
	}

	public int getMinX() {
		int min = 0;
		for (int i = 1; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			
			if(point.getMinX() < values.get(min).getMinX()) {
				min = i;
			}
		}
		return min;
	}

	public boolean isEmpty() {
		return values.isEmpty();
	}	

	public long getMaxArea() {
		long maxPointArea = -1L;
		for(int i = 0; i < values.size(); i++) {
			Point2D<P> point = values.get(i);
			if(maxPointArea < point.getArea()) {
				maxPointArea = point.getArea(); 
			}
		}
		return maxPointArea;
	}

	public void redo() {
		values.clear();
		placements.clear();
		
		addFirstPoint();
	}

	@Override
	public void reset(int dx, int dy, int dz) {
		setSize(dx, dy);
		
		redo();
	}
	
	public int findPoint(int x, int y) {
		for(int i = 0; i < values.size(); i++) {
			Point2D<P> point2d = values.get(i);
			if(point2d.getMinX() == x && point2d.getMinY() == y) {
				return i;
			}
		}
		return -1;
	}
		
}
