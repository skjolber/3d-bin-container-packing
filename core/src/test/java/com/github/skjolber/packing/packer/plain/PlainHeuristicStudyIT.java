package com.github.skjolber.packing.packer.plain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.PlacementComparator;
import com.github.skjolber.packing.packer.Version4Orders;
import com.github.skjolber.packing.packer.Version4Orders.BoxSpec;
import com.github.skjolber.packing.packer.Version4Orders.ContainerSpec;
import com.github.skjolber.packing.packer.Version4Orders.Scenario;
import com.github.skjolber.packing.packer.Version4Orders.Summary;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

/**
 * Measurement study (not a regression test, never runs by default; run it with {@code -Dtest=PlainHeuristicStudyIT}) of the plain
 * packager's placement ranking, on random orders ({@linkplain Version4Orders#random(long)}):
 * <ul>
 * <li>A: 5.x plain, the default ranking (higher volume, heavier, smaller area, lower z)</li>
 * <li>B: 5.x plain with a comparator which emulates that of 4.x: support ratio first (the floor counts as full support), then higher
 * z, then larger footprint area</li>
 * <li>C: 4.x plain (the relocated classes of {@code legacy/v4})</li>
 * </ul>
 * and, for context, the largest area fit first packagers (normal and fast) of 5.x against 4.x. Results are printed to the test output.
 * <p>
 * System properties: {@code study.seeds} (default 10000), {@code study.from} (default 0) and {@code study.insertionOrder} (default true).
 * 5.x puts the placements of a result in insertion order (by height) unless told not to, which 4.x does not: with {@code true}
 * the summaries of the two versions differ in the order of the lines, so the report also compares the placements regardless of order;
 * with {@code false} the lines are in the order the boxes were placed, as in 4.x.
 */
public class PlainHeuristicStudyIT {

	private static final int FROM = Integer.getInteger("study.from", 0);
	private static final int SEEDS = Integer.getInteger("study.seeds", 10_000);
	private static final long INTERRUPT_DURATION = 60_000L;
	/** 5.x puts the placements of a result in a possible insertion order (by height) unless this is false; 4.x leaves them in the order they were placed */
	private static final boolean INSERTION_ORDER = Boolean.parseBoolean(System.getProperty("study.insertionOrder", "true"));

	// configurations
	private static final int A = 0;
	private static final int B = 1;
	private static final int C = 2;
	private static final int L5 = 3;
	private static final int L4 = 4;
	private static final int F5 = 5;
	private static final int F4 = 6;

	private static final String[] NAMES = {
			"A  5.x plain, default ranking",
			"B  5.x plain, pre-4.2.5 ranking (support, higher z, larger area; the inverted z tiebreak fixed in 4.x by 775cdca6)",
			"C  4.x plain",
			"L5 5.x laff",
			"L4 4.x laff",
			"F5 5.x fast laff",
			"F4 4.x fast laff"
	};

	/**
	 * The ranking of the plain packager of 4.x: a higher support ratio (per mille, a box on the floor is fully supported), then a
	 * higher z, then a larger footprint area. Positive if {@code a} is preferred.
	 */
	private static List<String> canonical(Summary summary) {
		List<String> result = new ArrayList<>(summary.lines().size());
		List<String> placements = new ArrayList<>();
		for (String line : summary.lines()) {
			if(line.startsWith("container")) {
				java.util.Collections.sort(placements);
				result.addAll(placements);
				placements.clear();
				result.add(line);
			} else {
				placements.add(line);
			}
		}
		java.util.Collections.sort(placements);
		result.addAll(placements);
		return result;
	}

	/**
	 * The plain ranking of 4.x up to and including 4.2.4: support ratio, then HIGHER z (an inverted tiebreak, fixed in
	 * 4.x by commit 775cdca6 for 4.2.5), then larger footprint. Kept as the historical baseline of this study; against a
	 * fixed 4.x (-Dv4.version=4.2.5...) config B no longer matches config C.
	 */
	public static final class Version4HeuristicComparator implements PlacementComparator {

		@Override
		public boolean usesSupportedArea() {
			return true;
		}

		@Override
		public int compare(Placement a, Placement b) {
			long ratioA = a.getSupportedArea() * 1000 / a.getStackValue().getArea();
			long ratioB = b.getSupportedArea() * 1000 / b.getStackValue().getArea();
			if(ratioA != ratioB) {
				return Long.compare(ratioA, ratioB);
			}
			int z = Integer.compare(a.getAbsoluteZ(), b.getAbsoluteZ());
			if(z != 0) {
				return z;
			}
			return Long.compare(a.getStackValue().getArea(), b.getStackValue().getArea());
		}
	}

	/** Counts what the wrapped comparator sees: supported area (as set by the placement controls) by height. */
	public static final class RecordingComparator implements PlacementComparator {

		private final PlacementComparator delegate;

		long floorFull;
		long floorOther;
		long airFull;
		long airPartial;
		long airNone;
		long nonZeroSupport;
		long candidates;

		public RecordingComparator(PlacementComparator delegate) {
			this.delegate = delegate;
		}

		@Override
		public boolean usesSupportedArea() {
			return delegate.usesSupportedArea();
		}

		private void record(Placement p) {
			candidates++;
			long area = p.getStackValue().getArea();
			long supported = p.getSupportedArea();
			if(supported != 0) {
				nonZeroSupport++;
			}
			if(p.getAbsoluteZ() == 0) {
				if(supported == area) {
					floorFull++;
				} else {
					floorOther++;
				}
			} else if(supported == area) {
				airFull++;
			} else if(supported == 0) {
				airNone++;
			} else {
				airPartial++;
			}
		}

		@Override
		public int compare(Placement a, Placement b) {
			record(a);
			record(b);
			return delegate.compare(a, b);
		}

		@Override
		public String toString() {
			return "candidates seen " + candidates + ", with supported area > 0: " + nonZeroSupport + "; z=0 fully supported " + floorFull + ", z=0 other " + floorOther
					+ "; z>0 fully supported " + airFull + ", partly supported " + airPartial + ", unsupported " + airNone;
		}
	}

	/** The result of one packing: the summary, how many placements are above the floor, and how many of those rest on less than their full footprint, or on nothing */
	private record Outcome(Summary summary, int placements, int elevated, int partlySupported, int unsupported, boolean valid) {

		double elevatedShare() {
			return placements == 0 ? 0 : (double)elevated / placements;
		}
	}

	private static final class Stats {

		final String name;
		long nanos;
		int runs;
		int errors;
		int successes;
		int invalid;
		long containers;
		double shareSum;
		long elevated;
		long partly;
		long unsupported;
		long placements;
		final List<String> errorMessages = new ArrayList<>();
		final List<Long> invalidSeeds = new ArrayList<>();

		Stats(String name) {
			this.name = name;
		}
	}

	private static final class Example {

		final long seed;
		final Scenario scenario;
		final Outcome first;
		final Outcome second;

		Example(long seed, Scenario scenario, Outcome first, Outcome second) {
			this.seed = seed;
			this.scenario = scenario;
			this.first = first;
			this.second = second;
		}

		// smallest layouts first, those where both have a layout before those where one has none
		int size() {
			int a = first.summary().lines().size();
			int b = second.summary().lines().size();
			return a + b + (a == 0 || b == 0 ? 1000 : 0);
		}
	}

	private static final int EXAMPLES = 2;

	private static void keep(List<Example> examples, Example example) {
		examples.add(example);
		if(examples.size() > EXAMPLES) {
			int largest = 0;
			for (int i = 1; i < examples.size(); i++) {
				if(examples.get(i).size() >= examples.get(largest).size()) {
					largest = i;
				}
			}
			examples.remove(largest);
		}
	}

	private static final class Pair {

		final String name;
		final int first;
		final int second;

		int compared;
		int skipped;
		int wins;
		int losses;
		int ties;
		int identical;
		int tiesBothSuccess;
		int tiesBothFailed;
		int identicalBothSuccess;
		int differentLayoutBothSuccess;
		int onlyReorderedBothSuccess;
		int divergeAtFirstPlacement;
		int notIdenticalBothSuccess;
		final List<Example> winExamples = new ArrayList<>();
		final List<Example> lossExamples = new ArrayList<>();

		Pair(String name, int first, int second) {
			this.name = name;
			this.first = first;
			this.second = second;
		}

		void add(long seed, Scenario scenario, Outcome x, Outcome y) {
			compared++;
			int c = x.summary().compareTo(y.summary());
			boolean bothSuccess = x.summary().success() && y.summary().success();
			boolean same = x.summary().equals(y.summary());
			if(c > 0) {
				wins++;
				keep(winExamples, new Example(seed, scenario, x, y));
			} else if(c < 0) {
				losses++;
				keep(lossExamples, new Example(seed, scenario, x, y));
			} else {
				ties++;
				if(bothSuccess) {
					tiesBothSuccess++;
				} else {
					tiesBothFailed++;
				}
			}
			if(same) {
				identical++;
				if(bothSuccess) {
					identicalBothSuccess++;
				}
			} else if(bothSuccess) {
				notIdenticalBothSuccess++;
				List<String> lx = x.summary().lines();
				List<String> ly = y.summary().lines();
				if(canonical(x.summary()).equals(canonical(y.summary()))) {
					onlyReorderedBothSuccess++;
				} else {
					differentLayoutBothSuccess++;
					if(lx.size() > 1 && ly.size() > 1 && !lx.get(1).equals(ly.get(1))) {
						divergeAtFirstPlacement++;
					}
					classifyDivergence(new Example(seed, scenario, x, y), lx, ly);
				}
			}
		}

		// the first line which differs, of the orders where both packed all boxes but the placements differ (not only their order)
		int divergeContainers;
		int divergeSameBoxSameZAndFootprint;
		int divergeSameBoxOther;
		int divergeOtherBox;
		int divergeOtherBoxSameVolumeAndWeight;
		final List<Example> otherBoxExamples = new ArrayList<>();
		final List<Example> sameBoxOtherExamples = new ArrayList<>();

		private static boolean sameVolumeAndWeight(Scenario scenario, String idX, String idY) {
			BoxSpec x = null;
			BoxSpec y = null;
			for (BoxSpec box : scenario.boxes()) {
				if(box.id().equals(idX)) {
					x = box;
				}
				if(box.id().equals(idY)) {
					y = box;
				}
			}
			return x != null && y != null && (long)x.dx() * x.dy() * x.dz() == (long)y.dx() * y.dy() * y.dz() && x.weight() == y.weight();
		}

		private void classifyDivergence(Example example, List<String> lx, List<String> ly) {
			int n = Math.min(lx.size(), ly.size());
			for (int i = 0; i < n; i++) {
				String px = lx.get(i);
				String py = ly.get(i);
				if(px.equals(py)) {
					continue;
				}
				if(px.startsWith("container") || py.startsWith("container")) {
					divergeContainers++;
					return;
				}
				String[] sx = px.split(" ");
				String[] sy = py.split(" ");
				if(!sx[0].equals(sy[0])) {
					if(sameVolumeAndWeight(example.scenario, sx[0], sy[0])) {
						divergeOtherBoxSameVolumeAndWeight++;
					} else {
						divergeOtherBox++;
						keep(otherBoxExamples, example);
					}
				} else if(zOf(sx[1]) == zOf(sy[1]) && footprintOf(sx[2]) == footprintOf(sy[2])) {
					divergeSameBoxSameZAndFootprint++;
				} else {
					divergeSameBoxOther++;
					keep(sameBoxOtherExamples, example);
				}
				return;
			}
			// one is a prefix of the other
			divergeContainers++;
		}

		private static int zOf(String position) {
			return Integer.parseInt(position.substring(position.lastIndexOf(',') + 1));
		}

		private static int footprintOf(String size) {
			String[] d = size.split("x");
			return Integer.parseInt(d[0]) * Integer.parseInt(d[1]);
		}

		String report() {
			StringBuilder b = new StringBuilder();
			b.append(name).append("\n");
			b.append("  compared ").append(compared).append(" (skipped because of an error: ").append(skipped).append(")\n");
			b.append("  win   ").append(count(wins, compared)).append("   (first is better: all boxes packed, or fewer containers)\n");
			b.append("  loss  ").append(count(losses, compared)).append("\n");
			b.append("  tie   ").append(count(ties, compared)).append("   (quality tie: ").append(tiesBothSuccess).append(" both packed all boxes in as many containers, ")
					.append(tiesBothFailed).append(" neither packed all boxes)\n");
			b.append("  identical summaries ").append(count(identical, compared)).append(", of which ").append(identicalBothSuccess).append(" with all boxes packed; ")
					.append("tie but different layout: ").append(ties - identical).append("\n");
			int samePlacements = identicalBothSuccess + onlyReorderedBothSuccess;
			b.append("  not identical though both packed all boxes: ").append(notIdenticalBothSuccess).append(", of which the same placements in another order: ")
					.append(onlyReorderedBothSuccess).append("\n");
			b.append("  same placements ignoring their order, all boxes packed: ").append(samePlacements).append(" (").append(String.format("%.2f%%", 100.0 * samePlacements / Math.max(1, compared)))
					.append(" of all orders, ").append(String.format("%.2f%%", 100.0 * samePlacements / Math.max(1, tiesBothSuccess))).append(" of the quality ties with all boxes packed)\n");
			b.append("  different placements: ").append(differentLayoutBothSuccess).append(", of which already the first placement differs: ").append(divergeAtFirstPlacement).append("\n");
			b.append("  first differing line of those: a different box of another volume or weight ").append(divergeOtherBox)
					.append(", a different box of equal volume and weight (tied in the box order) ").append(divergeOtherBoxSameVolumeAndWeight)
					.append(", same box with the same z and footprint area (a tie of the ranking, resolved differently) ")
					.append(divergeSameBoxSameZAndFootprint).append(", same box with another z or footprint area ").append(divergeSameBoxOther).append(", container structure ")
					.append(divergeContainers).append("\n");
			return b.toString();
		}
	}

	private static String count(int n, int of) {
		return String.format("%6d  %6.2f%%", n, of == 0 ? 0.0 : 100.0 * n / of);
	}

	// ---- packing

	private static Outcome outcome5(PackagerResult result, boolean validate) {
		boolean valid = true;
		if(validate && !result.getContainers().isEmpty()) {
			try {
				PackagerResultAssert.assertThat(result).isStackedWithinConstraints();
			} catch (AssertionError e) {
				valid = false;
			}
		}
		int placements = 0;
		int elevated = 0;
		int partly = 0;
		int unsupported = 0;
		for (Container container : result.getContainers()) {
			List<int[]> boxes = new ArrayList<>();
			for (Placement p : container.getStack().getPlacements()) {
				boxes.add(new int[] { p.getAbsoluteX(), p.getAbsoluteY(), p.getAbsoluteZ(), p.getStackValue().getDx(), p.getStackValue().getDy(), p.getStackValue().getDz() });
			}
			int[] counts = supportCounts(boxes);
			placements += boxes.size();
			elevated += counts[0];
			partly += counts[1];
			unsupported += counts[2];
		}
		return new Outcome(Version4Orders.summary(result), placements, elevated, partly, unsupported, valid);
	}

	private static Outcome outcome4(com.github.skjolber.packing.v4.api.PackagerResult result) {
		int placements = 0;
		int elevated = 0;
		int partly = 0;
		int unsupported = 0;
		for (com.github.skjolber.packing.v4.api.Container container : result.getContainers()) {
			List<int[]> boxes = new ArrayList<>();
			for (com.github.skjolber.packing.v4.api.Placement p : container.getStack().getPlacements()) {
				boxes.add(new int[] { p.getAbsoluteX(), p.getAbsoluteY(), p.getAbsoluteZ(), p.getStackValue().getDx(), p.getStackValue().getDy(), p.getStackValue().getDz() });
			}
			int[] counts = supportCounts(boxes);
			placements += boxes.size();
			elevated += counts[0];
			partly += counts[1];
			unsupported += counts[2];
		}
		return new Outcome(Version4Orders.summary(result), placements, elevated, partly, unsupported, true);
	}

	/**
	 * @param boxes the placements of a container as {x, y, z, dx, dy, dz}
	 * @return the number of placements above the floor, of those the number which rest on less than their footprint, and the number which rest on nothing
	 */
	private static int[] supportCounts(List<int[]> boxes) {
		int elevated = 0;
		int partly = 0;
		int unsupported = 0;
		for (int[] box : boxes) {
			if(box[2] == 0) {
				continue;
			}
			elevated++;
			long supported = 0;
			for (int[] below : boxes) {
				if(below[2] + below[5] == box[2]) {
					long ox = Math.max(0, Math.min(box[0] + box[3], below[0] + below[3]) - Math.max(box[0], below[0]));
					long oy = Math.max(0, Math.min(box[1] + box[4], below[1] + below[4]) - Math.max(box[1], below[1]));
					supported += ox * oy;
				}
			}
			if(supported == 0) {
				unsupported++;
			} else if(supported < (long)box[3] * box[4]) {
				partly++;
			}
		}
		return new int[] { elevated, partly, unsupported };
	}

	private static PlainPackager.Builder plainDefault() {
		return PlainPackager.newBuilder();
	}

	/** The comparator declares {@code usesSupportedArea()}, so the support is calculated with or without {@code withCalculateSupport(..)} */
	private static PlainPackager.Builder plainWith(PlacementComparator comparator, boolean calculateSupport) {
		return PlainPackager.newBuilder().withPlacementControlsBuilderFactory(b -> b.withCalculateSupport(calculateSupport).withPlacementComparator(comparator));
	}

	private static Outcome packPlain(PlainPackager.Builder builder, Scenario scenario, boolean validate) {
		try (PlainPackager packager = builder.build()) {
			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(scenario.containerItems())
					.withBoxItems(scenario.boxItems())
					.withMaxContainerCount(scenario.maxContainerCount())
					.withInterruptDuration(INTERRUPT_DURATION)
					.withInsertionOrder(INSERTION_ORDER)
					.build();
			return outcome5(result, validate);
		}
	}

	private static Outcome packPlain4(Scenario scenario) {
		return outcome4(com.github.skjolber.packing.v4.packer.plain.PlainPackager.newBuilder()
				.build()
				.newResultBuilder()
				.withContainerItems(scenario.version4ContainerItems())
				.withBoxItems(scenario.version4BoxItems())
				.withMaxContainerCount(scenario.maxContainerCount())
				.build());
	}

	private static Outcome packLaff(boolean fast, Scenario scenario) {
		if(fast) {
			try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build()) {
				return outcome5(packager.newResultBuilder()
						.withContainerItems(scenario.containerItems())
						.withBoxItems(scenario.boxItems())
						.withMaxContainerCount(scenario.maxContainerCount())
						.withInterruptDuration(INTERRUPT_DURATION)
						.withInsertionOrder(INSERTION_ORDER)
						.build(), false);
			}
		}
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			return outcome5(packager.newResultBuilder()
					.withContainerItems(scenario.containerItems())
					.withBoxItems(scenario.boxItems())
					.withMaxContainerCount(scenario.maxContainerCount())
					.withInterruptDuration(INTERRUPT_DURATION)
					.withInsertionOrder(INSERTION_ORDER)
					.build(), false);
		}
	}

	private static Outcome packLaff4(boolean fast, Scenario scenario) {
		if(fast) {
			return outcome4(com.github.skjolber.packing.v4.packer.laff.FastLargestAreaFitFirstPackager.newBuilder()
					.build()
					.newResultBuilder()
					.withContainerItems(scenario.version4ContainerItems())
					.withBoxItems(scenario.version4BoxItems())
					.withMaxContainerCount(scenario.maxContainerCount())
					.build());
		}
		return outcome4(com.github.skjolber.packing.v4.packer.laff.LargestAreaFitFirstPackager.newBuilder()
				.build()
				.newResultBuilder()
				.withContainerItems(scenario.version4ContainerItems())
				.withBoxItems(scenario.version4BoxItems())
				.withMaxContainerCount(scenario.maxContainerCount())
				.build());
	}

	private static Outcome run(Stats stats, long seed, Supplier<Outcome> pack) {
		long start = System.nanoTime();
		try {
			Outcome outcome = pack.get();
			stats.runs++;
			if(outcome.summary().success()) {
				stats.successes++;
				stats.containers += outcome.summary().containerCount();
				stats.shareSum += outcome.elevatedShare();
				stats.elevated += outcome.elevated();
				stats.partly += outcome.partlySupported();
				stats.unsupported += outcome.unsupported();
				stats.placements += outcome.placements();
			}
			if(!outcome.valid()) {
				stats.invalid++;
				if(stats.invalidSeeds.size() < 5) {
					stats.invalidSeeds.add(seed);
				}
			}
			return outcome;
		} catch (RuntimeException e) {
			stats.errors++;
			if(stats.errorMessages.size() < 3) {
				stats.errorMessages.add("seed " + seed + ": " + e);
			}
			return null;
		} finally {
			stats.nanos += System.nanoTime() - start;
		}
	}

	// ---- printing

	private static String describe(Scenario s) {
		StringBuilder b = new StringBuilder();
		b.append("seed ").append(s.seed()).append(", max container count ").append(s.maxContainerCount()).append("\n    containers:");
		for (ContainerSpec c : s.containers()) {
			b.append(" ").append(c.id()).append("[").append(c.dx()).append("x").append(c.dy()).append("x").append(c.dz()).append(" maxLoad ").append(c.maxLoadWeight()).append(" x")
					.append(c.count()).append("]");
		}
		b.append("\n    boxes:");
		for (BoxSpec x : s.boxes()) {
			b.append(" ").append(x.id()).append("[").append(x.dx()).append("x").append(x.dy()).append("x").append(x.dz()).append(" rot").append(x.rotation()).append(" w").append(x.weight())
					.append(" x").append(x.count()).append("]");
		}
		return b.toString();
	}

	private static String format(String label, Outcome outcome) {
		StringBuilder b = new StringBuilder();
		b.append("    ").append(label).append(": success=").append(outcome.summary().success()).append(" containers=").append(outcome.summary().containerCount()).append(" placements=")
				.append(outcome.placements()).append(" elevated=").append(outcome.elevated()).append("\n");
		for (String line : outcome.summary().lines()) {
			b.append("        ").append(line).append("\n");
		}
		return b.toString();
	}

	private static void print(String title, List<Example> examples, String firstLabel, String secondLabel) {
		System.out.println(title);
		if(examples.isEmpty()) {
			System.out.println("  (none)");
		}
		examples.sort((x, y) -> x.size() != y.size() ? Integer.compare(x.size(), y.size()) : Long.compare(x.seed, y.seed));
		for (Example e : examples) {
			System.out.println("  " + describe(e.scenario));
			System.out.print(format(firstLabel, e.first));
			System.out.print(format(secondLabel, e.second));
		}
		System.out.println();
	}

	// ---- sanity

	/**
	 * Checks that B really ranks by support, and stacks: two flat boxes which fit side by side, or one on top of the other. A ranks lower
	 * z higher so it places them side by side; B, like 4.x, prefers the higher z when the support is the same.
	 */
	private void sanity() {
		Scenario two = new Scenario(0, List.of(new ContainerSpec("c", 4, 2, 2, 100_000, 1)), List.of(new BoxSpec("b", 2, 2, 1, 0, 1, 2)), 1);

		Outcome a = packPlain(plainDefault(), two, true);
		Outcome b = packPlain(plainWith(new Version4HeuristicComparator(), true), two, true);
		Outcome c = packPlain4(two);
		System.out.println("SANITY two boxes 2x2x1 in a container 4x2x2");
		System.out.print(format("A", a));
		System.out.print(format("B", b));
		System.out.print(format("C", c));

		assertThat(b.summary().success()).isTrue();
		assertThat(b.elevated()).as("B stacks the second box on top of the first").isEqualTo(1);

		// what the comparator sees: floor placements are fully supported, placements on boxes partly or fully
		RecordingComparator recording = new RecordingComparator(new Version4HeuristicComparator());
		RecordingComparator withoutSupport = new RecordingComparator(new Version4HeuristicComparator());
		for (long seed = 0; seed < 200; seed++) {
			Scenario scenario = Version4Orders.random(seed);
			packPlain(plainWith(recording, true), scenario, false);
			packPlain(plainWith(withoutSupport, false), scenario, false);
		}
		System.out.println("SANITY comparator input over seeds 0..199, support calculated: " + recording);
		System.out.println("SANITY comparator input over seeds 0..199, without withCalculateSupport (usesSupportedArea() is honored): " + withoutSupport);
		System.out.println();
		assertThat(recording.floorOther).as("the floor is fully supported").isZero();
		assertThat(recording.floorFull).isPositive();
		assertThat(recording.airFull).isPositive();
		assertThat(recording.airPartial).as("overhangs are seen").isPositive();
		assertThat(withoutSupport.nonZeroSupport).as("usesSupportedArea() alone causes the support to be calculated").isPositive();
	}

	// ---- study

	@Test
	void study() {
		long startTime = System.nanoTime();

		sanity();

		PlainPackager.Builder plainA = plainDefault();
		PlainPackager.Builder plainB = plainWith(new Version4HeuristicComparator(), true);

		Stats[] stats = new Stats[NAMES.length];
		for (int i = 0; i < stats.length; i++) {
			stats[i] = new Stats(NAMES[i]);
		}

		Pair[] pairs = {
				new Pair("A vs C: 5.x plain default vs 4.x plain", A, C),
				new Pair("B vs C: 5.x plain with the 4.x ranking vs 4.x plain (emulation fidelity)", B, C),
				new Pair("B vs A: 5.x plain with the 4.x ranking vs 5.x plain default", B, A),
				new Pair("L5 vs L4: 5.x laff vs 4.x laff", L5, L4),
				new Pair("F5 vs F4: 5.x fast laff vs 4.x fast laff", F5, F4)
		};

		// orders where A, B and C all packed all boxes
		int commonCount = 0;
		double[] commonShare = new double[3];
		long[] commonContainers = new long[3];
		long[] commonElevated = new long[3];
		long[] commonPlacements = new long[3];

		for (long seed = FROM; seed < FROM + SEEDS; seed++) {
			Scenario scenario = Version4Orders.random(seed);

			Outcome[] o = new Outcome[NAMES.length];
			o[A] = run(stats[A], seed, () -> packPlain(plainA, scenario, true));
			o[B] = run(stats[B], seed, () -> packPlain(plainB, scenario, true));
			o[C] = run(stats[C], seed, () -> packPlain4(scenario));
			o[L5] = run(stats[L5], seed, () -> packLaff(false, scenario));
			o[L4] = run(stats[L4], seed, () -> packLaff4(false, scenario));
			o[F5] = run(stats[F5], seed, () -> packLaff(true, scenario));
			o[F4] = run(stats[F4], seed, () -> packLaff4(true, scenario));

			for (Pair pair : pairs) {
				Outcome x = o[pair.first];
				Outcome y = o[pair.second];
				if(x == null || y == null) {
					pair.skipped++;
				} else {
					pair.add(seed, scenario, x, y);
				}
			}

			if(o[A] != null && o[B] != null && o[C] != null && o[A].summary().success() && o[B].summary().success() && o[C].summary().success()) {
				commonCount++;
				for (int i = 0; i < 3; i++) {
					commonShare[i] += o[i].elevatedShare();
					commonContainers[i] += o[i].summary().containerCount();
					commonElevated[i] += o[i].elevated();
					commonPlacements[i] += o[i].placements();
				}
			}

			if((seed - FROM + 1) % 1000 == 0) {
				System.out.println("progress: " + (seed - FROM + 1) + " seeds, " + (System.nanoTime() - startTime) / 1_000_000_000L + " s");
			}
		}

		System.out.println();
		System.out.println("==== STUDY seeds " + FROM + ".." + (FROM + SEEDS - 1) + " (" + SEEDS + " orders), 5.x insertion order " + INSERTION_ORDER);
		System.out.println();

		System.out.println("---- per configuration (time is the total for packing the orders, including validation for A and B)");
		for (Stats s : stats) {
			System.out.printf("%-60s runs %5d  errors %3d  all boxes packed %5d (%5.2f%%)  mean containers (packed) %.4f  time %.1f s%n", s.name, s.runs, s.errors, s.successes,
					100.0 * s.successes / Math.max(1, s.runs), s.successes == 0 ? 0.0 : (double)s.containers / s.successes, s.nanos / 1e9);
			for (String message : s.errorMessages) {
				System.out.println("      error " + message);
			}
			if(s.invalid > 0) {
				System.out.println("      " + s.invalid + " results not stacked within constraints, first seeds " + s.invalidSeeds);
			}
		}
		System.out.println();

		System.out.println("---- 1. pairwise tallies (quality: all boxes packed, then fewer containers)");
		for (Pair pair : pairs) {
			System.out.print(pair.report());
		}
		System.out.println();

		System.out.println("---- 3. elevated share: placements with z > 0 / all placements");
		System.out.println("per order, averaged over the orders where the configuration packed all boxes; pooled = all elevated placements / all placements of those orders");
		for (int i : new int[] { A, B, C, L5, L4, F5, F4 }) {
			Stats s = stats[i];
			System.out.printf("  %-60s mean per order %.4f  pooled %.4f  (%d orders)%n", s.name, s.successes == 0 ? 0.0 : s.shareSum / s.successes,
					s.placements == 0 ? 0.0 : (double)s.elevated / s.placements, s.successes);
		}
		System.out.println("support of the elevated placements (z > 0), pooled over the same orders: share resting on less than their footprint, and on nothing");
		for (int i : new int[] { A, B, C, L5, L4, F5, F4 }) {
			Stats s = stats[i];
			System.out.printf("  %-60s elevated %7d  partly supported %6.2f%%  unsupported %6.2f%%%n", s.name, s.elevated, s.elevated == 0 ? 0.0 : 100.0 * s.partly / s.elevated,
					s.elevated == 0 ? 0.0 : 100.0 * s.unsupported / s.elevated);
		}
		System.out.println("same, over the " + commonCount + " orders where A, B and C all packed all boxes");
		for (int i = 0; i < 3; i++) {
			System.out.printf("  %-60s mean per order %.4f  pooled %.4f  mean containers %.4f%n", NAMES[i], commonCount == 0 ? 0.0 : commonShare[i] / commonCount,
					commonPlacements[i] == 0 ? 0.0 : (double)commonElevated[i] / commonPlacements[i], commonCount == 0 ? 0.0 : (double)commonContainers[i] / commonCount);
		}
		System.out.println();

		System.out.println("---- examples (the smallest layouts among the orders concerned)");
		print("B loses to C (emulation divergence)", pairs[1].lossExamples, "B", "C");
		print("B vs C, not identical though both packed all boxes: the first differing line has another box", pairs[1].otherBoxExamples, "B", "C");
		print("B vs C, not identical though both packed all boxes: the first differing line has the same box at another z or footprint area", pairs[1].sameBoxOtherExamples, "B", "C");
		print("B beats A", pairs[2].winExamples, "B", "A");
		print("A beats B", swap(pairs[2].lossExamples), "A", "B");
		print("5.x plain default (A) beats 4.x (C)", pairs[0].winExamples, "A", "C");
		print("5.x plain default (A) loses to 4.x (C)", pairs[0].lossExamples, "A", "C");
		print("L5 loses to L4", pairs[3].lossExamples, "L5", "L4");
		print("F5 loses to F4", pairs[4].lossExamples, "F5", "F4");

		System.out.printf("total time %.1f s%n", (System.nanoTime() - startTime) / 1e9);
	}

	/** The examples of the pair B vs A in which B loses, as examples of A vs B */
	private static List<Example> swap(List<Example> examples) {
		List<Example> result = new ArrayList<>(examples.size());
		for (Example e : examples) {
			result.add(new Example(e.seed, e.scenario, e.second, e.first));
		}
		return result;
	}
}
