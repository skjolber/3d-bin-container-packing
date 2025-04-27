package com.github.skjolber.packing.packer.laff;

import com.github.skjolber.packing.comparator.IntermediatePackagerResultComparator;
import com.github.skjolber.packing.packer.AbstractDefaultPackager;

/**
 * Fit boxes into container, i.e. perform bin packing to a single container.
 * <br>
 * <br>
 * Thread-safe implementation. The input Boxes must however only be used in a single thread at a time.
 */
public abstract class AbstractLargestAreaFitFirstPackager extends AbstractDefaultPackager {

	public AbstractLargestAreaFitFirstPackager(IntermediatePackagerResultComparator packResultComparator) {
		super(packResultComparator);
	}

}
