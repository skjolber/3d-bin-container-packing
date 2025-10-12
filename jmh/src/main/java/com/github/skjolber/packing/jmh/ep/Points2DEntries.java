package com.github.skjolber.packing.jmh.ep;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.ep.points2d.DefaultPointCalculator2D;

@SuppressWarnings("rawtypes")
public class Points2DEntries {

	private final List<Point2DEntry> entries = new ArrayList<>();

	private final DefaultPointCalculator2D extremePoints2D;

	public Points2DEntries(DefaultPointCalculator2D extremePoints2D) {
		this.extremePoints2D = extremePoints2D;
	}

	public void add(Point2DEntry entry) {
		entries.add(entry);
	}

	public List<Point2DEntry> getEntries() {
		return entries;
	}

	public DefaultPointCalculator2D getExtremePoints2D() {
		return extremePoints2D;
	}
}
