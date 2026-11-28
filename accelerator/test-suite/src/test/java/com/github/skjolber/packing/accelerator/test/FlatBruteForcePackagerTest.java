package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/** {@link AbstractFlatBruteForcePackagerTest} for {@linkplain FlatEngine}, which is the reference: the ports to native code run the same tests. */
class FlatBruteForcePackagerTest extends AbstractFlatBruteForcePackagerTest {

	@Override
	protected BruteForceAccelerator engine(FlatEngineOptions options) {
		return options.toFlatEngine();
	}
}
