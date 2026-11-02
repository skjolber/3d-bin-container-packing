package com.github.skjolber.packing.tornado;

import java.util.List;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.common.TornadoDevice;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;
import uk.ac.manchester.tornado.api.types.arrays.IntArray;
import uk.ac.manchester.tornado.api.types.arrays.LongArray;

/**
 * Smoke test of a TornadoVM runtime, run by hand (see the README): chooses a device, runs a vector add, and a kernel with data-dependent loops, long arithmetic and
 * scratch memory per work item (like the search of the flat engine), and compares both with the same method run on the CPU. Not a unit test.
 */
public class GpuSmoke {

	private static final int SCRATCH = 32;

	public static void vectorAdd(IntArray a, IntArray b, IntArray c) {
		for (@Parallel int i = 0; i < c.getSize(); i++) {
			c.set(i, a.get(i) + b.get(i));
		}
	}

	public static void probe(IntArray scratch, LongArray out, IntArray steps, int n) {
		for (@Parallel int i = 0; i < n; i++) {
			int base = i * SCRATCH;
			for (int k = 0; k < SCRATCH; k++) {
				scratch.set(base + k, 0);
			}
			long x = 27L + i * 7919L;
			int count = 0;
			while (x != 1L && count < 4000) {
				int slot = base + (count & (SCRATCH - 1));
				scratch.set(slot, scratch.get(slot) + (int)(x & 0xFF));
				if ((x & 1L) == 0L) {
					x = x >> 1;
				} else {
					x = 3L * x + 1L;
				}
				count++;
			}
			long sum = 0L;
			for (int k = 0; k < SCRATCH; k++) {
				sum += scratch.get(base + k) * (long)(k + 1);
			}
			out.set(i, sum ^ x);
			steps.set(i, count);
		}
	}

	public static void main(String[] args) throws Exception {
		List<DeviceInfo> devices = TornadoDevices.discover();
		DeviceInfo info = DeviceSelector.newBuilder().build().select(devices).orElseThrow(() -> new IllegalStateException("No device found: " + devices));
		System.out.println("Device: " + info);
		TornadoDevice device = TornadoExecutionPlan.getDevice(info.backendIndex(), info.deviceIndex());

		vectorAdd(device);
		probe(device, 1 << 16);
	}

	private static void vectorAdd(TornadoDevice device) throws Exception {
		int n = 1 << 20;
		IntArray a = new IntArray(n);
		IntArray b = new IntArray(n);
		IntArray c = new IntArray(n);
		for (int i = 0; i < n; i++) {
			a.set(i, i);
			b.set(i, 3 * i + 1);
		}
		TaskGraph graph = new TaskGraph("vectorAdd").transferToDevice(DataTransferMode.FIRST_EXECUTION, a, b).task("add", GpuSmoke::vectorAdd, a, b, c)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, c);
		ImmutableTaskGraph immutable = graph.snapshot();
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(device);
			long start = System.nanoTime();
			plan.execute();
			long first = System.nanoTime() - start;
			for (int i = 0; i < n; i++) {
				if(c.get(i) != 4 * i + 1) {
					throw new IllegalStateException("vectorAdd: wrong value at " + i + ": " + c.get(i));
				}
			}
			start = System.nanoTime();
			plan.execute();
			long second = System.nanoTime() - start;
			System.out.println("vectorAdd OK: " + n + " items, first run (with JIT) " + first / 1_000_000 + " ms, second run " + second / 1_000_000.0 + " ms");
		}
	}

	private static void probe(TornadoDevice device, int n) throws Exception {
		IntArray scratch = new IntArray(n * SCRATCH);
		LongArray out = new LongArray(n);
		IntArray steps = new IntArray(n);

		IntArray expectedScratch = new IntArray(n * SCRATCH);
		LongArray expectedOut = new LongArray(n);
		IntArray expectedSteps = new IntArray(n);
		long start = System.nanoTime();
		probe(expectedScratch, expectedOut, expectedSteps, n);
		long cpu = System.nanoTime() - start;

		TaskGraph graph = new TaskGraph("probe").transferToDevice(DataTransferMode.FIRST_EXECUTION, scratch, out, steps).task("probe", GpuSmoke::probe, scratch, out, steps, n)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, out, steps);
		ImmutableTaskGraph immutable = graph.snapshot();
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(device);
			start = System.nanoTime();
			plan.execute();
			long first = System.nanoTime() - start;
			check(out, steps, expectedOut, expectedSteps, n);
			long best = Long.MAX_VALUE;
			for (int run = 0; run < 5; run++) {
				start = System.nanoTime();
				plan.execute();
				best = Math.min(best, System.nanoTime() - start);
			}
			check(out, steps, expectedOut, expectedSteps, n);
			long totalSteps = 0;
			int maxSteps = 0;
			for (int i = 0; i < n; i++) {
				totalSteps += steps.get(i);
				maxSteps = Math.max(maxSteps, steps.get(i));
			}
			System.out.println("probe OK: " + n + " items, " + totalSteps + " loop iterations (max per item " + maxSteps + "), first run (with JIT) " + first / 1_000_000 + " ms, best of 5 "
					+ best / 1_000_000.0 + " ms, one CPU thread " + cpu / 1_000_000.0 + " ms");
		}
	}

	private static void check(LongArray out, IntArray steps, LongArray expectedOut, IntArray expectedSteps, int n) {
		for (int i = 0; i < n; i++) {
			if(out.get(i) != expectedOut.get(i) || steps.get(i) != expectedSteps.get(i)) {
				throw new IllegalStateException("probe: mismatch at " + i + ": device " + out.get(i) + "/" + steps.get(i) + ", expected " + expectedOut.get(i) + "/" + expectedSteps.get(i));
			}
		}
	}
}
