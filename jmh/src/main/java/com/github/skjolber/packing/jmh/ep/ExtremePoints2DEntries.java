package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.ep.points2d.ExtremePoints2D;

@SuppressWarnings("rawtypes")
public class ExtremePoints2DEntries {

	private final List<ExtremePoint2DEntry> entries = new ArrayList<>();

	private final ExtremePoints2D extremePoints2D;

	public ExtremePoints2DEntries(ExtremePoints2D extremePoints2D) {
		this.extremePoints2D = extremePoints2D;
	}

	public void add(ExtremePoint2DEntry entry) {
		entries.add(entry);
	}

	public List<ExtremePoint2DEntry> getEntries() {
		return entries;
	}

	public ExtremePoints2D getExtremePoints2D() {
		return extremePoints2D;
	}
}
