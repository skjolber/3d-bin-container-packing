package com.github.skjolber.packing;

public class Box extends Dimension {
	final int weight;

	public Box(int w, int d, int h, int weight) {
		super(w, d, h);

		this.weight = weight;
	}

	public Box(String name, int w, int d, int h, int weight) {
		super(name, w, d, h);

		this.weight = weight;
	}

	public Box(final Dimension dimension, final Integer weight) {
		this(dimension.width, dimension.depth, dimension.height, weight);
	}

	/**
	 * Rotate box, i.e. in 3D
	 *
	 * @return this instance
	 */
	public Box rotate3D() {
		int height = this.height;

		this.height = width;
		this.width = depth;
		this.depth = height;

		return this;
	}

	/**
	 * Rotate box to largest footprint (downwards area) within a free space
	 *
	 * @param dimension space to fit within
	 * @return if this object fits within the input dimensions
	 */
	boolean rotateLargestFootprint3D(Dimension dimension) {
		return rotateLargestFootprint3D(dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}

	private boolean fitsWidthAndDepthDown(int w, int d, int h) {

		if (h < height) {
			return false;
		}

		return (d >= width && w >= depth) || (w >= width && d >= depth);
	}

	private boolean fitsHeightAndDepthDown(int w, int d, int h) {

		if (h < width) {
			return false;
		}

		return (d >= height && w >= depth) || (w >= height && d >= depth);
	}

	private boolean fitsHeightAndWidthDown(int w, int d, int h) {

		if (h < depth) {
			return false;
		}

		return (d >= height && w >= width) || (w >= height && d >= width);
	}

	private boolean rotateLargestFootprint3D(int w, int d, int h) {
		int a = Integer.MIN_VALUE;
		if (fitsWidthAndDepthDown(w, d, h)) {
			a = width * depth;
		}

		int b = Integer.MIN_VALUE;
		if (fitsHeightAndDepthDown(w, d, h)) {
			b = height * depth;
		}

		int c = Integer.MIN_VALUE;
		if (fitsHeightAndWidthDown(w, d, h)) {
			c = width * height;
		}

		if (a == Integer.MIN_VALUE && b == Integer.MIN_VALUE && c == Integer.MIN_VALUE) {
			return false;
		}

		if (a > b && a > c) {
			// no rotate
		} else if (b > c) {
			// rotate once
			rotate3D();
		} else {
			rotate3D();
			rotate3D();
		}

		if (h < height) {
			throw new IllegalArgumentException("Expected height " + height + " to fit within height constraint " + h);
		}

		if (width > w || depth > d) {
			// use the other orientation
			rotate2D();
		}

		if (width > w || depth > d) {
			throw new IllegalArgumentException("Expected width " + width + " and depth " + depth + " to fit within constraint width " + w + " and depth " + d);
		}

		return true;

	}

	boolean fitRotate2D(int w, int d) {

		if (w >= width && d >= depth) {
			return true;
		}

		if (d >= width && w >= depth) {
			rotate2D();

			return true;
		}

		return false;
	}


	/**
	 * Rotate box to smallest footprint (downwards area - width*depth) within a free space.
	 *
	 * @param space free space
	 * @return false if box does not fit
	 */

	boolean fitRotate3DSmallestFootprint(Dimension space) {
		return fitRotate3DSmallestFootprint(space.getWidth(), space.getDepth(), space.getHeight());
	}

	boolean fitRotate3DSmallestFootprint(int w, int d, int h) {
		int a = Integer.MAX_VALUE;
		if (fitsWidthAndDepthDown(w, d, h)) {
			a = width * depth;
		}

		int b = Integer.MAX_VALUE;
		if (fitsHeightAndDepthDown(w, d, h)) {
			b = height * depth;
		}

		int c = Integer.MAX_VALUE;
		if (fitsHeightAndWidthDown(w, d, h)) {
			c = width * height;
		}

		if (a == Integer.MAX_VALUE && b == Integer.MAX_VALUE && c == Integer.MAX_VALUE) {
			return false;
		}

		if (a < b && a < c) {
			// no rotate
		} else if (b < c) {
			// rotate once
			rotate3D();
		} else {
			rotate3D();
			rotate3D();
		}

		if (h < height) {
			throw new IllegalArgumentException("Expected height " + height + " to fit within height constraint " + h);
		}

		if (width > w || depth > d) {
			// use the other orientation
			rotate2D();
		}

		if (width > w || depth > d) {
			throw new IllegalArgumentException("Expected width " + width + " and depth " + depth + " to fit within constraint width " + w + " and depth " + d);
		}

		return true;
	}

	/**
	 * Rotate box within a free space in 2D
	 *
	 * @param dimension space to fit within
	 * @return if this object fits within the input dimensions
	 */

	boolean fitRotate2D(Dimension dimension) {
		if (dimension.getHeight() < height) {
			return false;
		}
		return fitRotate2D(dimension.getWidth(), dimension.getDepth());
	}

	int currentSurfaceArea() {
		return width * depth;
	}

	public Box clone() {
		return new Box(name, width, depth, height, weight);
	}

	/**
	 * Rotate box, i.e. in 2 dimensions, keeping the height constant.
	 *
	 * @return this
	 */

	public Box rotate2D() {
		int depth = this.depth;

		this.depth = width;
		this.width = depth;

		return this;
	}

	public Box rotate2D3D() {
		//rotate2D();
		// width -> depth
		// depth -> width

		//rotate3D();
		// height = width;
		// width = depth;
		// depth = height;

		// so
		// height -> width -> depth;
		// width -> depth -> width;
		// depth -> height;

		int depth = this.depth;

		this.depth = height;
		this.height = depth;

		return this;
	}

	public int getWeight() {
		return weight;
	}

	@Override
	public String toString() {
		return "Box [width=" + width + ", depth=" + depth + ", height=" + height + ", volume="
				+ volume + ", name=" + name + ", weight=" + weight + "]";
	}

}
