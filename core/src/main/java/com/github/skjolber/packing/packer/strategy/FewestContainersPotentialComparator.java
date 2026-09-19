package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.strategy.BruteForceContainerPackingStrategy.PotentialComparator;

/** Prunes a branch that cannot use fewer containers than the current best. */
public final class FewestContainersPotentialComparator implements PotentialComparator {

	@Override
	public int compare(List<Container> best, List<Container> prefix, PackagerAdapter state,
			List<Integer> containerIndexes, int selectedContainerIndex, int remainingSlots) {
		return Integer.compare(best.size(), prefix.size() + 1);
	}
}
