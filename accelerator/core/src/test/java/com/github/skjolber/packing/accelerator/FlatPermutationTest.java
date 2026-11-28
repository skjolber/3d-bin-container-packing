package com.github.skjolber.packing.accelerator;

/** {@link AbstractFlatPermutationTest} for the Java code, which is the reference: the ports to native code run the same tests. */
class FlatPermutationTest extends AbstractFlatPermutationTest {

	@Override
	protected FlatKernel kernel() {
		return JavaFlatKernel.INSTANCE;
	}
}
