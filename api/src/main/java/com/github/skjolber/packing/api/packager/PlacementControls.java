package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Placement;

public interface PlacementControls<R extends Placement> {

	R getPlacement(int offset, int length);
	
}
