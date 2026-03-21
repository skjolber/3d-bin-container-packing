package com.github.skjolber.packing.packer.composite;

public class DefaultPackagerAdapterCompositeManagerBuilder extends AbstractPackagerAdapterCompositeManagerBuilder<DefaultPackagerAdapterCompositeManagerBuilder> {

	@Override
	public PackagerAdapterCompositeManager build() {
		return new DefaultPackagerAdapterCompositeManager(packagers);
	}

}
