package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.strategy.BruteForceContainerPackingStrategy.PotentialComparator;

/** Allows every branch to be attempted. */
public final class NoPruningPotentialComparator implements PotentialComparator {

	@Override
	public int compare(List<Container> best, List<Container> prefix, PackagerAdapter state,
			List<Integer> containerIndexes, int selectedContainerIndex, int remainingSlots) {
		return 1;
	}
}
