package com.github.skjolber.packing.packer.util;

import java.util.Arrays;

import com.github.skjolber.packing.api.Placement;

/**
 * Snapshot of the cached supporter data used to add or remove a placement's
 * load relationships.
 */
public class LoadPlacementUtilityState {

	private final Placement[] placementSupporters;
	private final long[] placementAreas;

	LoadPlacementUtilityState(Placement[] placementSupporters, long[] placementAreas, int size) {
		this.placementSupporters = Arrays.copyOf(placementSupporters, size);
		this.placementAreas = Arrays.copyOf(placementAreas, size);
	}

	Placement[] getPlacementSupporters() {
		return placementSupporters;
	}

	long[] getPlacementAreas() {
		return placementAreas;
	}
}
