package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.BoxItem;

public interface CompositePackagerAdapterGuide {

	boolean accept(IntermediatePackagerResult result, List<BoxItem> remaining, PackagerAdapterBuilderFactory factory);

	List<PackagerAdapter> getAdapters();
}