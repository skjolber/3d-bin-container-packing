package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/** {@link AbstractFlatEngineTest} for {@linkplain FlatEngine}, which is the reference: the ports to native code run the same tests. */
class FlatEngineTest extends AbstractFlatEngineTest {

	@Override
	protected BruteForceAccelerator engine(FlatEngineOptions options) {
		return options.toFlatEngine();
	}
}
