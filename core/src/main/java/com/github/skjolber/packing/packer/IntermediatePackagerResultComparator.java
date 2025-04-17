package com.github.skjolber.packing.packer;

import java.util.Comparator;

public interface IntermediatePackagerResultComparator extends Comparator<IntermediatePackagerResult> {

	static final int ARGUMENT_1_IS_BETTER = 1;
	static final int ARGUMENT_2_IS_BETTER = -1;

	/**
	 * 
	 * Returns {@linkplain ARGUMENT_2_IS_BETTER} if o1 is less / worse than o2.
	 * Returns {@linkplain ARGUMENT_1_IS_BETTER} if o1 is more / better than o2.
	 * 
	 * Return 0 otherwise.
	 */

	@Override
	int compare(IntermediatePackagerResult o1, IntermediatePackagerResult o2);

}
