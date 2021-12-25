package com.github.skjolber.packing.packer.bruteforce;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.api.Point3D;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.points3d.ExtremePoints3D;

public class MemoryExtremePoints3D extends ExtremePoints3D<StackPlacement> {
	
	public MemoryExtremePoints3D(int dx, int dy, int dz) {
		super(dx, dy, dz);
	}

	protected List<Point3D> points = new ArrayList<>();

	@Override
	public boolean add(int index, StackPlacement placement) {
		points.add(values.get(index));
		
		return super.add(index, placement);
	}

	public List<Point3D> getPoints() {
		return points;
	}

	@Override
	public void reset(int dx, int dy, int dz) {
		points.clear();
		
		super.reset(dx, dy, dz);
	}
}
