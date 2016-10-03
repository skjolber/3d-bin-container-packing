package com.skjolberg.packing;

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
	
	protected int width;
	protected int height;
	protected int depth; 
	protected final int volume;
	
	protected final String name;
	
	protected final Dimension parent;

	public Dimension(Dimension parent, String name, int w, int d, int h) {
		this.parent = parent;
		this.name = name;
		
		this.depth = d;
		this.width = w;
		this.height = h;

		this.volume = depth * width * height;
	}	
	
	public Dimension(Dimension parent, int w, int d, int h) {
		this(parent, null, w, d, h);
	}
	public Dimension(String name, int w, int d, int h) {
		this(null, name, w, d, h);
	}

	public Dimension(int w, int d, int h) {
		this(null, null, w, d, h);
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
	 * @return true if any rotation of the argument can be placed inside this 
	 * 
	 */

	public boolean canHold(Dimension box) {
		return canHold(box.getWidth(), box.getDepth(), box.getHeight());
	}
	
	public boolean canHold(int w, int d, int h) {
		
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
	
	public int getFootprint() {
		return width * depth;
	}
	
	/**
	 * 
	 * @return true if this can fit within the argument space 
	 */
	
	public boolean fitsInside(Dimension space) {
		return fitsInside(space.getWidth(), space.getDepth(), space.getHeight());
	}

	public boolean fitsInside(int w, int d, int h) {
		
		if(w >= width && h >= height && d >= depth) {
			return true;
		}
		
		return false;
	}

	/**
	 * 
	 * @return true if this can fit within the argument space in any rotation
	 * 
	 */

	public boolean canFitInside(Dimension space) {
		return space.canHold(this);
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
	
	public Dimension getParent() {
		return parent;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + depth;
		result = prime * result + height;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + width;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Dimension other = (Dimension) obj;
		if (depth != other.depth)
			return false;
		if (height != other.height)
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (width != other.width)
			return false;
		return true;
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