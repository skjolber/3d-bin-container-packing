package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerAdapterBuilderFactory;

public interface PackagerAdapterGuide {

	boolean accept(IntermediatePackagerResult result, List<BoxItem> remaining, PackagerAdapterBuilderFactory factory);

	List<PackagerAdapter> getAdapters();
}