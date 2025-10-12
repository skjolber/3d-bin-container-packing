package com.github.skjolber.packing.ep.points3d;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;

/**
 * 
 * Custom list for working with points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point3DList {

	private int size = 0;
	private SimplePoint3D[] points;

	public Point3DList() {
		this(16);
	}

	public Point3DList(int initialSize) {
		points = new SimplePoint3D[initialSize];
	}

	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			SimplePoint3D[] nextPoints = new SimplePoint3D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);
			this.points = nextPoints;
		}
	}

	public void add(SimplePoint3D point) {
		points[size] = point;
		size++;
	}

	public int size() {
		return size;
	}

	public void reset() {
		Arrays.fill(this.points, 0, size, null);
		size = 0;
	}

	public SimplePoint3D get(int i) {
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
		if(obj instanceof Point3DList) {
			Point3DList other = (Point3DList)obj;
			if(other.size() == size) {
				for (int i = 0; i < size; i++) {
					if(!points[i].equals(other.get(i))) {
						return false;
					}
				}
			}
			return true;
		}
		return super.equals(obj);
	}

	public SimplePoint3D[] getPoints() {
		return points;
	}

	public void sort(Comparator<Point> comparator) {
		Arrays.sort(points, 0, size, comparator);
	}

}
