package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;

import com.github.skjolber.packing.api.PackagerResult;

public class ParallelBruteForcePackagerResultBuilder extends BruteForcePackagerResultBuilder {

	@Override
	public PackagerResult build() {
		long timestamp = System.nanoTime();
		try {
			return super.build();
		} catch (ParallelBruteForcePackagerException e) {
			return new PackagerResult(Collections.emptyList(), (System.nanoTime() - timestamp) / 1000000L , false);
		}
	}
}
