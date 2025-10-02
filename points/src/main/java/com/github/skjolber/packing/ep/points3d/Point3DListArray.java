package com.github.skjolber.packing.ep.points3d;

/**
 * 
 * Custom list array for working with points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point3DListArray {

	private static final int INITIAL_CAPACITY = 8;

	private Point3DList[] points = new Point3DList[16];

	public Point3DListArray() {
		points = new Point3DList[16];
		for (int i = 0; i < points.length; i++) {
			points[i] = new Point3DList(INITIAL_CAPACITY);
		}
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point3DList[] nextPoints = new Point3DList[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.points.length);
			for (int i = this.points.length; i < size; i++) {
				nextPoints[i] = new Point3DList(INITIAL_CAPACITY);
			}
			this.points = nextPoints;
		}
	}

	public void add(SimplePoint3D point, int index) {
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

	public Point3DList get(int i) {
		return points[i];
	}

	public void ensureAdditionalCapacity(int index, int count) {
		points[index].ensureAdditionalCapacity(count);
	}

}
