package com.github.skjolber.packing.api.packager;

@FunctionalInterface
public interface BoxItemGroupListenerBuilderSupplier {
	
	BoxItemGroupListenerBuilder<?> getBoxItemGroupListenerBuilder();
	
}
