package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

public class ExtremePoints3DEntries {

	private final List<ExtremePoint3DEntry> entries = new ArrayList<>();

	private final ExtremePoints3D extremePoints3D;
	
	public ExtremePoints3DEntries(ExtremePoints3D extremePoints3D) {
		this.extremePoints3D = extremePoints3D;
	}
	
	public void add(ExtremePoint3DEntry entry) {
		entries.add(entry);
	}
	
	public List<ExtremePoint3DEntry> getEntries() {
		return entries;
	}
	
	public ExtremePoints3D getExtremePoints3D() {
		return extremePoints3D;
	}
}
