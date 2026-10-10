package com.github.skjolber.packing.ep.points3d;

import java.util.Arrays;

/**
 * 
 * Custom list array for working with points.
 * 
 */

public class Point3DListArray {

	private static final int INITIAL_CAPACITY = 4;

	/**
	 * Shared, read-only placeholder for an index which has not been written to yet. Lists are
	 * created on first use of their index (see {@link #ensurePointAdditionalCapacity(int, int)}),
	 * so that reading an index (get, isEmpty) never needs a null check, and the placeholder's zero
	 * length array makes a stray add fail fast. Never grow or add to a list returned by
	 * {@link #get(int)} directly; always go through this class.
	 */
	private static final Point3DList EMPTY = new Point3DList(0);

	private Point3DList[] points = new Point3DList[16];

	public Point3DListArray() {
		Arrays.fill(points, EMPTY);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point3DList[] nextPoints = Arrays.copyOf(this.points, size);
			Arrays.fill(nextPoints, this.points.length, size, EMPTY);
			this.points = nextPoints;
		}
	}

	public void add(SimplePoint3D point, int index) {
		points[index].add(point);
	}

	public void reset() {
		for (int i = 0; i < this.points.length; i++) {
			Point3DList list = this.points[i];
			if(list != EMPTY) {
				list.clear();
			}
		}
	}

	public boolean isEmpty(int index) {
		return points[index].isEmpty();
	}

	public Point3DList get(int i) {
		return points[i];
	}

	public void ensurePointAdditionalCapacity(int index, int count) {
		Point3DList list = points[index];
		if(list == EMPTY) {
			points[index] = new Point3DList(Math.max(count, INITIAL_CAPACITY));
		} else {
			list.ensureAdditionalCapacity(count);
		}
	}

	public void ensurePointCapacity(int index, int count) {
		Point3DList list = points[index];
		if(list == EMPTY) {
			points[index] = new Point3DList(Math.max(count, INITIAL_CAPACITY));
		} else {
			list.ensureCapacity(count);
		}
	}

	public int getCapacity() {
		return points.length;
	}

}
