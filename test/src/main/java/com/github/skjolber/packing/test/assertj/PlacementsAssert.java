package com.github.skjolber.packing.test.assertj;

import java.util.List;

import com.github.skjolber.packing.api.Placement;

public class PlacementsAssert extends AbstractPlacementsAssert<PlacementsAssert, List<Placement>> {

	public static PlacementsAssert assertThat(List<Placement> actual) {
		return new PlacementsAssert(actual);
	}

	public PlacementsAssert(List<Placement> actual) {
		super(actual, PlacementsAssert.class);
	}

}
