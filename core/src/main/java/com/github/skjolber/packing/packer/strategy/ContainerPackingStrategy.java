package com.github.skjolber.packing.packer.strategy;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/** Coordinates packing across one or more available containers. */
public interface ContainerPackingStrategy {

	List<Container> pack(int limit, PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException;
}
