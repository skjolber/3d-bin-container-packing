package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxItemGroup;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerAccess;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.MaxFireHazardBoxItemGroupsPerContainerManifestControls;
import com.github.skjolber.packing.packer.plain.MaxFireHazardBoxItemPerContainerManifestControls;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;
import com.github.skjolber.packing.validator.stability.FullySupportedStabilityValidator;

/**
 * Every packager either packs each input feature correctly, or rejects the input: no packager silently ignores a
 * feature. Each cell (scenario, packager) has an expected status; a known gap must still fail, so that the table is
 * updated when a gap is closed. The expected table is the feature support table of FEATURES.md.
 */
public class PackagerConformanceTest {

	/** Status of a cell */
	enum Status {
		/** packs the scenario, and the results are valid */
		SUPPORTED("S"),
		/** rejects the input */
		REJECTED("R"),
		/** accepts the input, but some results are invalid */
		GAP("G");

		private final String code;

		Status(String code) {
			this.code = code;
		}

		static String describe(String code) {
			switch (code) {
				case "S":
					return "✓";
				case "R":
					return "rejected";
				default:
					return "known gap";
			}
		}
	}

	private static final int SEEDS = 6;

	/** enough containers for every scenario, so that a packager which does not pack all boxes has a gap */
	private static final int CONTAINERS = 8;

	/** the packagers, configured for a scenario (full support) */
	private static final Map<String, Function<Spec, AbstractPackager<?>>> PACKAGERS = new LinkedHashMap<>();

	/** column titles of the feature support table */
	private static final Map<String, String> PACKAGER_TITLES = new LinkedHashMap<>();

	static {
		PACKAGERS.put("plain", spec -> PlainPackager.newBuilder()
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("laff", spec -> LargestAreaFitFirstPackager.newBuilder()
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("fastLaff", spec -> FastLargestAreaFitFirstPackager.newBuilder()
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("bruteForce", spec -> BruteForcePackager.newBuilder()
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("fastBruteForce", spec -> FastBruteForcePackager.newBuilder()
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("parallelBruteForce", spec -> ParallelBruteForcePackager.newBuilder()
				.withThreads(2)
				.withParallelizationCount(2)
				.withRequireFullSupport(spec.fullSupport)
				.build());
		PACKAGERS.put("composite", spec -> CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().withRequireFullSupport(spec.fullSupport).build())
				.withPackager(FastBruteForcePackager.newBuilder().withRequireFullSupport(spec.fullSupport).build(), 1000)
				.build());

		PACKAGER_TITLES.put("plain", "Plain");
		PACKAGER_TITLES.put("laff", "LAFF");
		PACKAGER_TITLES.put("fastLaff", "Fast LAFF");
		PACKAGER_TITLES.put("bruteForce", "Brute force");
		PACKAGER_TITLES.put("fastBruteForce", "Fast brute force");
		PACKAGER_TITLES.put("parallelBruteForce", "Parallel brute force");
		PACKAGER_TITLES.put("composite", "Composite");
	}

	/**
	 * Expected status per scenario, one column per packager in the order of {@link #PACKAGERS}:
	 * S = supported, R = rejected, G = known gap.
	 */
	private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

	static {
		//                                         plain laff fastLaff bruteForce fastBruteForce parallelBruteForce composite
		EXPECTED.put("boxItems",                   "S S S S S S S");
		EXPECTED.put("groups",                     "S S S S S S S");
		EXPECTED.put("chronological",              "S S S S S S S");
		EXPECTED.put("chronologicalGroups",        "S S S S S S S");
		EXPECTED.put("allowSkipping",              "S S S S S S S");
		EXPECTED.put("allowSkippingGroups",        "S S S S S S S");
		EXPECTED.put("containerPriorities",        "S S S S S S S");
		EXPECTED.put("containerPrioritiesGroups",  "S S S S S S S");
		EXPECTED.put("extractionOrder",            "S S S S S S S");
		EXPECTED.put("extractionOrderGroups",      "S S S S S S S");
		EXPECTED.put("frontAccess",                "S S S S S S S");
		EXPECTED.put("frontAccessGroups",          "S S S S S S S");
		EXPECTED.put("obstacles",                  "S S S S S S S");
		EXPECTED.put("obstaclesGroups",            "S S S S S S S");
		EXPECTED.put("obstaclesTwoContainerTypes", "S S S S S S S");
		EXPECTED.put("loadLimits",                 "S S S S S S S");
		EXPECTED.put("loadLimitsGroups",           "S S S S S S S");
		EXPECTED.put("chronologicalLoadLimits",    "S S S S S S S");
		EXPECTED.put("allowSkippingLoadLimits",    "S S S S S S S");
		EXPECTED.put("fullSupport",                "S S S S S S S");
		// LAFF: a box which is taller than its level goes into a new level on top, where it may only be fully supported
		// by the level's boxes; a group fails when a later box needs the floor beside them (seed 4)
		EXPECTED.put("fullSupportGroups",          "S G G S S S S");
		EXPECTED.put("manifestControls",           "S S S R R R S");
		EXPECTED.put("manifestControlsGroups",     "S S S R R R S");
	}

	/** row titles of the feature support table */
	private static final Map<String, String> SCENARIO_TITLES = new LinkedHashMap<>();

	static {
		SCENARIO_TITLES.put("boxItems", "Box items");
		SCENARIO_TITLES.put("groups", "Box item groups");
		SCENARIO_TITLES.put("chronological", "Box item order (`CHRONOLOGICAL`)");
		SCENARIO_TITLES.put("chronologicalGroups", "Box item order, groups");
		SCENARIO_TITLES.put("allowSkipping", "Box item order with skipping (`CHRONOLOGICAL_ALLOW_SKIPPING`)");
		SCENARIO_TITLES.put("allowSkippingGroups", "Box item order with skipping, groups");
		SCENARIO_TITLES.put("containerPriorities", "Container priorities");
		SCENARIO_TITLES.put("containerPrioritiesGroups", "Container priorities, groups");
		SCENARIO_TITLES.put("extractionOrder", "Extraction order");
		SCENARIO_TITLES.put("extractionOrderGroups", "Extraction order, groups");
		SCENARIO_TITLES.put("frontAccess", "Container access through a door (`FRONT`)");
		SCENARIO_TITLES.put("frontAccessGroups", "Container access through a door, groups");
		SCENARIO_TITLES.put("obstacles", "Obstacles");
		SCENARIO_TITLES.put("obstaclesGroups", "Obstacles, groups");
		SCENARIO_TITLES.put("obstaclesTwoContainerTypes", "Obstacles in one of two container types");
		SCENARIO_TITLES.put("loadLimits", "Box load limits");
		SCENARIO_TITLES.put("loadLimitsGroups", "Box load limits, groups");
		SCENARIO_TITLES.put("chronologicalLoadLimits", "Box load limits, box item order");
		SCENARIO_TITLES.put("allowSkippingLoadLimits", "Box load limits, box item order with skipping");
		SCENARIO_TITLES.put("fullSupport", "Full support (`withRequireFullSupport`)");
		SCENARIO_TITLES.put("fullSupportGroups", "Full support, groups");
		SCENARIO_TITLES.put("manifestControls", "Custom manifest controls");
		SCENARIO_TITLES.put("manifestControlsGroups", "Custom manifest controls, groups");
	}

	/** An input */
	private static class Spec {
		List<BoxItem> boxItems;
		List<BoxItemGroup> groups;
		Order order = Order.NONE;
		ContainerAccess access = ContainerAccess.ANY;
		/** x, y, z, dx, dy, dz */
		List<int[]> obstacles = new ArrayList<>();
		/** a second, smaller container type without obstacles */
		boolean secondContainerType;
		boolean load;
		/** the packager requires full support */
		boolean fullSupport;
		/** every second box is a fire hazard, and a manifest control allows one fire hazard (box or group) per container */
		boolean fireHazards;
	}

	private static final Map<String, Function<Random, Spec>> SCENARIOS = new LinkedHashMap<>();

	static {
		SCENARIOS.put("boxItems", random -> items(random));
		SCENARIOS.put("groups", random -> groups(random));
		SCENARIOS.put("chronological", random -> withOrder(items(random), Order.CHRONOLOGICAL));
		SCENARIOS.put("chronologicalGroups", random -> withOrder(groups(random), Order.CHRONOLOGICAL));
		SCENARIOS.put("allowSkipping", random -> withOrder(items(random), Order.CHRONOLOGICAL_ALLOW_SKIPPING));
		SCENARIOS.put("allowSkippingGroups", random -> withOrder(groups(random), Order.CHRONOLOGICAL_ALLOW_SKIPPING));
		SCENARIOS.put("containerPriorities", random -> {
			Spec spec = items(random);
			for (BoxItem boxItem : spec.boxItems) {
				boxItem.withContainerPriority(random.nextInt(2));
			}
			return spec;
		});
		SCENARIOS.put("containerPrioritiesGroups", random -> {
			Spec spec = groups(random);
			for (BoxItemGroup group : spec.groups) {
				group.withContainerPriority(random.nextInt(2));
			}
			return spec;
		});
		SCENARIOS.put("extractionOrder", random -> {
			Spec spec = items(random);
			spec.access = ContainerAccess.TOP;
			for (BoxItem boxItem : spec.boxItems) {
				boxItem.withExtractionOrder(1 + random.nextInt(3));
			}
			return spec;
		});
		SCENARIOS.put("extractionOrderGroups", random -> {
			Spec spec = groups(random);
			spec.access = ContainerAccess.TOP;
			for (BoxItemGroup group : spec.groups) {
				group.withExtractionOrder(1 + random.nextInt(3));
			}
			return spec;
		});
		SCENARIOS.put("frontAccess", random -> {
			Spec spec = items(random);
			spec.access = ContainerAccess.FRONT;
			return spec;
		});
		SCENARIOS.put("frontAccessGroups", random -> {
			Spec spec = groups(random);
			spec.access = ContainerAccess.FRONT;
			return spec;
		});
		SCENARIOS.put("obstacles", random -> withObstacle(items(random)));
		SCENARIOS.put("obstaclesGroups", random -> withObstacle(groups(random)));
		SCENARIOS.put("obstaclesTwoContainerTypes", random -> {
			Spec spec = withObstacle(items(random));
			spec.secondContainerType = true;
			return spec;
		});
		SCENARIOS.put("loadLimits", random -> items(random, true));
		SCENARIOS.put("loadLimitsGroups", random -> groups(random, true));
		SCENARIOS.put("chronologicalLoadLimits", random -> withOrder(items(random, true), Order.CHRONOLOGICAL));
		SCENARIOS.put("allowSkippingLoadLimits", random -> withOrder(items(random, true), Order.CHRONOLOGICAL_ALLOW_SKIPPING));
		SCENARIOS.put("fullSupport", random -> withFullSupport(items(random)));
		// more boxes, so that some are stacked
		SCENARIOS.put("fullSupportGroups", random -> withFullSupport(groups(random, false, false, 2, 2)));
		SCENARIOS.put("manifestControls", random -> items(random, false, true));
		SCENARIOS.put("manifestControlsGroups", random -> groups(random, false, true));
	}

	private static Box box(Random random, int index, boolean load, boolean fireHazard) {
		Box.Builder builder = Box.newBuilder()
				.withId("b" + index)
				.withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2))
				.withRotate2D()
				.withWeight(1 + random.nextInt(3));
		if(load) {
			// carries at most a little weight
			builder.withMaxLoadWeight(random.nextInt(3));
		}
		if(fireHazard) {
			builder.withProperty(MaxFireHazardBoxItemPerContainerManifestControls.KEY, Boolean.TRUE);
		}
		return builder.build();
	}

	private static Spec items(Random random) {
		return items(random, false);
	}

	private static Spec items(Random random, boolean load) {
		return items(random, load, false);
	}

	private static Spec items(Random random, boolean load, boolean fireHazards) {
		Spec spec = new Spec();
		spec.load = load;
		spec.fireHazards = fireHazards;
		spec.boxItems = new ArrayList<>();
		int count = 4 + random.nextInt(3);
		for (int i = 0; i < count; i++) {
			spec.boxItems.add(new BoxItem(box(random, i, load, fireHazards && i % 2 == 0), 1));
		}
		return spec;
	}

	private static Spec groups(Random random) {
		return groups(random, false);
	}

	private static Spec groups(Random random, boolean load) {
		return groups(random, load, false);
	}

	private static Spec groups(Random random, boolean load, boolean fireHazards) {
		return groups(random, load, fireHazards, 2, 1);
	}

	/**
	 * @param minGroups the minimum number of groups (at most one more)
	 * @param minBoxes the minimum number of boxes per group (at most one more)
	 */
	private static Spec groups(Random random, boolean load, boolean fireHazards, int minGroups, int minBoxes) {
		Spec spec = new Spec();
		spec.load = load;
		spec.fireHazards = fireHazards;
		spec.groups = new ArrayList<>();
		int index = 0;
		int groupCount = minGroups + random.nextInt(2);
		for (int g = 0; g < groupCount; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			int count = minBoxes + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				boxItems.add(new BoxItem(box(random, index, load, fireHazards && index % 2 == 0), 1));
				index++;
			}
			spec.groups.add(new BoxItemGroup("g" + g, boxItems));
		}
		return spec;
	}

	private static Spec withOrder(Spec spec, Order order) {
		spec.order = order;
		return spec;
	}

	private static Spec withFullSupport(Spec spec) {
		spec.fullSupport = true;
		return spec;
	}

	private static ManifestControlsBuilderFactory manifestControls(Spec spec) {
		if(spec.groups != null) {
			return MaxFireHazardBoxItemGroupsPerContainerManifestControls.newFactory(1);
		}
		return MaxFireHazardBoxItemPerContainerManifestControls.newFactory(1);
	}

	private static Spec withObstacle(Spec spec) {
		spec.obstacles.add(new int[] { 0, 0, 0, 2, 2, 1 });
		return spec;
	}

	private static List<ContainerItem> containerItems(Spec spec) {
		ContainerItem.Builder builder = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder().withId("c").withSize(6, 4, 3).withMaxLoadWeight(100).withAccess(spec.access).build(), CONTAINERS);
		if(spec.secondContainerType) {
			builder.withContainer(Container.newBuilder().withId("small").withSize(3, 3, 2).withMaxLoadWeight(100).withAccess(spec.access).build(), CONTAINERS);
		}
		List<ContainerItem> containerItems = builder.build();
		if(spec.fireHazards) {
			for (ContainerItem containerItem : containerItems) {
				containerItem.setManifestControlsBuilderFactory(manifestControls(spec));
			}
		}
		return containerItems;
	}

	private static PackagerResult pack(AbstractPackager<?> packager, Spec spec, List<ContainerItem> containerItems) {
		var builder = packager.newResultBuilder();
		if(spec.fireHazards) {
			for (ContainerItem containerItem : containerItems) {
				builder.withContainerItem(b -> b
						.withContainerItem(containerItem)
						.withManifestControlsBuilderFactory(manifestControls(spec)));
			}
		} else if(spec.obstacles.isEmpty()) {
			builder.withContainerItems(containerItems);
		} else {
			ContainerItem withObstacles = containerItems.get(0);
			builder.withContainerItem(b -> b
					.withContainerItem(withObstacles.getContainer(), withObstacles.getCount())
					.withObstacles(o -> {
						for (int[] obstacle : spec.obstacles) {
							o.withObstacle(obstacle[0], obstacle[1], obstacle[2], obstacle[3], obstacle[4], obstacle[5]);
						}
					}));
			for (int i = 1; i < containerItems.size(); i++) {
				ContainerItem containerItem = containerItems.get(i);
				builder.withContainerItem(b -> b.withContainerItem(containerItem));
			}
		}
		if(spec.boxItems != null) {
			builder.withBoxItems(spec.boxItems);
		} else {
			builder.withBoxItemGroups(spec.groups);
		}
		return builder
				.withOrder(spec.order)
				.withMaxContainerCount(CONTAINERS)
				.withInterruptDuration(10_000)
				.build();
	}

	/**
	 * @return null if the result is valid, otherwise why not
	 */
	private static String validate(DefaultValidator validator, Spec spec, List<ContainerItem> containerItems, PackagerResult result) {
		if(!result.isSuccess()) {
			return "not packed";
		}
		var builder = validator.newResultBuilder()
				.withContainerItems(containerItems)
				.withMaxContainerCount(CONTAINERS)
				.withOrder(spec.order)
				.withPackagerResult(result);
		if(spec.boxItems != null) {
			builder.withBoxItems(spec.boxItems);
		} else {
			builder.withBoxItemGroups(spec.groups);
		}
		ValidatorResult validatorResult = builder.build();
		if(!validatorResult.isValid()) {
			if(validatorResult.getReasons().isEmpty()) {
				// the order validator gives no reasons
				return "not in the box item order";
			}
			return "invalid: " + validatorResult.getReasons().get(0).getMessage();
		}
		for (Container container : result.getContainers()) {
			List<Placement> placements = container.getStack().getPlacements();
			for (Placement obstacle : container.getObstacles()) {
				for (Placement placement : placements) {
					if(placement.intersects3D(obstacle)) {
						return "box " + placement.getStackValue().getBox().getId() + " overlaps an obstacle";
					}
				}
			}
			if(spec.fullSupport) {
				List<ValidatorResultReason> reasons = new ArrayList<>();
				if(!new FullySupportedStabilityValidator().isValid(placements, reasons)) {
					return "full support: " + reasons.get(0).getMessage();
				}
			}
			if(spec.fireHazards) {
				int fireHazards = countFireHazards(spec, placements);
				if(fireHazards > 1) {
					return fireHazards + " fire hazards in a container";
				}
			}
			if(spec.load) {
				LoadValidator loadValidator = new DefaultLoadValidatorBuilder()
						.withPlacements(placements)
						.withContainer(container)
						.build();
				List<ValidatorResultReason> reasons = new ArrayList<>();
				if(loadValidator != null && !loadValidator.isValid(placements, reasons)) {
					return "load: " + reasons.get(0).getMessage();
				}
			}
		}
		return null;
	}

	/**
	 * @return the number of fire hazard boxes, or with groups the number of groups with a fire hazard box
	 */
	private static int countFireHazards(Spec spec, List<Placement> placements) {
		List<String> groups = new ArrayList<>();
		int count = 0;
		for (Placement placement : placements) {
			Box box = placement.getStackValue().getBox();
			Boolean fireHazard = box.getProperty(MaxFireHazardBoxItemPerContainerManifestControls.KEY);
			if(fireHazard == null || !fireHazard) {
				continue;
			}
			if(spec.groups == null) {
				count++;
			} else {
				String group = box.getBoxItem().getGroup().getId();
				if(!groups.contains(group)) {
					groups.add(group);
					count++;
				}
			}
		}
		return count;
	}

	private static final Map<String, Map<String, String>> ACTUAL = new TreeMap<>();

	static Stream<Arguments> cells() {
		List<Arguments> arguments = new ArrayList<>();
		for (String scenario : SCENARIOS.keySet()) {
			for (String packager : PACKAGERS.keySet()) {
				arguments.add(Arguments.of(scenario, packager));
			}
		}
		return arguments.stream();
	}

	@ParameterizedTest(name = "{0} {1}")
	@MethodSource("cells")
	public void conforms(String scenario, String packagerName) throws Exception {
		Status status = Status.SUPPORTED;
		String detail = null;
		try (DefaultValidator validator = new DefaultValidator()) {
			for (long seed = 0; seed < SEEDS && status == Status.SUPPORTED; seed++) {
				Spec spec = SCENARIOS.get(scenario).apply(new Random(seed));
				List<ContainerItem> containerItems = containerItems(spec);
				try (AbstractPackager<?> packager = PACKAGERS.get(packagerName).apply(spec)) {
					String reason = packager.getUnsupportedReason(new PackagerInput(spec.boxItems, spec.groups, containerItems, CONTAINERS, spec.order));
					if(reason != null) {
						status = Status.REJECTED;
						detail = reason;
						break;
					}
					try {
						PackagerResult result = pack(packager, spec, containerItems);
						String failure = validate(validator, spec, containerItems, result);
						if(failure != null) {
							status = Status.GAP;
							detail = "seed " + seed + ": " + failure;
						}
					} catch (RuntimeException e) {
						status = Status.GAP;
						detail = "seed " + seed + ": " + e;
					}
				}
			}
		}
		synchronized (ACTUAL) {
			ACTUAL.computeIfAbsent(scenario, k -> new TreeMap<>()).put(packagerName, status.code + (detail != null ? " (" + detail + ")" : ""));
		}

		String expected = EXPECTED.get(scenario);
		if(expected != null) {
			List<String> names = new ArrayList<>(PACKAGERS.keySet());
			String expectedCode = expected.split("\\s+")[names.indexOf(packagerName)];
			assertThat(status.code).as("%s %s: %s", scenario, packagerName, detail).isEqualTo(expectedCode);
		}
	}

	private static final String TABLE_START = "<!-- feature support table: generated from PackagerConformanceTest -->";

	private static final String TABLE_END = "<!-- end of feature support table -->";

	/**
	 * @return the expected table as markdown, one row per scenario and one column per packager
	 */
	static String getFeatureSupportTable() {
		StringBuilder builder = new StringBuilder("| Feature |");
		StringBuilder separator = new StringBuilder("| --- |");
		for (String title : PACKAGER_TITLES.values()) {
			builder.append(' ').append(title).append(" |");
			separator.append(" --- |");
		}
		builder.append('\n').append(separator).append('\n');
		for (Map.Entry<String, String> entry : EXPECTED.entrySet()) {
			builder.append("| ").append(SCENARIO_TITLES.get(entry.getKey())).append(" |");
			for (String code : entry.getValue().split("\\s+")) {
				builder.append(' ').append(Status.describe(code)).append(" |");
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	/**
	 * The feature support table of FEATURES.md is the expected table of this test (which every cell checks).
	 */
	@Test
	public void featuresTableIsUpToDate() throws IOException {
		assertThat(SCENARIO_TITLES.keySet()).containsExactlyElementsOf(SCENARIOS.keySet());
		assertThat(EXPECTED.keySet()).containsExactlyElementsOf(SCENARIOS.keySet());

		String features = new String(Files.readAllBytes(Paths.get("..", "FEATURES.md")), StandardCharsets.UTF_8).replace("\r\n", "\n");
		String table = getFeatureSupportTable();
		assertThat(features)
				.as("FEATURES.md table, replace with%n%s%n%s%s", TABLE_START, table, TABLE_END)
				.contains(TABLE_START + "\n" + table + TABLE_END);
	}

	@AfterAll
	public static void printTable() {
		StringBuilder builder = new StringBuilder("Packager conformance (S = supported, R = rejected, G = known gap)\n");
		for (String scenario : SCENARIOS.keySet()) {
			Map<String, String> row = ACTUAL.get(scenario);
			if(row == null) {
				continue;
			}
			builder.append(scenario).append(":");
			for (String packager : PACKAGERS.keySet()) {
				String value = row.get(packager);
				builder.append(' ').append(value == null ? "-" : value.substring(0, 1));
			}
			builder.append('\n');
			for (String packager : PACKAGERS.keySet()) {
				String value = row.get(packager);
				if(value != null && value.length() > 1) {
					builder.append("    ").append(packager).append(": ").append(value).append('\n');
				}
			}
		}
		System.out.println(builder);
	}
}
