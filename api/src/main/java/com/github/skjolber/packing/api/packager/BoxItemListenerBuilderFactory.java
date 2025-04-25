package com.github.skjolber.packing.api.packager;


@FunctionalInterface
public interface BoxItemListenerBuilderFactory {
	
	BoxItemListenerBuilder<?> createBoxItemListenerBuilder();
}