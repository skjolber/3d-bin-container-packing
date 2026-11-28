package com.github.skjolber.packing.accelerator;

/** {@link AbstractFlatBestPointTest} for the Java code, which is the reference: the ports to native code run the same tests. */
class FlatBestPointTest extends AbstractFlatBestPointTest {

	@Override
	protected FlatKernel kernel() {
		return JavaFlatKernel.INSTANCE;
	}
}
