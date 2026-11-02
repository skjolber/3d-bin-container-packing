package com.github.skjolber.packing.tornado;

import java.util.Arrays;

import com.github.skjolber.packing.accelerator.FlatLayout;
import com.github.skjolber.packing.accelerator.FlatPermutation;
import com.github.skjolber.packing.accelerator.FlatPoints;
import com.github.skjolber.packing.accelerator.FlatProblem;
import com.github.skjolber.packing.accelerator.FlatSearch;
import com.github.skjolber.packing.accelerator.FlatSubtree;

import uk.ac.manchester.tornado.api.ImmutableTaskGraph;
import uk.ac.manchester.tornado.api.TaskGraph;
import uk.ac.manchester.tornado.api.TornadoExecutionPlan;
import uk.ac.manchester.tornado.api.annotations.Parallel;
import uk.ac.manchester.tornado.api.enums.DataTransferMode;

/**
 * Run by hand: finds which part of the kernel does not run on the device. One kernel, with a stage in the parameters: 0 resets the scratch, 1 steps the permutation, 2
 * searches one permutation, 3 runs the subtree. Each stage is compared with the same method on the host.
 * <p>
 * Arguments: seed, boxes, last stage to run.
 */
public class StageProbe {

	private static long checksum(int[] s, int frame) {
		int count = s[frame];
		long sum = count;
		for (int i = 0; i < count * 7; i++) {
			sum = sum * 31 + s[frame + 1 + i];
		}
		return sum;
	}

	/** Places the first two boxes of the permutation by hand, as far as the step says, recording what happened. */
	private static void places(FlatLayout layout, int[] problem, long[] problemLongs, int[] prefixes, int slot, int[] s, int base, long[] debug, int step) {
		int n = problem[FlatProblem.N];
		int types = problem[FlatProblem.TYPES];
		int rotationOffsets = FlatProblem.rotationOffsets(types);
		int rotationValues = FlatProblem.rotationValues(types);
		int d = slot * DEBUG;
		for (int i = 0; i < n; i++) {
			s[base + layout.perm + i] = prefixes[slot * n + i];
		}
		FlatPoints.clear(layout, s, base, problem[FlatProblem.CX], problem[FlatProblem.CY], problem[FlatProblem.CZ]);
		debug[d] = 1L;

		int type0 = s[base + layout.perm];
		int rotation0 = rotationValues + 3 * problem[rotationOffsets + type0];
		int dx0 = problem[rotation0];
		int dy0 = problem[rotation0 + 1];
		int dz0 = problem[rotation0 + 2];
		FlatPoints.setPlacement(layout, s, base, 0, 0, 0, 0, dx0 - 1, dy0 - 1, dz0 - 1);
		int count = FlatPoints.add(layout, s, base, 0, 0, 0, 0L, 0L);
		debug[d + 1] = count;
		debug[d + 2] = checksum(s, base + layout.frameOffset(1));
		if(step >= 2) {
			int type1 = s[base + layout.perm + 1];
			int rotation1 = rotationValues + 3 * problem[rotationOffsets + type1];
			int dx1 = problem[rotation1];
			int dy1 = problem[rotation1 + 1];
			int dz1 = problem[rotation1 + 2];
			int frame = base + layout.frameOffset(1);
			int pointIndex = FlatSearch.bestPoint(s, frame, dx1, dy1, dz1);
			debug[d + 3] = pointIndex;
			if(step >= 3 && pointIndex != -1) {
				int point = frame + 1 + pointIndex * 7;
				int x = s[point];
				int y = s[point + 1];
				int z = s[point + 2];
				FlatPoints.setPlacement(layout, s, base, 1, x, y, z, x + dx1 - 1, y + dy1 - 1, z + dz1 - 1);
				long minArea = step >= 4 ? problemLongs[FlatProblem.minAreas(types) + type1] : 0L;
				long minVolume = step >= 4 ? problemLongs[type1] : 0L;
				int count2 = FlatPoints.add(layout, s, base, 1, pointIndex, 1, minArea, minVolume);
				debug[d + 4] = count2;
				debug[d + 5] = checksum(s, base + layout.frameOffset(2));
			}
		}
		debug[d + 6] = 99L;
	}

	private static final int DEBUG = 256;

	/** A copy of FlatSearch.searchPermutation which stops after 50 iterations and records each of them in the debug array. */
	private static int traced(FlatLayout layout, int[] problem, long[] problemLongs, int[] s, long[] sl, int base, int longBase, int maxPackableCount, long[] debug, int d, int cap,
			int phase) {
		final int types = problem[FlatProblem.TYPES];
		final int n = problem[FlatProblem.N];
		final int rotationOffsets = FlatProblem.rotationOffsets(types);
		final int rotationValues = FlatProblem.rotationValues(types);
		final int permBase = base + layout.perm;
		final int rotationBase = base + layout.rotation;
		final int placedRotationBase = base + layout.placedRotation;
		final int bestRotationBase = base + layout.bestRotation;
		final int control = base + layout.control;
		final int minAreaBase = longBase + layout.minArea;
		final int minVolumeBase = longBase + layout.minVolume;
		final int placedVolumeBase = longBase + layout.placedVolume;
		final int length = maxPackableCount;

		sl[longBase + layout.bestVolume] = 0L;
		s[control + FlatLayout.C_BEST_COUNT] = 0;

		long minArea = Long.MAX_VALUE;
		for (int i = length - 1; i >= 0; i--) {
			long area = problemLongs[FlatProblem.minAreas(types) + s[permBase + i]];
			if(area < minArea) {
				minArea = area;
			}
			sl[minAreaBase + i] = minArea;
		}
		long minVolume = Long.MAX_VALUE;
		for (int i = n - 1; i >= 0; i--) {
			long volume = problemLongs[s[permBase + i]];
			if(volume < minVolume) {
				minVolume = volume;
			}
			sl[minVolumeBase + i] = minVolume;
		}
		debug[d + 0] = 1L;
		debug[d + 1] = sl[minAreaBase];
		debug[d + 2] = sl[minVolumeBase];

		FlatPoints.clear(layout, s, base, problem[FlatProblem.CX], problem[FlatProblem.CY], problem[FlatProblem.CZ]);
		if(sl[minAreaBase] != 0L || sl[minVolumeBase] != 0L) {
			FlatPoints.filterMinimums(layout, s, base, 0, sl[minAreaBase], sl[minVolumeBase]);
		}
		debug[d + 3] = s[base + layout.frameOffset(0)];
		sl[placedVolumeBase] = 0L;

		int bestCount = 0;
		int iteration = 0;
		int level = 0;
		boolean descend = true;
		boolean overflow = false;
		while (level >= 0 && !overflow && iteration < cap) {
			boolean unwind;
			int tracedPoint = -2;
			if(descend && level == length) {
				if(level > bestCount) {
					bestCount = level;
					sl[longBase + layout.bestVolume] = sl[longBase + layout.placedVolume + level];
				}
				unwind = true;
			} else if(!descend && bestCount == length) {
				unwind = true;
			} else {
				if(descend) {
					s[rotationBase + level] = 0;
				}
				final int type = s[permBase + level];
				final int firstRotation = problem[rotationOffsets + type];
				final int rotationCount = problem[rotationOffsets + type + 1] - firstRotation;
				final int frame = base + layout.frameOffset(level);

				int rotationIndex = s[rotationBase + level];
				int pointIndex = -1;
				int dx = 0;
				int dy = 0;
				int dz = 0;
				while (pointIndex == -1 && rotationIndex < rotationCount) {
					int rotation = rotationValues + 3 * (firstRotation + rotationIndex);
					dx = problem[rotation];
					dy = problem[rotation + 1];
					dz = problem[rotation + 2];
					rotationIndex++;
					pointIndex = FlatSearch.bestPoint(s, frame, dx, dy, dz);
				}
				s[rotationBase + level] = rotationIndex;
				if(iteration == 1 && phase == 1) {
					pointIndex = -1;
				}
				tracedPoint = pointIndex;

				if(pointIndex != -1) {
					int point = frame + 1 + pointIndex * 7;
					int x = s[point];
					int y = s[point + 1];
					int z = s[point + 2];
					FlatPoints.setPlacement(layout, s, base, level, x, y, z, x + dx - 1, y + dy - 1, z + dz - 1);
					int count = FlatPoints.add(layout, s, base, level, pointIndex, level, sl[minAreaBase + level], sl[minVolumeBase + level]);
					if(count < 0 || (iteration == 1 && phase == 2)) {
						overflow = true;
					} else {
						int nextLevel = level + 1;
						if(nextLevel < length) {
							long nextArea = sl[minAreaBase + nextLevel];
							long nextVolume = sl[minVolumeBase + nextLevel];
							if(nextArea != sl[minAreaBase + level] || nextVolume != sl[minVolumeBase + level]) {
								FlatPoints.filterMinimums(layout, s, base, nextLevel, nextArea, nextVolume);
							}
						}
						s[placedRotationBase + level] = rotationIndex - 1;
						sl[placedVolumeBase + nextLevel] = sl[placedVolumeBase + level] + problemLongs[type];
						level = nextLevel;
						descend = true;
					}
					unwind = false;
				} else {
					if(level > bestCount) {
						bestCount = level;
						sl[longBase + layout.bestVolume] = sl[longBase + layout.placedVolume + level];
					}
					unwind = true;
				}
			}
			debug[d + 8 + iteration] = (level + 1) | (descend ? 1L << 8 : 0L) | (unwind ? 1L << 9 : 0L) | ((long)(tracedPoint + 2) << 16) | ((long)bestCount << 32);
			if(unwind) {
				level--;
				descend = false;
			}
			iteration++;
		}
		debug[d + 4] = iteration;
		debug[d + 5] = bestCount;
		debug[d + 6] = level;
		debug[d + 7] = 99L;
		return bestCount;
	}

	public static void stage(FlatLayout layout, int[] problem, long[] problemLongs, int[] active, int[] prefixes, int[] parameters, long[] longParameters, int[] s, long[] sl, int[] out,
			long[] outLongs, long[] debug) {
		for (@Parallel int slot = 0; slot < active.length; slot++) {
			if(active[slot] != 0) {
				int n = problem[FlatProblem.N];
				int base = slot * layout.size;
				int longBase = slot * layout.longSize;
				int outBase = slot * FlatSubtree.outInts(n);
				int stage = parameters[2];
				layout.reset(s, base);
				if(stage == 0) {
					out[outBase + FlatSubtree.OUT_COUNT] = 7 + slot;
				} else if(stage == 1) {
					int cursor = outBase + FlatSubtree.OUT_CURSOR;
					for (int i = 0; i < n; i++) {
						out[cursor + i] = prefixes[slot * n + i];
					}
					out[outBase + FlatSubtree.OUT_COUNT] = FlatPermutation.next(out, cursor, n, parameters[3]);
				} else if(stage == 2) {
					outLongs[slot * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_NODES] = 11L;
					for (int i = 0; i < n; i++) {
						s[base + layout.perm + i] = prefixes[slot * n + i];
					}
					s[base + layout.control + FlatLayout.C_NODES] = 0;
					outLongs[slot * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_NODES] = 22L;
					outLongs[slot * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_VOLUME] = 1000L * n + s[base + layout.perm + 1];
					int size = FlatSearch.searchPermutation(layout, problem, problemLongs, s, sl, base, longBase, parameters[3]);
					outLongs[slot * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_WEIGHT] = 1000L * size + s[base + layout.control + FlatLayout.C_NODES];
					outLongs[slot * FlatSubtree.OUT_LONGS + FlatSubtree.OUT_NODES] = 33L;
					out[outBase + FlatSubtree.OUT_COUNT] = size;
					out[outBase + FlatSubtree.OUT_STATUS] = s[base + layout.control + FlatLayout.C_NODES];
				} else if(stage == 4) {
					places(layout, problem, problemLongs, prefixes, slot, s, base, debug, parameters[3]);
				} else if(stage == 5) {
					for (int i = 0; i < n; i++) {
						s[base + layout.perm + i] = prefixes[slot * n + i];
					}
					traced(layout, problem, problemLongs, s, sl, base, longBase, parameters[1], debug, slot * DEBUG, parameters[3], parameters[0]);
				} else {
					FlatSubtree.run(layout, problem, problemLongs, parameters[0], prefixes, slot, s, sl, base, longBase, out, outBase, outLongs, slot * FlatSubtree.OUT_LONGS, longParameters[0],
							parameters[1]);
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		long seed = args.length > 0 ? Long.parseLong(args[0]) : 1;
		int boxes = args.length > 1 ? Integer.parseInt(args[1]) : 8;
		int lastStage = args.length > 2 ? Integer.parseInt(args[2]) : 3;
		int firstStage = args.length > 3 ? Integer.parseInt(args[3]) : 0;
		int mpc = args.length > 4 ? Integer.parseInt(args[4]) : 2;
		int phase = args.length > 5 ? Integer.parseInt(args[5]) : 0;

		FlatProblem problem = KernelParity.problem(KernelParity.random(seed, boxes, boxes, 4, 9));
		int n = problem.n();
		FlatLayout layout = new FlatLayout(n, 128);
		int[] ints = problem.ints();
		long[] longs = problem.longs();
		int depth = FlatPermutation.depth(ints, 4, 65536);
		int[] prefixes = FlatPermutation.prefixes(ints, depth, 65536);
		int items = prefixes.length / n;
		int slots = Math.min(4, items);
		System.out.println("n " + n + ", depth " + depth + ", items " + items + ", slots " + slots + ", scratch ints " + layout.size);

		int outInts = FlatSubtree.outInts(n);
		int[] active = new int[slots];
		Arrays.fill(active, 1);
		int[] windowPrefixes = Arrays.copyOf(prefixes, slots * n);
		int[] parameters = new int[4];
		long[] longParameters = new long[1];
		int[] s = new int[slots * layout.size];
		long[] sl = new long[slots * layout.longSize];
		int[] out = new int[slots * outInts];
		long[] outLongs = new long[slots * FlatSubtree.OUT_LONGS];
		long[] debug = new long[slots * DEBUG];
		parameters[0] = depth;
		parameters[1] = 1;
		parameters[3] = 2;

		TaskGraph graph = new TaskGraph("probe").transferToDevice(DataTransferMode.FIRST_EXECUTION, layout, ints, longs, s, sl)
				.transferToDevice(DataTransferMode.EVERY_EXECUTION, active, windowPrefixes, parameters, longParameters, out, outLongs, debug)
				.task("stage", StageProbe::stage, layout, ints, longs, active, windowPrefixes, parameters, longParameters, s, sl, out, outLongs, debug)
				.transferToHost(DataTransferMode.EVERY_EXECUTION, out, outLongs, s, sl, debug);
		ImmutableTaskGraph immutable = graph.snapshot();
		try (TornadoExecutionPlan plan = new TornadoExecutionPlan(immutable)) {
			plan.withDevice(TornadoExecutionPlan.getDevice(0, 0));
			for (int stage = firstStage; stage <= lastStage; stage++) {
				int firstVariant = stage == 2 || stage == 4 || stage == 5 ? 1 : 0;
				int variants = stage == 2 ? n + 1 : stage == 4 ? 5 : stage == 5 ? n + 1 : 1;
				for (int variant = firstVariant; variant < variants; variant++) {
					parameters[2] = stage;
					parameters[3] = stage == 2 || stage == 4 || stage == 5 ? variant : 2;
					if(stage == 5) {
						// the maximum packable count in the parameters, the iteration cap as the variant, and the phase in which the second iteration is cut short
						parameters[1] = mpc;
						parameters[0] = phase;
					}
					Arrays.fill(out, 0);
					Arrays.fill(outLongs, 0);
					Arrays.fill(debug, 0);

					int[] expectedOut = new int[out.length];
					long[] expectedOutLongs = new long[outLongs.length];
					long[] expectedDebug = new long[debug.length];
					int[] expectedS = new int[s.length];
					long[] expectedSl = new long[sl.length];
					stage(layout, ints, longs, active, windowPrefixes, parameters, longParameters, expectedS, expectedSl, expectedOut, expectedOutLongs, expectedDebug);

					long start = System.nanoTime();
					System.out.println("stage " + stage + "/" + parameters[3] + " starting");
					plan.execute();
					long time = System.nanoTime() - start;
					System.out.println("stage " + stage + "/" + parameters[3] + ": " + time / 1_000_000 + " ms, ints equal " + Arrays.equals(out, expectedOut) + ", longs equal "
							+ Arrays.equals(outLongs, expectedOutLongs) + ", slot 0 longs device " + Arrays.toString(Arrays.copyOf(outLongs, FlatSubtree.OUT_LONGS)) + " host "
							+ Arrays.toString(Arrays.copyOf(expectedOutLongs, FlatSubtree.OUT_LONGS)));
					if(stage == 4 || stage == 5) {
						System.out.println("  debug equal " + Arrays.equals(debug, expectedDebug));
						for (int slot = 0; slot < slots; slot++) {
							int length = stage == 5 ? DEBUG : 7;
							long[] device = Arrays.copyOfRange(debug, slot * DEBUG, slot * DEBUG + length);
							long[] host = Arrays.copyOfRange(expectedDebug, slot * DEBUG, slot * DEBUG + length);
							if(!Arrays.equals(device, host)) {
								System.out.println("  slot " + slot + " device " + Arrays.toString(device) + "\n         host   " + Arrays.toString(host));
							}
						}
					}
					if(!Arrays.equals(out, expectedOut)) {
						System.out.println("  expected " + Arrays.toString(Arrays.copyOf(expectedOut, Math.min(expectedOut.length, outInts))));
						System.out.println("  actual   " + Arrays.toString(Arrays.copyOf(out, Math.min(out.length, outInts))));
					}
					int differing = 0;
					StringBuilder first = new StringBuilder();
					for (int i = 0; i < layout.size; i++) {
						if(s[i] != expectedS[i]) {
							if(differing < 12) {
								first.append(" [").append(i).append("] ").append(expectedS[i]).append("->").append(s[i]);
							}
							differing++;
						}
					}
					System.out.println("  scratch ints of slot 0 differing: " + differing + first);
				}
			}
		}
		System.exit(0);
	}
}
