package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/** {@link AbstractFlatSearchPermutationTest} for the Java code, which is the reference: the ports to native code run the same tests. */
class FlatSearchPermutationTest extends AbstractFlatSearchPermutationTest {

	@Override
	protected FlatKernel kernel() {
		return JavaFlatKernel.INSTANCE;
	}
}
