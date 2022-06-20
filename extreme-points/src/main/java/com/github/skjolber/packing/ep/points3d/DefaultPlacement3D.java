package com.github.skjolber.packing.ep.points3d;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.Placement3D;

public class DefaultPlacement3D implements Placement3D {

	protected final int x;
	protected final int y;
	protected final int z;
	protected final int endX;
	protected final int endY;
	protected final int endZ;
	
	// protected final List<DefaultPlacement3D> supports;

	public DefaultPlacement3D(int x, int y, int z, int endX, int endY, int endZ) {
		this.x = x;
		this.y = y;
		this.z = z;
		this.endX = endX;
		this.endY = endY;
		this.endZ = endZ;
	}
	
	@Override
	public int getAbsoluteX() {
		return x;
	}

	@Override
	public int getAbsoluteY() {
		return y;
	}

	@Override
	public int getAbsoluteEndX() {
		return endX;
	}

	@Override
	public int getAbsoluteEndY() {
		return endY;
	}

	@Override
	public boolean intersects2D(Placement2D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > endX || point.getAbsoluteEndY() < y || point.getAbsoluteY() > endY);
	}

	@Override
	public boolean intersects3D(Placement3D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > endX || point.getAbsoluteEndY() < y || point.getAbsoluteY() > endY || point.getAbsoluteEndZ() < z || point.getAbsoluteZ() > endZ);
	}
	
	@Override
	public int getAbsoluteZ() {
		return z;
	}

	@Override
	public int getAbsoluteEndZ() {
		return endZ;
	}
	
	@Override
	public String toString() {
		return "DefaultPlacement3D [" + x + "x" + y + "x" + z + " " + endX + "x" + endY + "x" + endZ + "]";
	}

	/*
	@Override
	public List<DefaultPlacement3D> getSupports3D() {
		return (List<DefaultPlacement3D>) supports;
	}

	@Override
	public List<? extends Placement2D> getSupports2D() {
		return supports;
	}
	*/
}
