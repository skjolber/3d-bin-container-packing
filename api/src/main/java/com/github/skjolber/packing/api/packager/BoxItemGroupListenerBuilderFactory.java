package com.github.skjolber.packing.api.packager;

@FunctionalInterface
public interface BoxItemGroupListenerBuilderFactory {
	
	BoxItemGroupListenerBuilder<?> createBoxItemGroupListenerBuilder();
	
}
