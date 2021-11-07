package com.github.skjolber.packing.points3d;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.points2d.DefaultPlacement2D;

public class DefaultPlacement3D extends DefaultPlacement2D implements Placement3D {

	private final int z;
	private int endZ;
	
	public DefaultPlacement3D(int x, int y, int z, int endX, int endY, int endZ) {
		super(x, y, endX, endY);
		this.z = z;
		this.endZ = endZ;
	}

	@Override
	public int getAbsoluteZ() {
		return z;
	}

	@Override
	public int getAbsoluteEndZ() {
		return endZ;
	}
	
	public boolean intersects(Placement3D point) {
		return !(point.getAbsoluteEndX() < x || point.getAbsoluteX() > endX || point.getAbsoluteEndY() < y || point.getAbsoluteY() > endY || point.getAbsoluteEndZ() < z || point.getAbsoluteZ() > endZ);
	}

	@Override
	public String toString() {
		return "DefaultPlacement3D [" + x + "x" + y + "x" + z + " " + endX + "x" + endY + "x" + endZ + "]";
	}


	
}
