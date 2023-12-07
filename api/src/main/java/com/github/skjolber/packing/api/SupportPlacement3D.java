package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.Surface.Label;

public interface SupportPlacement3D extends Placement3D {

	List<VerticalSupport> getBottomSupports();

	List<VerticalSupport> getTopSupports();

	List<HorizontalSupport> getLeftSupports();

	List<HorizontalSupport> getRightSupports();

	List<HorizontalSupport> getFrontSupports();

	List<HorizontalSupport> getRearSupports();

}
