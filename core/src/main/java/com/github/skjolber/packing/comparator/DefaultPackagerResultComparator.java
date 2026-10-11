package com.github.skjolber.packing.comparator;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultComparator;

/**
 * Prefers successful results, then lower cost, then fewer containers, then less total container volume.
 */
public class DefaultPackagerResultComparator implements PackagerResultComparator {

	@Override
	public boolean prefersFewerContainers() {
		return true;
	}

	@Override
	public int compare(PackagerResult a, PackagerResult b) {
		if(a.isSuccess() != b.isSuccess()) {
			return a.isSuccess() ? 1 : -1;
		}
		if(!a.isSuccess()) {
			return 0;
		}
		if(a.getCost() != b.getCost()) {
			return a.getCost() < b.getCost() ? 1 : -1;
		}
		if(a.size() != b.size()) {
			return a.size() < b.size() ? 1 : -1;
		}
		long volumeA = getContainerVolume(a);
		long volumeB = getContainerVolume(b);
		if(volumeA != volumeB) {
			return volumeA < volumeB ? 1 : -1;
		}
		return 0;
	}

	private static long getContainerVolume(PackagerResult result) {
		long volume = 0L;
		for(int i = 0; i < result.size(); i++) {
			volume += result.get(i).getVolume();
		}
		return volume;
	}
}
