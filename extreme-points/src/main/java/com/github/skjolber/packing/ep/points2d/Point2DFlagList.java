package com.github.skjolber.packing.ep.points2d;

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

public class Point2DFlagList<P extends Placement2D> {

	private int size = 0;
	private Point2D<P>[] points = new Point2D[16];
	private boolean[] flag = new boolean[16];
	
	@SuppressWarnings("unchecked")
	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	@SuppressWarnings("unchecked")
	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point2D[] nextPoints = new Point2D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);

			boolean[] nextFlag = new boolean[size];
			System.arraycopy(this.flag, 0, nextFlag, 0, this.size);

			this.points = nextPoints;
			this.flag = nextFlag;
		}
	}
	
	public void add(Point2D<P> point) {
		points[size] = point;
		size++;
	}
	
	public void sort(Comparator<Point2D<?>> comparator) {
		Arrays.sort(points, 0, size, comparator);
	}
	
	public int size() {
		return size;
	}
	
	public Point2D<P> get(int i) {
		return points[i];
	}
	
	public void reset() {
		for(int i = 0; i < points.length; i++) {
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
	
	public void compress() {
		int offset = 0;
		int index = 0;
		while(index < size) {
			if(flag[index]) {
				flag[index] = false;
			} else {
				points[offset] = points[index];
				offset++;
			}
			index++;
		}
		size = offset;
	}

	public List<Point2D<P>> toList() {
		List<Point2D<P>> list = new ArrayList<>();
		for(int i = 0; i < size; i++) {
			list.add(points[i]);
		}
		return list;
	}

	public Point2D<P>[] getPoints() {
		return points;
	}
	
	
    /**
     * Returns the hash code value for this list.
     *
     * <p>This implementation uses exactly the code that is used to define the
     * list hash function in the documentation for the {@link List#hashCode}
     * method.
     *
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
		for(int i = 0; i < size; i++) {
			if(!flag[i]) {
				hashCode = 31*hashCode + points[i].hashCode() + Boolean.hashCode(flag[i]);
			}
		}
        return hashCode;
    }

    
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof Point2DFlagList) {
    		Point2DFlagList other = (Point2DFlagList)obj;
    		if(other.size() == size) {
    			for(int i = 0; i < size; i++) {
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
}
