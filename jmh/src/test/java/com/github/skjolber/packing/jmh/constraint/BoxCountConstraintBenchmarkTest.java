package com.github.skjolber.packing.jmh.constraint;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.visualizer.packaging.DefaultPackagingResultVisualizerFactory;

/**
 * Functional tests for {@link BoxCountConstraintBenchmark}.
 * <p>
 * Calls the same {@code packPlain}/{@code packLaff} methods that JMH invokes,
 * asserts that all 20 boxes fit in one container, and writes the result to
 * {@code containers.json} for inspection in the Three.js viewer.
 */
public class BoxCountConstraintBenchmarkTest {

	private static final File OUTPUT =
			new File("../visualizer/viewer/public/assets/containers.json");

	private BoxCountConstraintBenchmarkState state;
	private BoxCountConstraintBenchmark benchmark;

	@BeforeEach
	public void init() {
		state = new BoxCountConstraintBenchmarkState();
		state.init();
		benchmark = new BoxCountConstraintBenchmark();
	}

	@AfterEach
	public void shutdown() {
		state.shutdown();
	}

	@Test
	public void plainPackager() throws Exception {
		PackagerResult result = benchmark.packPlain(state);
		assertTrue(result.isSuccess(), "Expected all boxes to fit in one container");
		new DefaultPackagingResultVisualizerFactory(true)
				.visualize(result.getContainers(), OUTPUT);
	}

	@Test
	public void laffPackager() throws Exception {
		PackagerResult result = benchmark.packLaff(state);
		assertTrue(result.isSuccess(), "Expected all boxes to fit in one container");
		new DefaultPackagingResultVisualizerFactory(true)
				.visualize(result.getContainers(), OUTPUT);
	}

	/** Two box types with different {@code maxLoadBoxCount} limits — both fit in one container. */
	@Test
	public void plainPackager_mixed() throws Exception {
		PackagerResult result = benchmark.packPlainMixed(state);
		assertTrue(result.isSuccess(), "Expected all mixed-count boxes to fit in one container");
		new DefaultPackagingResultVisualizerFactory(true)
				.visualize(result.getContainers(), OUTPUT);
	}

	/** Two box types with different {@code maxLoadBoxCount} limits — LAFF variant. */
	@Test
	public void laffPackager_mixed() throws Exception {
		PackagerResult result = benchmark.packLaffMixed(state);
		assertTrue(result.isSuccess(), "Expected all mixed-count boxes to fit in one container");
		new DefaultPackagingResultVisualizerFactory(true)
				.visualize(result.getContainers(), OUTPUT);
	}
}
