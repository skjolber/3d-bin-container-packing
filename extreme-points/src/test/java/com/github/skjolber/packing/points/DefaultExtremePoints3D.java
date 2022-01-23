package com.github.skjolber.packing.points;

import com.github.skjolber.packing.api.Placement3D;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class DefaultExtremePoints3D extends ExtremePoints3D<Placement3D> {

	public DefaultExtremePoints3D(int dx, int dy, int dz) {
		super(dx, dy, dz);
	}

	@Override
	public boolean add(int index, Placement3D placement) {
		boolean add = super.add(index, placement);
		validate(placement);
		return add;
	}

	private void validate(Placement3D target) {
		
		for (Placement3D p : placements) {
			for (int i = 0; i < values.size(); i++) {
				Point3D point = values.get(i);
			
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
