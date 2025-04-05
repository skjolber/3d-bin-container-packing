package com.github.skjolber.packing.api.packager;

import com.github.skjolber.packing.api.Container;

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

	int getContainerItemIndex();

	boolean containsLastStackable();

}
