package com.github.skjolber.packing.ep.points3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.ep.Point3D;

/**
 * 
 * Custom list for working with to-be-removed points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point3DFlagList implements Serializable {

	private static final long serialVersionUID = 1L;

	private int size = 0;
	private SimplePoint3D[] points = new SimplePoint3D[16];
	private boolean[] flag = new boolean[16];

	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			SimplePoint3D[] nextPoints = new SimplePoint3D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);

			boolean[] nextFlag = new boolean[size];
			System.arraycopy(this.flag, 0, nextFlag, 0, this.size);

			this.points = nextPoints;
			this.flag = nextFlag;
		}
	}

	public void add(SimplePoint3D point) {
		points[size] = point;
		size++;
	}

	public void add(SimplePoint3D point, int index) {
		points[index] = point;
		flag[index] = false;
	}

	public void sort(Comparator<Point3D> comparator, int maxSize) {
		Arrays.sort(points, 0, maxSize, comparator);
	}

	public int size() {
		return size;
	}

	public SimplePoint3D get(int i) {
		return points[i];
	}

	public void reset() {
		Arrays.fill(this.points, 0, size, null);
		Arrays.fill(this.flag, 0, size, false);
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
		for (int i = 0; i < size; i++) {
			if(flag[i]) {
				flag[i] = false;
			}
		}
		size = 0;
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

	public List<Point3D> toList() {
		List<Point3D> list = new ArrayList<>(size);
		for (int i = 0; i < size; i++) {
			list.add(points[i]);
		}
		return list;
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

	public void copyFrom(Point3DFlagList source) {
		source.copyInto(this);
	}

	public void copyInto(Point3DFlagList destination) {
		destination.ensureCapacity(size);

		System.arraycopy(points, 0, destination.points, 0, size);
		System.arraycopy(flag, 0, destination.flag, 0, size);
		destination.size = size;

		for (int i = size; i < destination.flag.length; i++) {
			destination.flag[i] = false;
			destination.points[i] = null;
		}
	}

	public Point3D[] getPoints() {
		return points;
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
			hashCode = 31 * hashCode + points[i].hashCode() + Boolean.hashCode(flag[i]);
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Point3DFlagList) {
			Point3DFlagList other = (Point3DFlagList)obj;
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

	public void setAll(Point3DList add, int offset) {
		System.arraycopy(add.getPoints(), 0, points, offset, add.size());
		int limit = offset + add.size();
		for (int i = offset; i < limit; i++) {
			flag[i] = false;
		}

	}

	public void unflag(int i) {
		flag[i] = false;
	}

	public void set(SimplePoint3D point, int i) {
		points[i] = point;
	}

}
