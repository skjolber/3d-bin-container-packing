package com.github.skjolber.packing.jmh.ep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PointsBenchmark3DTest {

	/** The recording must contain a real packing, and replays must be repeatable. */
	@Test
	public void recordedReplayIsRepeatable() throws Exception {
		Points3DRecordedState state = new Points3DRecordedState();
		state.init();
		long adds = state.getOperations().stream().filter(o -> o.type == Points3DRecordedState.ADD).count();
		assertTrue(adds > 50, "Expected a real packing, got " + adds + " adds");

		PointsBenchmark3D benchmark = new PointsBenchmark3D();
		int first = benchmark.points3DRecorded(state);
		int second = benchmark.points3DRecorded(state);
		assertEquals(first, second);
	}
}
