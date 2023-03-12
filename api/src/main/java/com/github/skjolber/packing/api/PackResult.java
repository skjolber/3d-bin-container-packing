package com.github.skjolber.packing.api;

public interface PackResult {

	boolean isEmpty();

	int getSize();

	long getLoadVolume();

	int getLoadWeight();

	int getMaxLoadWeight();

	int getWeight();

	long getVolume();

	long getMaxLoadVolume();

	Container getContainer();
	
	int getIndex();

	boolean containsLastStackable();

}
