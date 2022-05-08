package com.github.skjolber.packing.api;

public interface PackResult {

	boolean isEmpty();

	int getCount();
	long getLoadVolume();
	int getLoadWeight();
	
	int getMaxLoadWeight();
	int getWeight();
	
	long getVolume();
	long getMaxLoadVolume();

	Stack getStack();

	boolean containsLastStackable();

}
