package com.github.skjolber.packing.tornado;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.interrupt.PackagerInterruptedException;
import com.github.skjolber.packing.packer.bruteforce.flat.AcceleratorWinner;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatEngine;
import com.github.skjolber.packing.packer.bruteforce.flat.FlatProblem;
import com.github.skjolber.packing.tornado.KernelParity.Instance;

/**
 * The winner of the kernel must be the winner of the CPU engine, which the tests of the core module pin to the packager's loop. Skipped without a
 * TornadoVM runtime and device; see the README for how to run with one ({@code -Dtornado.argfile=...}).
 */
class KernelParityTest {

	private static FlatEngine cpu;

	@BeforeAll
	static void requireDevice() {
		TornadoBruteForceAccelerator accelerator = TornadoBruteForceAccelerator.newBuilder().build();
		if(accelerator.getDevice().isEmpty() && TornadoDevices.getLastFailure() != null) {
			TornadoDevices.getLastFailure().printStackTrace(System.out);
		}
		Assumptions.assumeTrue(accelerator.getDevice().isPresent(), () -> "no TornadoVM device: " + TornadoDevices.getLastFailure());
		cpu = FlatEngine.newBuilder().withMinBoxes(1).build();
	}

	@AfterAll
	static void closeEngine() {
		if(cpu != null) {
			cpu.close();
		}
	}

	private static TornadoBruteForceAccelerator.Builder device() {
		return TornadoBruteForceAccelerator.newBuilder().withMinBoxes(1);
	}

	/** @return the number of problems the device gave up on */
	private static int compare(TornadoBruteForceAccelerator device, long firstSeed, int seeds, int minBoxes, int maxBoxes, boolean mayGiveUp) throws PackagerInterruptedException {
		int gaveUp = 0;
		try {
			for (int i = 0; i < seeds; i++) {
				long seed = firstSeed + i;
				Instance instance = KernelParity.random(seed, minBoxes, maxBoxes, 4, 9);
				FlatProblem problem = KernelParity.problem(instance);

				Optional<AcceleratorWinner> expected = cpu.search(problem, 0L, () -> false);
				Optional<AcceleratorWinner> actual = device.search(problem, 0L, () -> false);
				assertThat(device.getLastFailure()).as("seed " + seed).isNull();
				if(actual.isEmpty() && mayGiveUp) {
					gaveUp++;
				} else {
					assertThat(actual).as("seed " + seed).isPresent();
					assertThat(expected).as("seed " + seed).isPresent();
					assertThat(KernelParity.same(actual.get(), expected.get())).as("seed " + seed + ": expected " + KernelParity.describe(expected) + ", got " + KernelParity.describe(actual)).isTrue();
				}
			}
		} finally {
			device.close();
		}
		return gaveUp;
	}

	@Test
	void winnerIsTheWinnerOfTheCpuEngine() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().build();
		compare(device, 1, 20, 8, 10, false);
		assertThat(device.getSearches()).isEqualTo(20);
	}

	@Test
	void winnerWithASmallWindowAndPausing() throws PackagerInterruptedException {
		// a window of three items, paused after seven nodes: the state of an item goes to the host and back many times
		TornadoBruteForceAccelerator device = device().withWindow(3).withNodeBudget(7).build();
		compare(device, 31, 10, 8, 9, false);
		assertThat(device.getRounds()).isGreaterThan(1000);
	}

	@Test
	void winnerWithABound() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().build();
		try {
			for (long seed = 51; seed < 61; seed++) {
				Instance instance = KernelParity.random(seed, 8, 9, 4, 9);
				FlatProblem problem = KernelParity.problem(instance);
				// a bound in the middle of the volumes: some permutations are skipped, others not
				long bound = cpu.search(problem, 0L, () -> false).get().getLoadVolume() / 2;
				Optional<AcceleratorWinner> expected = cpu.search(problem, bound, () -> false);
				Optional<AcceleratorWinner> actual = device.search(problem, bound, () -> false);
				assertThat(actual).as("seed " + seed).isPresent();
				assertThat(KernelParity.same(actual.get(), expected.get())).as("seed " + seed).isTrue();
			}
		} finally {
			device.close();
		}
	}

	@Test
	void givesUpWhenTheFreePointsDoNotFit() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().withMaxPoints(12).build();
		int gaveUp = compare(device, 1, 20, 8, 11, true);
		assertThat(gaveUp).isGreaterThan(0);
		assertThat(device.getLastFailure()).isNull();
	}

	@Test
	void interruptIsHonoured() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().build();
		try {
			FlatProblem problem = KernelParity.problem(KernelParity.random(1, 9, 9, 4, 9));
			org.assertj.core.api.Assertions.assertThatThrownBy(() -> device.search(problem, 0L, () -> true)).isInstanceOf(PackagerInterruptedException.class);
			assertThat(device.getLastFailure()).isNull();
			// usable afterwards
			assertThat(device.search(problem, 0L, () -> false)).isPresent();
		} finally {
			device.close();
		}
	}

	@Test
	void loopKernelWinnerIsTheWinnerOfTheCpuEngine() throws PackagerInterruptedException {
		// the orders of an item searched one at a time, as before the tree
		TornadoBruteForceAccelerator device = device().withTree(false).build();
		compare(device, 1, 12, 8, 10, false);
	}

	@Test
	void anyBestHasTheValueOfTheCpuEngine() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().build();
		FlatEngine tree = FlatEngine.newBuilder().withMinBoxes(1).withTree(true).build();
		try {
			for (long seed = 71; seed < 91; seed++) {
				Instance instance = KernelParity.random(seed, 8, 10, 4, 9);
				FlatProblem problem = KernelParity.problem(instance, true);
				Optional<AcceleratorWinner> expected = cpu.search(KernelParity.problem(instance, false), 0L, () -> false);
				Optional<AcceleratorWinner> actual = device.search(problem, 0L, () -> false);
				assertThat(device.getLastFailure()).as("seed " + seed).isNull();
				assertThat(actual).as("seed " + seed).isPresent();
				// any arrangement of the best value: the value of the loop's winner, and of the CPU tree
				assertThat(KernelParity.sameValue(actual.get(), expected.get())).as("seed " + seed + ": expected " + KernelParity.describe(expected) + ", got " + KernelParity.describe(actual)).isTrue();
				Optional<AcceleratorWinner> cpuTree = tree.search(problem, 0L, () -> false);
				assertThat(KernelParity.sameValue(actual.get(), cpuTree.get())).as("seed " + seed).isTrue();
			}
		} finally {
			device.close();
			tree.close();
		}
	}

	@Test
	void closedAcceleratorGivesUp() throws PackagerInterruptedException {
		TornadoBruteForceAccelerator device = device().build();
		FlatProblem problem = KernelParity.problem(KernelParity.random(2, 8, 8, 4, 9));
		assertThat(device.search(problem, 0L, () -> false)).isPresent();
		device.close();
		assertThat(device.supports(problem)).isFalse();
		assertThat(device.search(problem, 0L, () -> false)).isEmpty();
	}
}
