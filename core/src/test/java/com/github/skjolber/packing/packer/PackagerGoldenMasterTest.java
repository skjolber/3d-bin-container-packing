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
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager.ClosestVolumeAndAreaPointFilter;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.LoadBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.LoadParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
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
 * <p>
 * The checksums include the order of the placements: re-recorded when results were put in insertion order
 * (see {@code InsertionSequencer}), which changes only the order.
 */
public class PackagerGoldenMasterTest {

	private static final int SEEDS = 12;

	private static final long[] EXPECTED_PLAIN = {
		-9160848500013070274L, 7667088552895734155L, 5201803515951897933L, 6330087534530794951L,
		-644367644032015422L, 7475620767755938904L, 627226774454442345L, -792337151211718308L,
		3709420909368498176L, 5740560506386502804L, 2L, 95658530115774112L,
	};
	private static final long[] EXPECTED_PLAIN_GROUPS = {
		2L, 2L, 2L, -2032654743021448663L,
		3589503855599729816L, -5574024339278861482L, 2L, -2909053052049581072L,
		2L, 2L, 2L, 5666543873772116186L,
	};
	private static final long[] EXPECTED_LAFF = {
		3153628227128398916L, -6919073934318036109L, -665764527610979713L, -2267211291841922765L,
		4253691313652650181L, -1021017525770118402L, 6518814367490818566L, -6511231719034580139L,
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
	// (identical: re-recorded after fixing off-by-one limits in the full-support fallback;
	// weight and weight/pressure/count: re-recorded after recording boxes placed under boxes already there;
	// all three: re-recorded when such boxes relieve the boxes below, as they are inserted first)
	private static final long[] EXPECTED_PLAIN_LOAD_WEIGHT = {
		-925753603611382288L, 639348097663690781L, -6272832596758394095L, 6330087534530794951L,
		-3802822227494517041L, -7513425642811408186L, -1983292615108176145L, -8109133009313854259L,
		-4572262393676727746L, 7130504414036667758L, 2L, 7151371277880131217L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD = {
		4818304549925485126L, -7590956110027602462L, -4391878859024672438L, 6330087534530794951L,
		-7004146948283626607L, 2277814755061572745L, -1983292615108176145L, 3233188466323884316L,
		3709420909368498176L, -6323359436677269901L, 2L, 2313201347568195485L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD_IDENTICAL = {
		-1600863268006379955L, -833218921769312981L, 1241005079151785244L, 6330087534530794951L,
		5511018664920160600L, -7513425642811408186L, -1542815407150795683L, 150244721619004359L,
		5015260888062179456L, -3686020180268787754L, 2L, 8574484934765814692L,
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

	public static final int LOAD_NONE = 0;
	public static final int LOAD_WEIGHT = 1;
	public static final int LOAD_WEIGHT_PRESSURE_COUNT = 2;
	public static final int LOAD_IDENTICAL = 3;

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

	// full support: re-recorded after fixing off-by-one limits in the fallback
	private static final long[] EXPECTED_PLAIN_SUPPORT = {
		8499244949225421112L, 8795503427511248271L, 9095814756170933444L, 6330087534530794951L,
		-8977633540374500160L, 7475620767755938904L, 4469331435551308345L, -8567859255327857506L,
		1042620184816567544L, -6812826950630383200L, 2L, -5598021915169739785L,
	};
	private static final long[] EXPECTED_PLAIN_FULL_SUPPORT = {
		7540344805314906826L, 5753752866334497554L, 4611678950995132222L, 6330087534530794951L,
		6427231848685324706L, 7475620767755938904L, 4469331435551308345L, -8567859255327857506L,
		-4572262393676727746L, 9199315560691006402L, 2L, 6934468580461683793L,
	};

	@Test
	public void plainPackagerSupport() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().withCalculateSupport(true).build()) {
			check("EXPECTED_PLAIN_SUPPORT", EXPECTED_PLAIN_SUPPORT, seed -> pack(packager, seed, 40, 2, false));
		}
	}

	@Test
	public void plainPackagerFullSupport() throws IOException {
		try (PlainPackager packager = PlainPackager.newBuilder().withRequireFullSupport(true).build()) {
			check("EXPECTED_PLAIN_FULL_SUPPORT", EXPECTED_PLAIN_FULL_SUPPORT, seed -> pack(packager, seed, 40, 2, false));
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

	/*
	 * Brute-force variants: point filter, groups, load limits and the parallel packagers. The parallel
	 * packagers select among equally good results in thread completion order, so only a summary of their
	 * results is deterministic.
	 */

	private static final long[] EXPECTED_BRUTE_FORCE_POINT_FILTER = {
		7843580046106859895L, 2440287871093439682L, -4516682763885846808L, 6039731108546513903L,
		8728185113305420136L, -2358077914062050043L, 1839652993381982561L, -7810854047523803376L,
		-372242133089036785L, -7004518245045586089L, -8521740760356600005L, -473978201621490708L,
	};

	private static final long[] EXPECTED_BRUTE_FORCE_GROUPS = {
		7843580046106859895L, 2440287864107926838L, 2548309615160605511L, 6039731106805676818L,
		5330130213954713715L, -2358077914062050043L, 9015769085271897720L, -5934595884552817430L,
		1746508692343909076L, -7004518245045586089L, -2105154166812315596L, -738026494876541206L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_WEIGHT = {
		-1560624090259993544L, 2440287864107926838L, 4657213197540965501L, 6039731106805676818L,
		6854638827737156533L, -2358077914062050043L, 9015769088129271694L, -5934595884496482649L,
		1746508692343909076L, -7004518245045586089L, -2105154166812315596L, -738026494876541206L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE = {
		-1560624090259993544L, 3990204954788621529L, -6268159047462828399L, 6039731106828764843L,
		6854638827737156533L, 4174926516148258215L, -4681298428429779372L, -5934595884472471103L,
		1746508692343909076L, -7004518245045586089L, -8521740758384882670L, -8280268140199339607L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_IDENTICAL = {
		-1560624090258174402L, 5413079661044550367L, -6268159047462828399L, 4933539009470205692L,
		6854638827737156533L, -2354905164612236164L, -4681298428429779372L, -1280305952004183045L,
		1917409172158508868L, -7890222614149646750L, -8445515327695668177L, -8280268140199339607L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_POINT_FILTER = {
		-1560624090259993544L, 3990204961774134373L, -4516682763660507684L, 6039731108546513903L,
		8728185108150325914L, 4174926516148258215L, 1941286900968525406L, -7810854047523803376L,
		-372242133089036785L, -7004518245045586089L, -8521740758384882670L, -473978201621490708L,
	};

	private static final long[] EXPECTED_PARALLEL_BRUTE_FORCE = {
		53359L, 38364L, 40409L, 45924L,
		43878L, 59469L, 56247L, 44435L,
		70689L, 56280L, 41583L, 49457L,
	};

	private static final long[] EXPECTED_LOAD_PARALLEL_BRUTE_FORCE = {
		53359L, 38364L, 40409L, 45924L,
		43878L, 59469L, 56247L, 44435L,
		70689L, 56280L, 41583L, 49457L,
	};

	@Test
	public void bruteForcePackagerPointFilter() throws IOException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().withPointFilter(new ClosestVolumeAndAreaPointFilter()).build()) {
			check("EXPECTED_BRUTE_FORCE_POINT_FILTER", EXPECTED_BRUTE_FORCE_POINT_FILTER, seed -> pack(packager, seed, 5, 1, false));
		}
	}

	@Test
	public void bruteForcePackagerGroups() throws IOException {
		try (BruteForcePackager packager = BruteForcePackager.newBuilder().build()) {
			check("EXPECTED_BRUTE_FORCE_GROUPS", EXPECTED_BRUTE_FORCE_GROUPS, seed -> pack(packager, seed, 5, 1, true));
		}
	}

	@Test
	public void loadBruteForcePackagerWeight() throws IOException {
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().build()) {
			check("EXPECTED_LOAD_BRUTE_FORCE_WEIGHT", EXPECTED_LOAD_BRUTE_FORCE_WEIGHT, seed -> pack(packager, seed, 5, 1, false, LOAD_WEIGHT));
		}
	}

	@Test
	public void loadBruteForcePackager() throws IOException {
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().build()) {
			check("EXPECTED_LOAD_BRUTE_FORCE", EXPECTED_LOAD_BRUTE_FORCE, seed -> pack(packager, seed, 5, 1, false, LOAD_WEIGHT_PRESSURE_COUNT));
		}
	}

	@Test
	public void loadBruteForcePackagerIdentical() throws IOException {
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().build()) {
			check("EXPECTED_LOAD_BRUTE_FORCE_IDENTICAL", EXPECTED_LOAD_BRUTE_FORCE_IDENTICAL, seed -> pack(packager, seed, 5, 1, false, LOAD_IDENTICAL));
		}
	}

	@Test
	public void loadBruteForcePackagerPointFilter() throws IOException {
		try (LoadBruteForcePackager packager = LoadBruteForcePackager.newBuilder().withPointFilter(new ClosestVolumeAndAreaPointFilter()).build()) {
			check("EXPECTED_LOAD_BRUTE_FORCE_POINT_FILTER", EXPECTED_LOAD_BRUTE_FORCE_POINT_FILTER, seed -> pack(packager, seed, 5, 1, false, LOAD_WEIGHT_PRESSURE_COUNT));
		}
	}

	@Test
	public void parallelBruteForcePackager() throws IOException {
		try (ParallelBoxItemBruteForcePackager packager = ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(4).build()) {
			check("EXPECTED_PARALLEL_BRUTE_FORCE", EXPECTED_PARALLEL_BRUTE_FORCE, seed -> packSummary(packager, seed, 5, 1, false));
		}
	}

	@Test
	public void loadParallelBruteForcePackager() throws IOException {
		try (LoadParallelBoxItemBruteForcePackager packager = LoadParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(4).build()) {
			check("EXPECTED_LOAD_PARALLEL_BRUTE_FORCE", EXPECTED_LOAD_PARALLEL_BRUTE_FORCE, seed -> packSummary(packager, seed, 5, 1, false, LOAD_WEIGHT_PRESSURE_COUNT));
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
	public static long pack(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups) {
		return pack(packager, seed, maxBoxes, maxCount, groups, LOAD_NONE);
	}

	/**
	 * @param load which load limits to give the boxes; drawn from a separate random sequence, so
	 *        that boxes are otherwise the same as without load limits
	 */
	public static long pack(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups, int load) {
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

	/**
	 * Checksum of the result summary: success, and the number of boxes, loaded volume and weight per container.
	 */
	protected static long packSummary(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups, int load) {
		Random random = new Random(seed);
		List<BoxItem> items = createItems(random, new Random(seed * 31 + 7), maxBoxes, maxCount, load);
		List<ContainerItem> containers = createContainers(random);

		PackagerResult result = build(packager, items, containers, groups);
		long hash = result.isSuccess() ? 1 : 2;
		for(int c = 0; c < result.size(); c++) {
			Container packed = result.get(c);
			hash = hash * 31 + packed.getStack().size();
			hash = hash * 31 + packed.getStack().getVolume();
			hash = hash * 31 + packed.getStack().getWeight();
		}
		return hash;
	}

	protected static long packSummary(Packager<?> packager, long seed, int maxBoxes, int maxCount, boolean groups) {
		return packSummary(packager, seed, maxBoxes, maxCount, groups, LOAD_NONE);
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
