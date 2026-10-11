package com.github.skjolber.packing.jmh;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Runs the plain packager benchmark once per product set, so that broken benchmark setup
 * (for example products not reaching the packager) fails the build.
 */
public class TychoBenchmarkTest {

	@Test
	public void plainPackager22() throws Exception {
		assertPacks("22");
	}

	@Test
	public void plainPackager33() throws Exception {
		assertPacks("33");
	}

	@Test
	public void plainPackager93() throws Exception {
		assertPacks("93");
	}

	private static void assertPacks(String boxes) throws Exception {
		TychoPackagerState state = new TychoPackagerState();
		state.init();
		try {
			TychoBenchmark benchmark = new TychoBenchmark();
			benchmark.setBoxes(boxes);
			benchmark.init();
			assertEquals(1, benchmark.plainPackager(state));
		} finally {
			state.shutdown();
		}
	}
}
