package com.github.skjolber.packing.ep.points2d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.github.skjolber.packing.api.point.Point;

/**
 * 
 * Custom list for working with to-be-removed points.
 * 
 */

@SuppressWarnings("unchecked")
public class Point2DFlagList implements Iterable<Point> {

	private static class PointIterator implements Iterator<Point> {

		private int size;
		private SimplePoint2D[] points;
		private int index = 0;

		public void set(int size, SimplePoint2D[] points) {
			this.size = size;
			this.points = points;
			this.index = 0;
		}

		@Override
		public boolean hasNext() {
			return index < size;
		}

		@Override
		public Point next() {
			SimplePoint2D p = points[index];
			index++;
			return p;
		}
		
	}

	
	private int size = 0;
	private SimplePoint2D[] points = new SimplePoint2D[16];
	private boolean[] flag = new boolean[16];

	public Point2DFlagList() {
		this(16);
	}

	public Point2DFlagList(int capacity) {
		points = new SimplePoint2D[capacity];
		flag = new boolean[capacity];
	}
	
	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	public void ensureCapacity(int size) {
		if(points.length < size) {
			SimplePoint2D[] nextPoints = new SimplePoint2D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);

			boolean[] nextFlag = new boolean[size];
			System.arraycopy(this.flag, 0, nextFlag, 0, this.size);

			this.points = nextPoints;
			this.flag = nextFlag;
		}
	}

	public void add(SimplePoint2D point) {
		points[size] = point;
		size++;
	}

	public void sort(Comparator<SimplePoint2D> comparator) {
		Arrays.sort(points, 0, size, comparator);
	}

	public int size() {
		return size;
	}

	public SimplePoint2D get(int i) {
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

	public void copyInto(Point2DFlagList destination) {
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

	public List<Point> toList() {
		List<Point> list = new ArrayList<>();
		for (int i = 0; i < size; i++) {
			list.add(points[i]);
		}
		return list;
	}

	public SimplePoint2D[] getPoints() {
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
			if(!flag[i]) {
				hashCode = 31 * hashCode + points[i].hashCode() + Boolean.hashCode(flag[i]);
			}
		}
		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Point2DFlagList) {
			Point2DFlagList other = (Point2DFlagList)obj;
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

	public void setAll(Point2DList add, int offset) {
		System.arraycopy(add.getPoints(), 0, points, offset, add.size());
		int limit = offset + add.size();
		for (int i = offset; i < limit; i++) {
			flag[i] = false;
		}

	}

	public void sort(Comparator<Point2D> comparator, int maxSize) {
		Arrays.sort(points, 0, maxSize, comparator);
	}
	
	@Override
	public Iterator<Point> iterator() {
		PointIterator iterator = new PointIterator();
		iterator.set(size, points);
		return iterator;
	}

	public Point2DFlagList clone(boolean clonePoints) {
		Point2DFlagList clone = new Point2DFlagList(points.length);

		clone.size = size;

		System.arraycopy(flag, 0, clone.flag, 0, flag.length);
		if(clonePoints) {
			for(int i = 0; i < size; i++) {
				clone.points[i] = points[i].clone();
			}
		} else {
			System.arraycopy(points, 0, clone.points, 0, points.length);
		}
		
		return clone;
	}


	public int binarySearchPlusMinX(int minIndex, int key) {
		return binarySearchPlusMinX(points, minIndex, size, key);
	}
	
	public static int binarySearchPlusMinX(SimplePoint2D[] points, int low, int size, int key) {
		// return exclusive result

		int high = size - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			int midVal = points[mid].getMinX();

			if(midVal < key) {
				low = mid + 1;
			} else if(midVal != key) {
				high = mid - 1;
			} else {
				// key found
				do {
					mid++;
				} while (mid < size && points[mid].getMinX() == key);

				return mid;
			}
		}
		// key not found
		return low;
	}

	public int getIndex(Point point, int minIndex) {
		// return inclusive result
		
		int key = point.getMinX();

		int high = size - 1;

		while (minIndex <= high) {
			int mid = (minIndex + high) >>> 1;

			int midVal = points[mid].getMinX();

			if(midVal < key) {
				minIndex = mid + 1;
			} else if(midVal != key) {
				high = mid - 1;
			} else {
				// key found
				SimplePoint2D simplePoint3D = points[mid];
				if(simplePoint3D == point) {
					return mid;
				}
				
				int compare = SimplePoint2D.COMPARATOR_X_THEN_Y_THEN_Z.compare(point, simplePoint3D);
				if(compare <= 0) {
					// check below
					do {
						mid--;
						if(mid < 0) {
							throw new IllegalStateException("Cannot locate point " + point);
						}
						if(points[mid] == point) {
							return mid;
						}
					} while(true);
				}  
					
				if(compare >= 0) {
					// check above
					do {
						mid++;
						if(mid == size) {
							throw new IllegalStateException("Cannot locate point " + point);
						}
						if(points[mid] == point) {
							return mid;
						}
					} while(true);
				}
				
				throw new IllegalStateException("Cannot locate point " + point);
			}
		}
		// key not found
		throw new IllegalStateException("Cannot locate point " + point);
	}

	public int binarySearchMinusMinX(int minIndex, int key) {
		return binarySearchMinusMinX(points, minIndex, size, key);
	}
	
	public static int binarySearchMinusMinX(SimplePoint2D[] points, int low, int size, int key) {
		// return inclusive result
		
		int high = size - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			int midVal = points[mid].getMinX();

			if(midVal < key) {
				low = mid + 1;
			} else if(midVal != key) {
				high = mid - 1;
			} else {
				// key found
				while (mid > 0 && points[mid - 1].getMinX() == key) {
					mid--;
				}

				return mid;
			}
		}
		// key not found
		return low;
	}

	public int binarySearchPlusMinY(int minIndex, int key) {
		return binarySearchPlusMinY(points, minIndex, size, key);
	}
	
	public static int binarySearchPlusMinY(SimplePoint2D[] points, int low, int size, int key) {
		// return exclusive result

		int high = size - 1;

		while (low <= high) {
			int mid = (low + high) >>> 1;

			int midVal = points[mid].getMinY();

			if(midVal < key) {
				low = mid + 1;
			} else if(midVal != key) {
				high = mid - 1;
			} else {
				// key found
				do {
					mid++;
				} while (mid < size && points[mid].getMinY() == key);

				return mid;
			}
		}
		// key not found
		return low;
	}
}
