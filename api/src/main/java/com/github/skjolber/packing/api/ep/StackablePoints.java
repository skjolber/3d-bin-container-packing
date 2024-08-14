package com.github.skjolber.packing.api.ep;

import java.io.Serializable;
import java.util.List;

import com.github.skjolber.packing.api.StackPlacement;

public interface StackablePoints {

	List<Point3D> getStackablePoints();
	List<StackPlacement> getPlacements();
}
