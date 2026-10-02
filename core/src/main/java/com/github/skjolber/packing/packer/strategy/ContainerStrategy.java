package com.github.skjolber.packing.packer.strategy;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.packer.PackagerAdapter;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

/** Coordinates packing across one or more available containers. */
public interface ContainerStrategy {

	ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerAdapter adapter) throws PackagerInterruptedException;
}
