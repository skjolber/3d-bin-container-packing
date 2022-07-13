package com.github.skjolber.packing.ep.points3d;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;


/**
 * 
 * Custom list for working with points.
 * 
 */

public class Point3DArrayArray<P extends Placement3D> {


	private List<Point3D<P>>[] points = new List[16];
	
	public Point3DArrayArray() {
		 points = new List[16];
		 for(int i = 0; i < points.length;i++) {
			 points[i] = new ArrayList<>(6);
		 }
	}

	@SuppressWarnings("unchecked")
	public void ensureCapacity(int size) {
		if(points.length < size) {
			List<Point3D<P>>[] nextPoints = new List[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.points.length);
			for(int i = this.points.length; i < size; i++) {
				nextPoints[i] = new ArrayList<>(6);
			}
			this.points = nextPoints;
		}
	}
	
	public void add(Point3D<P> point, int index) {
		points[index].add(point);
	}

	public void reset() {
		for(int i = 0; i < this.points.length; i++) {
			this.points[i].clear();
		}
	}
	
	public boolean isEmpty(int index) {
		return points[index].isEmpty();
	}
	
	public List<Point3D<P>> get(int i) {
		return points[i];
	}

}
