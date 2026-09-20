package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.packer.PackagerAdapter;

/** Selects a complete packing using the fewest containers. */
public final class FewestContainersControls implements BruteForceContainerStrategy.Controls {

	private ContainerResult best;

	@Override
	public boolean attempt(List<Container> containers, PackagerAdapter state,
			List<Integer> availableContainerIndexes, int selectedContainerIndex) {
		return best == null || containers.size() + 1 < best.getPackList().size();
	}

	@Override
	public boolean result(ContainerResult result) {
		if(best == null || result.getPackList().size() < best.getPackList().size()) {
			best = result;
		}
		return true;
	}

	@Override
	public ContainerResult getResult() {
		return best;
	}
}
