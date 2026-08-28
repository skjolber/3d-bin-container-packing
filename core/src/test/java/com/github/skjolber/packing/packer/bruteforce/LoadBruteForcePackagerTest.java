package com.github.skjolber.packing.packer.bruteforce;

class LoadBruteForcePackagerTest extends AbstractLoadBruteForcePackagerTest {

	@Override
	protected LoadBruteForcePackager createPackager() {
		return LoadBruteForcePackager.newBuilder().build();
	}
}
