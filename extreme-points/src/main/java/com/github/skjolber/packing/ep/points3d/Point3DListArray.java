package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;

/**
 * 
 * Custom list array for working with points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point3DListArray<P extends Placement3D> {

	private static final int INITIAL_CAPACITY = 8;

	private Point3DList<P>[] points = new Point3DList[16];

	public Point3DListArray() {
		points = new Point3DList[16];
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point3DList<>(INITIAL_CAPACITY);
		}
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point3DList<P>[] nextPoints = new Point3DList[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.points.length);
			for (int i = this.points.length; i < size; i++) {
				nextPoints[i] = new Point3DList<>(INITIAL_CAPACITY);
			}
			this.points = nextPoints;
		}
	}

	public void add(Point3D<P> point, int index) {
		points[index].add(point);
	}

	public void reset() {
		for (int i = 0; i < this.points.length; i++) {
			this.points[i].clear();
		}
	}

	public boolean isEmpty(int index) {
		return points[index].isEmpty();
	}

	public Point3DList<P> get(int i) {
		return points[i];
	}

	public void ensureAdditionalCapacity(int index, int count) {
		points[index].ensureAdditionalCapacity(count);
	}

}
