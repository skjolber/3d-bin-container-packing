package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * Golden master for packing results: seeded random orders packed by each packager, with every
 * placement folded into a checksum. The expected checksums were recorded from the implementation
 * before performance work on the point calculators; optimizations must preserve them exactly.
 *
 * <pre>
 *   seed --> random boxes --> packager --> containers --> checksum of (container, x, y, z, dx, dy, dz, box)
 * </pre>
 *
 * To re-record after an intentional behavior change, run with {@code -Dgolden.record=true}.
 */
public class PackagerGoldenMasterTest {

	private static final int SEEDS = 12;

	private static final long[] EXPECTED_PLAIN = {
		-8125460584613589196L, -4007092753495096003L, -8314917218695541917L, 6330087534530794951L,
		-3353823600822593436L, 7475620767755938904L, 1930236695440217529L, -5833765920609263258L,
		6525436538119130930L, 5740560506386502804L, 2L, -6190387249970890846L,
	};
	private static final long[] EXPECTED_PLAIN_GROUPS = {
		2L, 2L, 2L, -2032654743021448663L,
		3589503855599729816L, -5574024339278861482L, 2L, -2909053052049581072L,
		2L, 2L, 2L, 5666543873772116186L,
	};
	private static final long[] EXPECTED_LAFF = {
		3153628227128398916L, -6919073934318036109L, -665764527610979713L, -2267211291841922765L,
		4253691313652650181L, -1021017525770118402L, 6518814367490818566L, 832066022421585147L,
		-7128108129372295242L, -5334338792276141704L, -1936242811544520425L, -4910033009882616769L,
	};
	private static final long[] EXPECTED_FAST_LAFF = {
		3153628227128398916L, -6038480351888857219L, -400919950876757339L, -3058446070184556941L,
		8727452831431883490L, -1021017525770118402L, 6518814367490818566L, -4768101040292129277L,
		-7128108129372295242L, -1078286612861106899L, 8703860944983651677L, 76520317873161353L,
	};
	private static final long[] EXPECTED_BRUTE_FORCE = {
		7843580046106859895L, 2440287864107926838L, 2548309615160605511L, 6039731106805676818L,
		5330130213954713715L, -2358077914062050043L, 9015769085271897720L, -5934595884552817430L,
		1746508692343909076L, -7004518245045586089L, -2105154166812315596L, -738026494876541206L,
	};
	private static final long[] EXPECTED_FAST_BRUTE_FORCE = {
		-1560624090259993544L, 5413079662647782823L, -4516682763885846808L, -7262719745320755856L,
		8728185108150325914L, -2358077914062050043L, 1839652993381982561L, -7810854047523803376L,
		-372242133089036785L, 4199480982093190664L, -8521740758384882670L, -473978201624261271L,
	};

	// load limits: recorded after fixing max load weight validation to count all levels and paths
	private static final long[] EXPECTED_PLAIN_LOAD_WEIGHT = {
		6589069623082643938L, 81039236789473475L, -1797899563749744075L, 6330087534530794951L,
		5376224182771789087L, -7513425642811408186L, -1983292615108176145L, 4682514977918533302L,
		-4572262393676727746L, 7130504414036667758L, 2L, -60350065412151678L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD = {
		7309104712301445591L, -653233135499217900L, -8623567631089296812L, 6330087534530794951L,
		-1462200561396399423L, 2277814755061572745L, -1983292615108176145L, 6397218944232533496L,
		6525436538119130930L, -6323359436677269901L, 2L, 8134015584711729404L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD_IDENTICAL = {
		3724055406751851022L, -833218921769312981L, 2389175421615136264L, 6330087534530794951L,
		-3104287622241531972L, -7513425642811408186L, -1542815407150795683L, 150244721619004359L,
		5015260888062179456L, -6561971635329019084L, 2L, -7463867672966263811L,
	};
	private static final long[] EXPECTED_LAFF_LOAD = {
		7672082031273427783L, 8388558747619766127L, 2L, -4145238886259736213L,
		-7515374598539958332L, -3724500381554186230L, 792412005652056350L, 1581358766320319693L,
		-3000234718410066959L, -7557826166562001291L, 2L, 2L,
	};
	private static final long[] EXPECTED_FAST_LAFF_LOAD = {
		7672082031273427783L, 1888809420262416351L, 2L, 2817127498803975670L,
		-3119496990263852379L, -3724500381554186230L, 792412005652056350L, -2866389344860570129L,
		-3000234718410066959L, -1304404878125705941L, 2L, 2L,
	};

	static final int LOAD_NONE = 0;
	static final int LOAD_WEIGHT = 1;
	static final int LOAD_WEIGHT_PRESSURE_COUNT = 2;
	static final int LOAD_IDENTICAL = 3;

	@Test
	public void plainPackagerLoadWeight() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check("EXPECTED_PLAIN_LOAD_WEIGHT", EXPECTED_PLAIN_LOAD_WEIGHT, seed -> pack(packager, seed, 40, 2, false, LOAD_WEIGHT));
		}
	}

	@Test
	public void plainPackagerLoad() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check("EXPECTED_PLAIN_LOAD", EXPECTED_PLAIN_LOAD, seed -> pack(packager, seed, 40, 2, false, LOAD_WEIGHT_PRESSURE_COUNT));
		}
	}

	@Test
	public void plainPackagerLoadIdentical() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check("EXPECTED_PLAIN_LOAD_IDENTICAL", EXPECTED_PLAIN_LOAD_IDENTICAL, seed -> pack(packager, seed, 40, 2, false, LOAD_IDENTICAL));
		}
	}

	@Test
	public void largestAreaFitFirstPackagerLoad() throws IOException {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			check("EXPECTED_LAFF_LOAD", EXPECTED_LAFF_LOAD, seed -> pack(packager, seed, 40, 2, false, LOAD_WEIGHT_PRESSURE_COUNT));
		}
	}

	@Test
	public void fastLargestAreaFitFirstPackagerLoad() throws IOException {
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build()) {
			check("EXPECTED_FAST_LAFF_LOAD", EXPECTED_FAST_LAFF_LOAD, seed -> pack(packager, seed, 40, 2, false, LOAD_WEIGHT_PRESSURE_COUNT));
		}
	}

	@Test
	public void plainPackager() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check("EXPECTED_PLAIN", EXPECTED_PLAIN, seed -> pack(packager, seed, 40, 2, false));
		}
	}

	@Test
	public void plainPackagerGroups() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().build()) {
			check("EXPECTED_PLAIN_GROUPS", EXPECTED_PLAIN_GROUPS, seed -> pack(packager, seed, 30, 2, true));
		}
	}

	@Test
	public void largestAreaFitFirstPackager() throws IOException {
		try (LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().build()) {
			check("EXPECTED_LAFF", EXPECTED_LAFF, seed -> pack(packager, seed, 40, 2, false));
		}
	}

	@Test
	public void fastLargestAreaFitFirstPackager() throws IOException {
		try (FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().build()) {
			check("EXPECTED_FAST_LAFF", EXPECTED_FAST_LAFF, seed -> pack(packager, seed, 40, 2, false));
		}
	}

	@Test
	public void bruteForcePackager() throws IOException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			check("EXPECTED_BRUTE_FORCE", EXPECTED_BRUTE_FORCE, seed -> pack(packager, seed, 5, 1, false));
		}
	}

	@Test
	public void fastBruteForcePackager() throws IOException {
		try (FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().build()) {
			check("EXPECTED_FAST_BRUTE_FORCE", EXPECTED_FAST_BRUTE_FORCE, seed -> pack(packager, seed, 5, 1, false));
		}
	}

	private interface Run {
		long run(long seed);
	}

	private static void check(String name, long[] expected, Run run) {
		long[] actual = new long[SEEDS];
		for(int i = 0; i < SEEDS; i++) {
			actual[i] = run.run(i);
		}
		if(Boolean.getBoolean("golden.record")) {
			StringBuilder builder = new StringBuilder("private static final long[] " + name + " = {");
			for(int i = 0; i < actual.length; i++) {
				builder.append(i % 4 == 0 ? "\n\t\t" : " ").append(actual[i]).append("L,");
			}
			System.out.println(builder.append("\n\t};"));
			return;
		}
		assertThat(actual).containsExactly(expected);
	}

	/**
	 * @param maxBoxes maximum number of box items (brute force is exponential: keep small)
	 * @param maxCount maximum count per box item
	 */
	protected static long pack(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups) {
		return pack(packager, seed, maxBoxes, maxCount, groups, LOAD_NONE);
	}

	/**
	 * @param load which load limits to give the boxes; drawn from a separate random sequence, so
	 *        that boxes are otherwise the same as without load limits
	 */
	protected static long pack(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups, int load) {
		Random random = new Random(seed);
		List<BoxItem> items = createItems(random, new Random(seed * 31 + 7), maxBoxes, maxCount, load);
		List<ContainerItem> containers = createContainers(random);

		PackagerResult result;
		try {
			result = build(packager, items, containers, groups);
		} catch(RuntimeException e) {
			// a crash is recorded as an outcome, so that new crashes and behavior changes are both detected
			return 3L * 31 + e.getClass().getName().hashCode();
		}

		long hash = result.isSuccess() ? 1 : 2;
		for(int c = 0; c < result.size(); c++) {
			Container packed = result.get(c);
			hash = hash * 31 + c;
			for(Placement placement : packed.getStack().getPlacements()) {
				hash = hash * 31 + placement.getAbsoluteX();
				hash = hash * 31 + placement.getAbsoluteY();
				hash = hash * 31 + placement.getAbsoluteZ();
				hash = hash * 31 + placement.getStackValue().getDx();
				hash = hash * 31 + placement.getStackValue().getDy();
				hash = hash * 31 + placement.getStackValue().getDz();
				hash = hash * 31 + placement.getStackValue().getBox().getId().hashCode();
			}
		}
		return hash;
	}

	/** Random boxes; the load limits are drawn from {@code loadRandom}, so boxes are otherwise the same for all load modes. */
	static List<BoxItem> createItems(Random random, Random loadRandom, int maxBoxes, int maxCount, int load) {
		int boxCount = 2 + random.nextInt(maxBoxes - 1);
		List<BoxItem> items = new ArrayList<>();
		for(int i = 0; i < boxCount; i++) {
			Box.Builder builder = Box.newBuilder()
					.withId("box-" + i)
					.withSize(1 + random.nextInt(9), 1 + random.nextInt(9), 1 + random.nextInt(9))
					.withRotate3D()
					.withWeight(1 + random.nextInt(5));
			// limits tight enough to bind for the random weights (1-5)
			if(load != LOAD_NONE) {
				builder.withMaxLoadWeight(1 + loadRandom.nextInt(15));
			}
			if(load == LOAD_WEIGHT_PRESSURE_COUNT || load == LOAD_IDENTICAL) {
				builder.withMaxLoadPressure(0.05 + loadRandom.nextInt(20) * 0.05);
				builder.withMaxLoadBoxCount(1 + loadRandom.nextInt(4));
			}
			if(load == LOAD_IDENTICAL && loadRandom.nextInt(3) == 0) {
				builder.withMaxLoadIdenticalBoxCount(1 + loadRandom.nextInt(3));
			}
			items.add(new BoxItem(builder.build(), 1 + random.nextInt(maxCount)));
		}
		return items;
	}

	static List<ContainerItem> createContainers(Random random) {
		Container container = Container.newBuilder()
				.withDescription("container")
				.withSize(10 + random.nextInt(8), 10 + random.nextInt(8), 10 + random.nextInt(8))
				.withEmptyWeight(1)
				.withMaxLoadWeight(1_000)
				.build();
		return ContainerItem.newListBuilder()
				.withContainer(container, 4)
				.build();
	}

	private static PackagerResult build(Packager<?> packager, List<BoxItem> items, List<ContainerItem> containers, boolean groups) {
		if(groups) {
			List<BoxItemGroup> boxItemGroups = new ArrayList<>();
			for(int i = 0; i < items.size(); i += 2) {
				boxItemGroups.add(new BoxItemGroup("group-" + i, new ArrayList<>(items.subList(i, Math.min(i + 2, items.size())))));
			}
			return packager.newResultBuilder()
					.withContainerItems(containers)
					.withBoxItemGroups(boxItemGroups)
					.withMaxContainerCount(4)
					.build();
		}
		return packager.newResultBuilder()
				.withContainerItems(containers)
				.withBoxItems(items)
				.withMaxContainerCount(4)
				.build();
	}
}
