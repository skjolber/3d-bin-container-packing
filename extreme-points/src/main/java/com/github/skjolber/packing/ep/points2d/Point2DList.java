package com.github.skjolber.packing.ep.points2d;

import java.util.List;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;


/**
 * 
 * Custom list for working with to-be-removed points. 
 * 
 */

public class Point2DList<P extends Placement2D> {

	private int size = 0;
	private Point2D<P>[] points = new Point2D[16];
	
	@SuppressWarnings("unchecked")
	public void addCapacity(int count) {
		ensureCapacity(size + count);
	}

	@SuppressWarnings("unchecked")
	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point2D[] nextPoints = new Point2D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);
			this.points = nextPoints;
		}
	}
	
	public void add(Point2D<P> point) {
		points[size] = point;
		size++;
	}
	
	public int size() {
		return size;
	}

	public void reset() {
		for(int i = 0; i < points.length; i++) {
			points[i] = null;
		}
		size = 0;
	}
	
	public Point2D<P> get(int i) {
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
     * <p>This implementation uses exactly the code that is used to define the
     * list hash function in the documentation for the {@link List#hashCode}
     * method.
     *
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
		for(int i = 0; i < size; i++) {
            hashCode = 31*hashCode + points[i].hashCode();
		}
        return hashCode;
    }
    
    @Override
    public boolean equals(Object obj) {
    	if(obj instanceof Point2DList) {
    		Point2DList other = (Point2DList)obj;
    		if(other.size() == size) {
    			for(int i = 0; i < size; i++) {
    	            if(!points[i].equals(other.get(i))) {
    	            	return false;
    	            }
    			}
    		}
    		return true;
    	}
    	return super.equals(obj);
    }    
	
}
