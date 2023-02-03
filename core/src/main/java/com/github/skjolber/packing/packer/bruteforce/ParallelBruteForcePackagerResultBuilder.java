package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;

import com.github.skjolber.packing.api.PackagerResult;

public class ParallelBruteForcePackagerResultBuilder extends BruteForcePackagerResultBuilder {

	@Override
	public PackagerResult build() {
		try {
			return super.build();
		} catch (ParallelBruteForcePackagerException e) {
			return new PackagerResult(Collections.emptyList(), 0);
		}
	}
}
