package com.github.skjolber.packing.api;

import java.util.List;

import com.github.skjolber.packing.api.Surface.Label;

public interface SupportedPlacement3D extends Placement3D {

	List<SupportedPlacement3D> getBottomSupports();

	List<SupportedPlacement3D> getTopSupports();

	List<SupportedPlacement3D> getLeftSupports();

	List<SupportedPlacement3D> getRightSupports();

	List<SupportedPlacement3D> getFrontSupports();

	List<SupportedPlacement3D> getRearSupports();

	List<SupportedPlacement3D> getSupports(Label label);

}
