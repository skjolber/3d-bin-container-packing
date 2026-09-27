package com.github.skjolber.packing.jmh.boundingbox;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.boundingbox.BoundingBox;
import com.github.skjolber.packing.boundingbox.BoundingBoxLayout;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;
import com.github.skjolber.packing.validator.load.IdenticalBoxOnlyLoadValidator;
import com.github.skjolber.packing.validator.load.MaxBoxCountLoadValidator;
import com.github.skjolber.packing.validator.load.MaxPressureLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;

/** Fixture correctness tests, not performance measurements. Never invokes an @Benchmark method. */
class BruteForceBoundingBoxSearchBenchmarkTest {

	/*
	 * Every fixture has a filled floor arrangement. For boxCount = 4:
	 *
	 * Identical / rotated:                    Mixed:
	 *
	 *       +-------+-------+                 +-------+-------+
	 *       |   A   |   A   |                 |       |       |
	 *       +-------+-------+                 |   B   |   B   |
	 *       |   A   |   A   |                 |       |       |
	 *       +-------+-------+                 +-------+-------+
	 *                                        |   A   |   A   |
	 *                                        +-------+-------+
	 *
	 * A = 2 x 1 x 1, B = 2 x 2 x 1. Six-box inputs add a third column.
	 * Other search layouts exercise vertical loading; returned load graphs must
	 * satisfy weight, pressure, depth and identical-only validators.
	 */
	@TestFactory
	List<DynamicTest> allBenchmarkInputsProduceValidResults() {
		List<DynamicTest> tests = new ArrayList<>();
		for(String workload : List.of("identical", "mixed", "rotated")) {
			for(int count : new int[] {4, 6}) {
				for(boolean load : new boolean[] {false, true}) {
					tests.add(DynamicTest.dynamicTest(workload + "/" + count + "/load=" + load, () -> verify(workload, count, load)));
				}
			}
		}
		return tests;
	}

	protected void verify(String workload, int count, boolean load) {
		BruteForceBoundingBoxSearchState state = new BruteForceBoundingBoxSearchState();
		state.workload = workload;
		state.boxCount = count;
		state.load = load;
		state.setup();
		// Only correctness tests are time-bounded; the actual benchmarks run to completion.
		ScheduledThreadPoolExecutor scheduler = new ScheduledThreadPoolExecutor(1);
		try(PackagerInterruptSupplier stop = PackagerInterruptSupplierBuilder.builder().withScheduledThreadPoolExecutor(scheduler)
				.withDeadline(System.currentTimeMillis() + 30_000).build()) {
			state.interrupt = stop;
			assertNotSame(state.newSingleObjectiveSearch(), state.newSingleObjectiveSearch());

			BruteForceBoundingBoxResult single = state.newSingleObjectiveSearch().pack(System.nanoTime());
			assertEquals(Termination.EXHAUSTED, single.getTermination());
			assertValid(single, state);

			BruteForceBoundingBoxResult multiSingle = state.newSingleObjectiveViaMultiSearch().pack(System.nanoTime());
			assertEquals(Termination.EXHAUSTED, multiSingle.getTermination());
			assertValid(multiSingle, state);
			assertEquals(single.getObjectiveResults().get("volume").getBoundingBox(), multiSingle.getObjectiveResults().get("volume").getBoundingBox());

			BruteForceBoundingBoxResult multi = state.newVolumeAndAxesSearch().pack(System.nanoTime());
			assertEquals(Termination.EXHAUSTED, multi.getTermination());
			assertEquals(List.of("volume", "x", "y", "z"), new ArrayList<>(multi.getObjectiveResults().keySet()));
			assertValid(multi, state);
			assertEquals(single.getObjectiveResults().get("volume").getBoundingBox(), multi.getObjectiveResults().get("volume").getBoundingBox());

			BruteForceBoundingBoxResult goal = state.newFilledGoalSearch().pack(System.nanoTime());
			assertEquals(Termination.GOAL_REACHED, goal.getTermination());
			assertValid(goal, state);
			assertEquals(state.volume, goal.getObjectiveResults().get("volume").getBoundingBox().getVolume());
			assertEquals(1, goal.getReachedGoals().size());
			assertTrue(goal.getReachedGoals().contains("volume"));

			assertTrue(state.container.getStack().isEmpty());
			for(int i = 0; i < state.items.size(); i++) {
				BoxItem item = state.items.get(i);
				assertEquals(state.items.size() == 1 ? count : count / 2, item.getCount());
				assertEquals(i == 0 ? 7 : 11, item.getLocalIndex());
				assertEquals(i == 0 ? 19 : 23, item.getGlobalIndex());
				assertSame(item, item.getBox().getBoxItem());
			}
		} finally {
			scheduler.shutdownNow();
		}
	}

	protected void assertValid(BruteForceBoundingBoxResult result, BruteForceBoundingBoxSearchState state) {
		assertTrue(result.isSuccess());
		for(BoundingBoxLayout layout : result.getResults()) {
			List<Placement> placements = layout.getStack().getPlacements();
			assertEquals(state.boxCount, placements.size());
			Map<BoxItem, Integer> counts = new IdentityHashMap<>();
			int dx = 0, dy = 0, dz = 0;
			for(int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				counts.merge(placement.getBoxItem(), 1, Integer::sum);
				assertTrue(placement.getAbsoluteX() >= 0 && placement.getAbsoluteEndX() < state.container.getLoadDx());
				assertTrue(placement.getAbsoluteY() >= 0 && placement.getAbsoluteEndY() < state.container.getLoadDy());
				assertTrue(placement.getAbsoluteZ() >= 0 && placement.getAbsoluteEndZ() < state.container.getLoadDz());
				dx = Math.max(dx, placement.getAbsoluteEndX() + 1);
				dy = Math.max(dy, placement.getAbsoluteEndY() + 1);
				dz = Math.max(dz, placement.getAbsoluteEndZ() + 1);
				for(int j = 0; j < i; j++) {
					assertFalse(placement.intersects3D(placements.get(j)));
				}
			}
			assertEquals(new BoundingBox(dx, dy, dz), layout.getBoundingBox());
			assertEquals(state.items.size(), counts.size());
			for(BoxItem item : state.items) {
				assertEquals(item.getCount(), counts.get(item).intValue());
			}
			if(state.load) {
				List<ValidatorResultReason> reasons = new ArrayList<>();
				assertTrue(new WeightLoadValidator().isValid(placements, reasons), () -> reasons.toString());
				assertTrue(new MaxPressureLoadValidator().isValid(placements, reasons), () -> reasons.toString());
				assertTrue(new MaxBoxCountLoadValidator().isValid(placements, reasons), () -> reasons.toString());
				assertTrue(new IdenticalBoxOnlyLoadValidator().isValid(placements, reasons), () -> reasons.toString());
			}
		}
	}
}
