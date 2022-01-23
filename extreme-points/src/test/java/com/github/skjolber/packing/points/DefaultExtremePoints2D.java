package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.Placement2D;
import com.github.skjolber.packing.api.ep.Point2D;
import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;

public class DefaultExtremePoints2D extends ExtremePoints2D<Placement2D> {

	public DefaultExtremePoints2D(int dx, int dy) {
		super(dx, dy);
	}

	@Override
	public boolean add(int index, Placement2D placement) {
		boolean add = super.add(index, placement);
		validate(placement);
		return add;
	}

	private void validate(Placement2D target) {
		
		for (Placement2D p : placements) {
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
