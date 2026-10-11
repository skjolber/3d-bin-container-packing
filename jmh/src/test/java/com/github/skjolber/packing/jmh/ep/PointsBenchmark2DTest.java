package com.github.skjolber.packing.jmh.ep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class PointsBenchmark2DTest {

	/** The recording must contain a real packing, and replays must be repeatable. */
	@Test
	public void recordedReplayIsRepeatable() throws Exception {
		Points2DRecordedState state = new Points2DRecordedState();
		state.init();
		int adds = 0;
		for(Points2DRecordedState.Operation operation : state.getOperations()) {
			if(operation.type == Points2DRecordedState.ADD) {
				adds++;
			}
		}
		assertTrue(adds > 50, "Expected a real packing, got " + adds + " adds");

		PointsBenchmark2D benchmark = new PointsBenchmark2D();
		int first = benchmark.points2DRecorded(state);
		int second = benchmark.points2DRecorded(state);
		assertEquals(first, second);
	}
}
