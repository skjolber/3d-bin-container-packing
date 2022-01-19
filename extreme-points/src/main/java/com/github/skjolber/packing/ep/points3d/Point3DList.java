package com.github.skjolber.packing.ep.points3d;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;


/**
 * 
 * Custom list for working with points.
 * 
 */

public class Point3DList<P extends Placement3D> {

	private int size = 0;
	private Point3D<P>[] points = new Point3D[16];
	
	public void ensureAdditionalCapacity(int count) {
		ensureCapacity(size + count);
	}

	@SuppressWarnings("unchecked")
	public void ensureCapacity(int size) {
		if(points.length < size) {
			Point3D[] nextPoints = new Point3D[size];
			System.arraycopy(this.points, 0, nextPoints, 0, this.size);
			this.points = nextPoints;
		}
	}
	
	public void add(Point3D<P> point) {
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
	
	public Point3D<P> get(int i) {
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
    	if(obj instanceof Point3DList) {
    		Point3DList other = (Point3DList)obj;
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
    
    public Point3D<P>[] getPoints() {
		return points;
	}

	public void sort(Comparator<Point3D<?>> comparator) {
		Arrays.sort(points, 0, size, comparator);
	}

}
