package com.github.skjolber.packing.ep.points3d;

import org.eclipse.collections.api.block.comparator.primitive.IntComparator;

import com.github.skjolber.packing.api.ep.Point3D;

public class CustomIntZComparator implements IntComparator {

	private static final long serialVersionUID = 1L;
	
	private Point3DFlagList<?> values;
	private int zz;

	@Override
	public int compare(int value1, int value2) {
		
		Point3D<?> o1 = values.get(value1);
		Point3D<?> o2 = values.get(value2);
		
		int x = Integer.compare(o1.getMinX(), o2.getMinX());

		if(x == 0) {
			x = Integer.compare(o1.getMinY(), o2.getMinY());
		}

		if(x == 0) {
			long volume1 = o1.getArea() * (o1.getMaxZ() - zz + 1);
			long volume2 = o2.getArea() * (o2.getMaxZ() - zz + 1);
			
			return -Long.compare(volume1, volume2);
		}

		return x;
	}
	
	public void setZz(int zz) {
		this.zz = zz;
	}

	public void setValues(Point3DFlagList<?> values) {
		this.values = values;
	}
}
