package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import com.github.skjolber.packing.api.point.Point;

public class CustomIntYComparator implements IntComparator {

	private static final long serialVersionUID = 1L;

	private Point3DFlagList values;
	private int yy;

	@Override
	public int compare(int value1, int value2) {
		Point o1 = values.get(value1);
		Point o2 = values.get(value2);

		if(o1.getMinZ() < o2.getMinZ()) {
			return -1;
		} else if(o1.getMinZ() != o2.getMinZ()) {
			return 1;
		}

		if(o1.getMinX() < o2.getMinX()) {
			return -1;
		} else if(o1.getMinX() != o2.getMinX()) {
			return 1;
		}

		long volume1 = o1.getDx() * o1.getDz() * (o1.getMaxY() - yy + 1L);
		long volume2 = o2.getDx() * o2.getDz() * (o2.getMaxY() - yy + 1L);

		return -Long.compare(volume1, volume2);
	}

	public void setYy(int yy) {
		this.yy = yy;
	}

	public void setValues(Point3DFlagList values) {
		this.values = values;
	}
}
