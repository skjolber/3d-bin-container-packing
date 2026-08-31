package com.github.skjolber.packing.packer.bruteforce;

class LoadFastBruteForcePackagerTest extends AbstractLoadBruteForcePackagerTest {

	@Override
	protected LoadFastBruteForcePackager createPackager() {
		return LoadFastBruteForcePackager.newBuilder().build();
	}
}
