package com.github.skjolber.packing.packer.composite;

import java.util.List;

import com.github.skjolber.packing.packer.IntermediatePackagerResult;
import com.github.skjolber.packing.packer.PackagerAdapter;

public class DefaultPackagerAdapterCompositeManager implements PackagerAdapterCompositeManager {

	private List<PackagerAdapter> packagers;

	public DefaultPackagerAdapterCompositeManager(List<PackagerAdapter> packagers) {
		this.packagers = packagers;
	}

	@Override
	public boolean accept(int packagerAdapterIndex, IntermediatePackagerResult result) {
		return false;
	}

	@Override
	public List<PackagerAdapter> getAdapters() {
		return packagers;
	}

	

}
