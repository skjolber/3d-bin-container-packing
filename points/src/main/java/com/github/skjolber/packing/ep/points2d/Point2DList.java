package com.github.skjolber.packing.ep.points2d;

import java.util.Arrays;

import java.util.List;

/**
 * 
 * Custom list for working with points.
 * 
 */

public class Point2DList {

	private int size = 0;
	private SimplePoint2D[] points;

	public Point2DList() {
		this(16);
	}

	public Point2DList(int initialSize) {
		points = new SimplePoint2D[initialSize];
	}

	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			SimplePoint2D[] nextPoints = new SimplePoint2D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);
			this.points = nextPoints;
		}
	}

	public void add(SimplePoint2D point) {
		points[size] = point;
		size++;
	}

	public int size() {
		return size;
	}

	public void reset() {
		Arrays.fill(points, 0, size, null);
		size = 0;
	}

	public SimplePoint2D get(int i) {
		return points[i];
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public void clear() {
		size = 0;
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * 
	 * This implementation uses exactly the code that is used to define the
	 * list hash function in the documentation for the {@link List#hashCode}
	 * method.
	 *
	 * @return the hash code value for this list
	 */
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < size; i++) {
			hashCode = 31 * hashCode + points[i].hashCode();
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Point2DList) {
			Point2DList other = (Point2DList)obj;
			if(other.size() != size) {
				return false;
			}
			for (int i = 0; i < size; i++) {
				if(!points[i].equals(other.get(i))) {
					return false;
				}
			}
			return true;
		}
		return super.equals(obj);
	}

	public SimplePoint2D[] getPoints() {
		return points;
	}

	public int getCapacity() {
		return points.length;
	}

}
