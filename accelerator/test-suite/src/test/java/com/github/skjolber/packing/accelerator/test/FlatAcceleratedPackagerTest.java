package com.github.skjolber.packing.accelerator.test;

import com.github.skjolber.packing.accelerator.*;

/** {@link AbstractFlatAcceleratedPackagerTest} for {@linkplain FlatEngine}, which is the reference: the ports to native code run the same tests. */
class FlatAcceleratedPackagerTest extends AbstractFlatAcceleratedPackagerTest {

	@Override
	protected BruteForceAccelerator engine(FlatEngineOptions options) {
		return options.toFlatEngine();
	}
}
