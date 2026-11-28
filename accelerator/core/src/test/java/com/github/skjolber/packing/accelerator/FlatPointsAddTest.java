package com.github.skjolber.packing.accelerator;

/** {@link AbstractFlatPointsAddTest} for the Java code, which is the reference: the ports to native code run the same tests. */
class FlatPointsAddTest extends AbstractFlatPointsAddTest {

	@Override
	protected FlatKernel kernel() {
		return JavaFlatKernel.INSTANCE;
	}
}
