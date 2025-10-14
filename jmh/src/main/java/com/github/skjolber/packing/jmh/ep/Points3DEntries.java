package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

@SuppressWarnings("rawtypes")
public class Points3DEntries {

	private final List<Point3DEntry> entries = new ArrayList<>();

	private DefaultPointCalculator3D extremePoints3D;

	public void add(Point3DEntry entry) {
		entries.add(entry);
	}

	public List<Point3DEntry> getEntries() {
		return entries;
	}

	public DefaultPointCalculator3D getExtremePoints3D() {
		return extremePoints3D;
	}
	
	public void setExtremePoints3D(DefaultPointCalculator3D extremePoints3D) {
		this.extremePoints3D = extremePoints3D;
	}
}
