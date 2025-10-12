package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

public class ValidatingPointCalculator3D extends DefaultPointCalculator3D {

	public ValidatingPointCalculator3D() {
		super();
	}

	@Override
	public boolean add(int index, Placement placement) {
		boolean add = super.add(index, placement);
		validate(placement);
		return add;
	}

	private void validate(Placement target) {

		for (Placement p : placements) {
			for (int i = 0; i < values.size(); i++) {
				Point point = values.get(i);

				boolean x = point.getMinX() <= p.getAbsoluteEndX() && point.getMaxX() >= p.getAbsoluteX();
				boolean y = point.getMinY() <= p.getAbsoluteEndY() && point.getMaxY() >= p.getAbsoluteY();
				boolean z = point.getMinZ() <= p.getAbsoluteEndZ() && point.getMaxZ() >= p.getAbsoluteZ();

				if(x && y && z) {
					throw new IllegalArgumentException();
				}
			}
		}

	}

}
