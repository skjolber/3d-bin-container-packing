package com.github.skjolber.packing.api.packager;


@FunctionalInterface
public interface BoxItemListenerBuilderSupplier {
	
	BoxItemListenerBuilder<?> getBoxItemListenerBuilder();
}