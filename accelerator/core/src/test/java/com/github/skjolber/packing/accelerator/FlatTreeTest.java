package com.github.skjolber.packing.accelerator;

/** {@link AbstractFlatTreeTest} for {@linkplain FlatEngine}, which is the reference: the ports to native code run the same tests. */
class FlatTreeTest extends AbstractFlatTreeTest {

	@Override
	protected BruteForceAccelerator engine(FlatEngineOptions options) {
		return options.toFlatEngine();
	}
}
