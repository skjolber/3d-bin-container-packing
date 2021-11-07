package com.github.skjolber.packing.packer.laff;

import java.util.List;

import com.github.skjolber.packing.old.Box;
import com.github.skjolber.packing.points2d.ExtremePoints2D;

public interface LargestAreaFitFirstScorer {

	int searchFirstBox(List<Box> boxes, ExtremePoints2D extremePoints, SearchContext context);

	int searchBox(List<Box> boxes, ExtremePoints2D extremePoints, SearchContext context);

}