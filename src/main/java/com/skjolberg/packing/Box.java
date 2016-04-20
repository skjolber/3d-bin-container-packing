package com.skjolberg.packing;

public class Box extends Dimension {

	public Box(int w, int d, int h) {
		super(w, d, h);
	}

	public Box(String name, int w, int d, int h) {
		super(name, w, d, h);
	}
	
	/**
	 * 
	 * Rotate box, i.e. in 3 dimensions
	 * 
	 * @return
	 */

	public Box rotate3D() {
		int depth = this.depth;
		
		this.depth = height;
		this.height = width;
		this.width = depth;

		return this;
	}

	@Override
	public String toString() {
		return "Box [name=" + name + ", width=" + width + ", depth=" + depth + ", height=" + height + ", volume="
				+ volume + "]";
	}

	/**
	 * 
	 * Rotate box to largest footprint (downwards area) within a free space
	 * 
	 */

	public boolean rotateLargestFootprint(Dimension space) {
		return rotateLargestFootprint(space.getWidth(), space.getDepth(), space.getHeight());
	}

	public boolean rotateLargestFootprint(int w, int d, int h) {
		int a = -1;
		if(w >= width && h >= height && d >= depth) {
			a = width * depth;
		}

		int b = -1;
		if(h >= width && d >= height && w >= depth) {
			b = height * depth;
		}

		int c = -1;
		if(d >= width && w >= height && h >= depth) {
			c = width * height;
		}
		
		if(a == -1 && b == -1 && c == -1) {
			return false;
		}
		
		if(a > b && a > c) {
			// no rotate
		} else if(b > c) {
			// rotate once
			rotate3D();
		} else {
			rotate3D();
			rotate3D();
		}

		return true;
		
	}

	public boolean fitInFootprintRotateMinimumWidth(int w, int d) {

		if(w >= width && d >= depth && d >= width && w >= depth) {
			
			// fits in any rotation
			if(width > depth) { // use depth as width
				rotate2D();
			}
			
			return true;
		} else {
			return fitInFootprintRotate(w, d);
		}
	}

	public boolean fitInFootprintRotateMinimumDepth(int w, int d) {

		if(w >= width && d >= depth && d >= width && w >= depth) {
			
			// fits in any rotation
			if(width < depth) { // use depth as width
				rotate2D();
			}
			
			return true;
		} else {
			return fitInFootprintRotate(w, d);
		}
	}

	public boolean fitInFootprintRotate(int w, int d) {
		
		if(w >= width && d >= depth) {
			return true;
		}
		
		if(d >= width && w >= depth) {
			rotate2D();
			
			return true;
		}

		return false;
		
		
	}
	
	/**
	 * 
	 * Rotate box to smallest footprint (downwards area) within a free space.
	 * 
	 * @param space free space
	 * @return false if box does not fit
	 * 
	 */
	
	public boolean rotateSmallestFootprint(Dimension space) {
		return rotateSmallestFootprint(space.getWidth(), space.getDepth(), space.getHeight());
	}
	
	public boolean rotateSmallestFootprint(int w, int d, int h) {
		int a = Integer.MAX_VALUE;
		if(w >= width && h >= height && d >= depth) {
			a = width * depth;
		}

		int b = Integer.MAX_VALUE;
		if(h >= width && d >= height && w >= depth) {
			b = height * depth;
		}

		int c = Integer.MAX_VALUE;
		if(d >= width && w >= height && h >= depth) {
			c = width * height;
		}
		
		if(a == Integer.MAX_VALUE && b == Integer.MAX_VALUE && c == Integer.MAX_VALUE) {
			return false;
		}
		
		if(a < b && a < c) {
			// no rotate
		} else if(b < c) {
			// rotate once
			rotate3D();
		} else {
			rotate3D();
			rotate3D();
		}
		
		return true;
	}
	
	/**
	 * 
	 * Rotate box, i.e. in 2 dimensions, keeping the height constant.
	 * 
	 */
	
	public void rotate2D() {
		int depth = this.depth;
		
		this.depth = width;
		this.width = depth;
	}
	
	public boolean fitInFootprintRotateMinimumDepth(Dimension box) {
		return fitInFootprintRotateMinimumDepth(box.getWidth(), box.getDepth());
	}

	public boolean fitInFootprintRotateMinimumWidth(Dimension box) {
		return fitInFootprintRotateMinimumWidth(box.getWidth(), box.getDepth());
	}

	public boolean fitInFootprintRotate(Dimension box) {
		return fitInFootprintRotate(box.getWidth(), box.getDepth());
	}
	
}