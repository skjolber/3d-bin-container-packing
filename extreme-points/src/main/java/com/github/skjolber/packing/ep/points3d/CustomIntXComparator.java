package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import com.github.skjolber.packing.api.ep.Point3D;

public class CustomIntXComparator implements IntComparator {

	private static final long serialVersionUID = 1L;

	private Point3DFlagList values;
	private int xx;

	@Override
	public int compare(int value1, int value2) {
		Point3D o1 = values.get(value1);
		Point3D o2 = values.get(value2);

		if(o1.getMinY() < o2.getMinY()) {
			return -1;
		} else if(o1.getMinY() != o2.getMinY()) {
			return 1;
		}

		if(o1.getMinZ() < o2.getMinZ()) {
			return -1;
		} else if(o1.getMinZ() != o2.getMinZ()) {
			return 1;
		}

		long volume1 = o1.getDy() * o1.getDz() * (o1.getMaxX() - xx + 1L);
		long volume2 = o2.getDy() * o2.getDz() * (o2.getMaxX() - xx + 1L);

		return -Long.compare(volume1, volume2);
	}

	public void setXx(int xx) {
		this.xx = xx;
	}

	public void setValues(Point3DFlagList values) {
		this.values = values;
	}
}
