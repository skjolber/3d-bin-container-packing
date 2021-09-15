package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.impl.points.ExtremePoints2D;

public interface LargestAreaFitFirstScorer {

	int searchFirstBox(List<Box> boxes, ExtremePoints2D extremePoints, SearchContext context);

	int searchBox(List<Box> boxes, ExtremePoints2D extremePoints, SearchContext context);

}
