package com.github.skjolber.packing.packer.bruteforce;

import java.util.List;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.FilteredBoxItems;

public interface BruteForceSearchStrategy {

	boolean isStop(BruteForceIntermediatePackagerResult result, ContainerItem containerItem, FilteredBoxItems boxItems, List<ContainerItem> containerItems);
	
}
