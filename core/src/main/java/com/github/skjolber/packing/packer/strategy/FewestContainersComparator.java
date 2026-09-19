package com.github.skjolber.packing.packer.strategy;

import java.util.Comparator;
import java.util.List;

import com.github.skjolber.packing.api.Container;

/** Prefers complete packings with fewer containers. */
public final class FewestContainersComparator implements Comparator<List<Container>> {

	@Override
	public int compare(List<Container> first, List<Container> second) {
		return Integer.compare(second.size(), first.size());
	}
}
