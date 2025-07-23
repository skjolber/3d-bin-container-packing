package com.github.skjolber.packing.api;

import java.util.List;

public class BoxStackValue {
	
	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height

	protected final long area;

	protected final List<Surface> surfaces;
	protected long volume;
	
	protected final int index;
	protected Box box;

	public BoxStackValue(int dx, int dy, int dz, List<Surface> surfaces, int index) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.surfaces = surfaces;

		this.area = (long)dx * (long)dy;
		this.volume = (long)dx * (long)dy * (long)dz;
		
		this.index = index;
	}
	
	protected BoxStackValue(BoxStackValue other) {
		this.dx = other.dx;
		this.dy = other.dy;
		this.dz = other.dz;
		this.surfaces = other.surfaces;

		this.area = other.area;
		this.volume = other.volume;
		this.index = other.index;
		
		this.box = other.box;
	}
	
	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

	public int getDz() {
		return dz;
	}

	/**
	 * Check whether this object fits within a dimension (without rotation).
	 *
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space
	 */

	public boolean fitsInside3D(Dimension dimension) {
		return fitsInside3D(dimension.getDx(), dimension.getDy(), dimension.getDz());
	}

	public boolean fitsInside3D(int dx, int dy, int dz) {
		return dx >= this.dx && dy >= this.dy && dz >= this.dz;
	}

	public boolean fitsInside2D(int dx, int dy) {
		return dx >= this.dx && dy >= this.dy;
	}

	public long getArea() {
		return area;
	}

	public long getVolume() {
		return volume;
	}

	public List<Surface> getSurfaces() {
		return surfaces;
	}

	@Override
	public String toString() {
		return "StackValue[" + surfaces + " " + dx + "x" + dy + "x" + dz + "]";
	}
	
	@Override
	public BoxStackValue clone() {
		return new BoxStackValue(this);
	}
	
	public void setBox(Box box) {
		this.box = box;
	}
	
	public Box getBox() {
		return box;
	}
	
	public int getIndex() {
		return index;
	}

	
}
