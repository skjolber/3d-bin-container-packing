package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class DefaultExtremePoints3D extends ExtremePoints3D {

	public DefaultExtremePoints3D() {
		super();
	}

	@Override
	public boolean add(int index, StackPlacement placement) {
		boolean add = super.add(index, placement);
		validate(placement);
		return add;
	}

	private void validate(StackPlacement target) {

		for (StackPlacement p : placements) {
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
