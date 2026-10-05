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
 * (see {@code InsertionSequencer}), and when that order became the order by height; both change only the order.
 */
public class PackagerGoldenMasterTest {

	private static final int SEEDS = 12;

	private static final long[] EXPECTED_PLAIN = {
		6269108598308100426L, -4218810479978732669L, 5609864738487528145L, 6330087534530794951L,
		-1661402643685732956L, 7475620767755938904L, 3067614420663008315L, -3107238940785257222L,
		1042620184816567544L, -4132006372344096946L, 2L, -8184319057665681374L,
	};
	// groups: re-recorded after fixing the placement search to cover all boxes of a group (it ended at the group's
	// size as an index, so a group which did not start at the first box was searched partly or not at all)
	// and when the boxes of each group were inserted together (the placement order changed; success and container
	// counts are unchanged, two of 40 probed seeds spread their boxes differently over the containers)
	private static final long[] EXPECTED_PLAIN_GROUPS = {
		6517405776647160655L, 3041863191684893552L, 7118535348878618426L, -2032654743021448663L,
		3589503855599729816L, 482441099952015889L, 2L, 1628858756539204265L,
		2L, 7044755507023126594L, 6177845627565951891L, 8725034261711837291L,
	};
	private static final long[] EXPECTED_LAFF = {
		3153628227128398916L, 4344751454144374009L, 5628232661577704305L, -2267211291841922765L,
		4253691313652650181L, -1021017525770118402L, 6518814367490818566L, 1367928967171889045L,
		-7128108129372295242L, -9158421765456766304L, -1936242811544520425L, -8680935825088634087L,
	};
	private static final long[] EXPECTED_FAST_LAFF = {
		3153628227128398916L, -6038480351888857219L, -400919950876757339L, -3058446070184556941L,
		8727452831431883490L, -1021017525770118402L, 6518814367490818566L, -4768101040292129277L,
		-7128108129372295242L, -1078286612861106899L, 8703860944983651677L, 76520317873161353L,
	};
	private static final long[] EXPECTED_BRUTE_FORCE = {
		-7633541546413779901L, 5409039543547643048L, 3136616822457889421L, -2044062733234777270L,
		5330130213954713715L, -2358077914062050043L, 9015769085271897720L, -5934595884552817430L,
		-2765747327543961870L, 3663375559547421731L, -7362082114236403302L, 1453686011565233354L,
	};
	private static final long[] EXPECTED_FAST_BRUTE_FORCE = {
		-1560624090259993544L, 5413079662647782823L, -4516682763885846808L, -7262719745320755856L,
		-5044198348180103722L, -2358077914062050043L, 1839652993381982561L, -7810854047523803376L,
		-372242133089036785L, 4199480982093190664L, 8903866090484190208L, -473978201624261271L,
	};

	// load limits: recorded after fixing max load weight validation to count all levels and paths
	// (identical: re-recorded after fixing off-by-one limits in the full-support fallback;
	// weight and weight/pressure/count: re-recorded after recording boxes placed under boxes already there;
	// all three: re-recorded when such boxes relieve the boxes below, as they are inserted first)
	private static final long[] EXPECTED_PLAIN_LOAD_WEIGHT = {
		-7278450135807481676L, -7593054383952631157L, 1678728453830140449L, 6330087534530794951L,
		2682820483957262671L, -7513425642811408186L, -1254512451382163703L, -140256645068609361L,
		-4572262393676727746L, -2540965422519104882L, 2L, 5322114658185957429L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD = {
		-1231746723585068318L, -7632579730128107382L, 923310607841865656L, 6330087534530794951L,
		-786315448319721581L, 2277814755061572745L, -1254512451382163703L, -5888320648292507378L,
		1042620184816567544L, 3285420651363551829L, 2L, 5935128686767365055L,
	};
	private static final long[] EXPECTED_PLAIN_LOAD_IDENTICAL = {
		-5270661235118862813L, -3897126229941941899L, -2716980932078629694L, 6330087534530794951L,
		7200344719187717956L, -7513425642811408186L, -6653138079995642183L, -5072334428258487725L,
		8650261768278845418L, -4049003709997568758L, 2L, 2434988644481735968L,
	};
	private static final long[] EXPECTED_LAFF_LOAD = {
		7672082031273427783L, 3659540260224510663L, 2L, -4145238886259736213L,
		3799772826547815654L, -3724500381554186230L, 792412005652056350L, -8926997828161368213L,
		-3000234718410066959L, -6382514313413117565L, 2L, 2L,
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
		-9075380168386622064L, -3131598466575916135L, -3146844372858785354L, 6330087534530794951L,
		1689082614725196586L, 7475620767755938904L, 3067614420662979485L, -7653027299444263182L,
		1042620184816567544L, 8612228455617767752L, 2L, 1257138254960851617L,
	};
	private static final long[] EXPECTED_PLAIN_FULL_SUPPORT = {
		6280330005420608328L, -3589431017044658746L, 6047818531465536446L, 6330087534530794951L,
		-7010033367002818820L, 7475620767755938904L, 3067614420662979485L, -7653027299444263182L,
		-4572262393676727746L, 6844942549526695448L, 2L, 2738420796642164409L,
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
		-7633541546413779901L, -5315426022851283044L, -4516682763885846808L, 8957428069069225933L,
		8728185113305420136L, -2358077914062050043L, 1839652993381982561L, -7810854047523803376L,
		-372242133089036785L, 3663375559547421731L, -8521740760356600005L, -473978201621490708L,
	};

	// groups: re-recorded when the boxes of each group were inserted together (success and boxes per container are
	// unchanged)
	private static final long[] EXPECTED_BRUTE_FORCE_GROUPS = {
		-7633541546413779901L, 5409039543547643048L, 3136616822457889421L, 6039731106805676818L,
		5330130213954713715L, -2358077914062050043L, 9015769085271897720L, -5934595884552817430L,
		8706740959182255506L, 3663375559547421731L, 3277214328778857454L, 1453686011565233354L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_WEIGHT = {
		-1560624090259993544L, 5409039543547643048L, -99884376588914189L, -2044062733234777270L,
		-1732625102963685305L, -2358077914062050043L, -4576250406293102926L, -1239025572799048789L,
		-2765747327543961870L, 3663375559547421731L, -7362082114236403302L, 1453686011565233354L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE = {
		-1560624090259993544L, 5409039543603977829L, -6268159047462828399L, -1408850810824656495L,
		-1732625102963685305L, 9049185741530308401L, 1273006024687245378L, 5891486491182146729L,
		-2765747327543961870L, 3663375559547421731L, 8903866090484190208L, -8280268140199339607L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_IDENTICAL = {
		-1560624090258174402L, 5413079661044550367L, -6268159047462828399L, 4933539009470205692L,
		-1732625102963685305L, -2354905164612236164L, 1273006024687245378L, 3415264359693250815L,
		1917409172158508868L, -3649500522981729288L, 8903866090486960771L, -8280268140199339607L,
	};

	private static final long[] EXPECTED_LOAD_BRUTE_FORCE_POINT_FILTER = {
		-1560624090259993544L, -5315426022794948263L, -4516682763660507684L, 8957428069069225933L,
		-5044198348180103722L, 9049185741530308401L, 1941286900968525406L, -7810854047523803376L,
		-372242133089036785L, 3663375559547421731L, 8903866090484190208L, -473978201621490708L,
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
