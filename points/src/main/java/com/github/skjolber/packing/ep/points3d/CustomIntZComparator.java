package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import com.github.skjolber.packing.api.point.Point;

public class CustomIntZComparator implements IntComparator {

	private static final long serialVersionUID = 1L;

	private Point3DFlagList values;

	@Override
	public int compare(int value1, int value2) {
		Point o1 = values.get(value1);
		Point o2 = values.get(value2);

		if(o1.getMinX() < o2.getMinX()) {
			return -1;
		} else if(o1.getMinX() != o2.getMinX()) {
			return 1;
		}

		if(o1.getMinY() < o2.getMinY()) {
			return -1;
		} else if(o1.getMinY() != o2.getMinY()) {
			return 1;
		}

		// not exact volume, but good enough for comparison
		long volume1 = o1.getArea() * o1.getMaxZ();
		long volume2 = o2.getArea() * o2.getMaxZ();

		// inline -Long.compare(volume1, volume2)
		return (volume2 < volume1) ? -1 : ((volume2 == volume1) ? 0 : 1);
	}

	public void setValues(Point3DFlagList values) {
		this.values = values;
	}
}
