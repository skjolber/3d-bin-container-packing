package com.github.skjolberg.packing;

public class Dimension {

	public static final Dimension EMPTY = new Dimension(0, 0, 0);
	
	public static Dimension decode(String size) {
		String[] dimensions = size.split("x");
		
		return newInstance(Integer.parseInt(dimensions[0]), Integer.parseInt(dimensions[1]), Integer.parseInt(dimensions[2]));
	}

	public static String encode(Dimension dto) {
		return encode(dto.getWidth(), dto.getDepth(), dto.getHeight());
	}

	public static String encode(int width, int depth, int height) {
		return width + "x" + depth + "x" + height;
	}
	
	public static String encode(int width, int depth, int height, int grams) {
		return width + "x" + depth + "x" + height + "x" + grams;
	}

	public static Dimension newInstance(int width, int depth, int height) {
		return new Dimension(width, depth, height);
	}
	
	protected int width; // x
	protected int depth; // y
	protected int height; // z
	protected final int volume;
	
	protected final String name;
	
	public Dimension(String name, int w, int d, int h) {
		this.name = name;
		
		this.depth = d;
		this.width = w;
		this.height = h;

		this.volume = depth * width * height;
	}	
	
	public Dimension(int w, int d, int h) {
		this(null, w, d, h);
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDepth() {
		return depth;
	}
	
	/**
	 * 
	 * Check whether a dimension fits within the current dimensions, rotated in 3D.
	 * 
	 * @param dimension the space to fit
	 * @return true if any rotation of the argument can be placed inside this 
	 * 
	 */

	public boolean canHold3D(Dimension dimension) {
		return canHold3D(dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}
	
	public boolean canHold3D(int w, int d, int h) {
		
		if(w <= width && h <= height && d <= depth) {
			return true;
		}

		if(h <= width && d <= height && w <= depth) {
			return true;
		}

		if(d <= width && w <= height && h <= depth) {
			return true;
		}
		
		if(h <= width && w <= height && d <= depth) {
			return true;
		}

		if(d <= width && h <= height && w <= depth) {
			return true;
		}

		if(w <= width && d <= height && h <= depth) {
			return true;
		}		
		
		return false;
	}
	
	
	/**
	 * 
	 * Check whether a dimension fits within the current object, rotated in 2D.
	 * 
	 * @param dimension the dimension to fit
	 * @return true if any rotation of the argument can be placed inside this 
	 * 
	 */

	public boolean canHold2D(Dimension dimension) {
		return canHold2D(dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}
	
	public boolean canHold2D(int w, int d, int h) {
		if(h > height) {
			return false;
		}
		return (w <= width && d <= depth) || (d <= width && w <= depth);
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
	
	public int getFootprint() {
		return width * depth;
	}
	
	/**
	 * Check whether this object can fit within a dimension (without rotation).
	 * 
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space 
	 */
	
	public boolean fitsInside(Dimension dimension) {
		return fitsInside(dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}

	public boolean fitsInside(int w, int d, int h) {
		
		if(w >= width && h >= height && d >= depth) {
			return true;
		}
		
		return false;
	}

	/**
	 * Check whether this object can fit within a dimension, with 3D rotation.
	 * 
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space in any rotation
	 * 
	 */

	public boolean canFitInside3D(Dimension dimension) {
		return dimension.canHold3D(this);
	}

	/**
	 * Check whether this object can fit within a dimension, with 2D rotation.
	 * 
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space in any 2D rotation
	 * 
	 */

	public boolean canFitInside2D(Dimension dimension) {
		return dimension.canHold2D(this);
	}

	public int getVolume() {
		return volume;
	}
	
	public boolean isEmpty() {
		return width <= 0 || depth <= 0 || depth <= 0;
	}

	@Override
	public String toString() {
		return "Dimension [width=" + width + ", depth=" + depth + ", height=" + height + ", volume=" + volume + "]";
	}

	public String encode() {
		return encode(width, depth, height);
	}

	public String getName() {
		return name;
	}
	
	public String encode(int grams) {
		return encode(width, depth, height, grams);
	}

	
}