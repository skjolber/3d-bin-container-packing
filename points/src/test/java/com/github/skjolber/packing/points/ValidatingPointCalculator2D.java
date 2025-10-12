package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;
import com.github.skjolber.packing.ep.points2d.Point2D;

public class ValidatingPointCalculator2D extends DefaultPointCalculator2D {

	public ValidatingPointCalculator2D() {
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
				Point2D point = values.get(i);

				boolean x = point.getMinX() <= p.getAbsoluteEndX() && point.getMaxX() >= p.getAbsoluteX();
				boolean y = point.getMinY() <= p.getAbsoluteEndY() && point.getMaxY() >= p.getAbsoluteY();

				if(x && y) {
					throw new IllegalArgumentException();
				}
			}
		}

	}
}
