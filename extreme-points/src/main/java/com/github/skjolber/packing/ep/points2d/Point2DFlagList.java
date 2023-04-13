package com.github.skjolber.packing.ep.points2d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;

/**
 * 
 * Custom list for working with to-be-removed points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point2DFlagList<P extends Placement2D & Serializable> {

	private int size = 0;
	private SimplePoint2D<P>[] points = new SimplePoint2D[16];
	private boolean[] flag = new boolean[16];

	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			SimplePoint2D<P>[] nextPoints = new SimplePoint2D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);

			boolean[] nextFlag = new boolean[size];
			System.arraycopy(this.flag, 0, nextFlag, 0, this.size);

			this.points = nextPoints;
			this.flag = nextFlag;
		}
	}

	public void add(SimplePoint2D<P> point) {
		points[size] = point;
		size++;
	}

	public void sort(Comparator<SimplePoint2D<?>> comparator) {
		Arrays.sort(points, 0, size, comparator);
	}

	public int size() {
		return size;
	}

	public SimplePoint2D<P> get(int i) {
		return points[i];
	}

	public void reset() {
		for (int i = 0; i < points.length; i++) {
			points[i] = null;
			flag[i] = false;
		}
		size = 0;
	}

	public void flag(int i) {
		flag[i] = true;
	}

	public boolean isEmpty() {
		return size == 0;
	}

	public boolean isFlag(int i) {
		return flag[i];
	}

	public void clear() {
		size = 0;
	}

	public void offset(int offset) {
		move(offset);
		for (int i = 0; i < offset; i++) {
			flag[i] = true;
		}
	}

	public void move(int offset) {
		ensureCapacity(size + offset);

		System.arraycopy(points, 0, points, offset, size);
		System.arraycopy(flag, 0, flag, offset, size);
		this.size += offset;
	}

	public void copyInto(Point2DFlagList<P> destination) {
		destination.ensureCapacity(size);

		System.arraycopy(points, 0, destination.points, 0, size);
		System.arraycopy(flag, 0, destination.flag, 0, size);
		destination.size = size;
	}

	public int removeFlagged() {
		int offset = 0;
		int index = 0;
		while (index < size) {
			if(flag[index]) {
				flag[index] = false;
			} else {
				points[offset] = points[index];
				offset++;
			}
			index++;
		}
		size = offset;

		return index - offset;
	}

	public List<SimplePoint2D<P>> toList() {
		List<SimplePoint2D<P>> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			list.add(points[i]);
		}
		return list;
	}

	public SimplePoint2D<P>[] getPoints() {
		return points;
	}

	/**
	 * Returns the hash code value for this list.
	 *
	 * <p>
	 * This implementation uses exactly the code that is used to define the
	 * list hash function in the documentation for the {@link List#hashCode}
	 * method.
	 *
	 * @return the hash code value for this list
	 */
	public int hashCode() {
		int hashCode = 1;
		for (int i = 0; i < size; i++) {
			if(!flag[i]) {
				hashCode = 31 * hashCode + points[i].hashCode() + Boolean.hashCode(flag[i]);
			}
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Point2DFlagList) {
			Point2DFlagList<P> other = (Point2DFlagList<P>)obj;
			if(other.size() == size) {
				for (int i = 0; i < size; i++) {
					if(!points[i].equals(other.get(i))) {
						return false;
					}
					if(flag[i] != other.flag[i]) {
						return false;
					}
				}
			}
			return true;
		}
		return super.equals(obj);
	}

	public void setAll(Point2DList<P, Point2D<P>> add, int offset) {
		System.arraycopy(add.getPoints(), 0, points, offset, add.size());
		int limit = offset + add.size();
		for (int i = offset; i < limit; i++) {
			flag[i] = false;
		}

	}

	public void sort(Comparator<Point2D<?>> comparator, int maxSize) {
		Arrays.sort(points, 0, maxSize, comparator);
	}

}
