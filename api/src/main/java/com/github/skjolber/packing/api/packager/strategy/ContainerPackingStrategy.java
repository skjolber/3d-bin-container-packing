package com.github.skjolber.packing.api.packager.strategy;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;

/**
 * Decides which containers to use for a packaging operation, and in which order: for example the
 * first container (in preference order) which holds all boxes, or the lowest cost combination of
 * containers.
 */
public interface ContainerPackingStrategy {

	/**
	 * @param interrupt checked regularly; stop when it returns true
	 * @param session the packaging operation
	 * @return the accepted containers and their total cost, or null if the boxes could not be packed
	 * @throws PackagerInterruptedException if interrupted
	 */
	ContainerResult pack(PackagerInterruptSupplier interrupt, PackagerSession session) throws PackagerInterruptedException;
}
