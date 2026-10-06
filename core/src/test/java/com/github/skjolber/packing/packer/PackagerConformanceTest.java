package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterAll;
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
import com.github.skjolber.packing.api.validator.ValidatorResult;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.api.validator.placement.LoadValidator;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.LoadBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.LoadFastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.LoadParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBoxItemBruteForcePackager;
import com.github.skjolber.packing.packer.composite.CompositePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.validator.DefaultValidator;
import com.github.skjolber.packing.validator.load.DefaultLoadValidatorBuilder;

/**
 * Every packager either packs each input feature correctly, or rejects the input: no packager silently ignores a
 * feature. Each cell (scenario, packager) has an expected status; a known gap must still fail, so that the table is
 * updated when a gap is closed.
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
	}

	private static final int SEEDS = 6;

	/** enough containers for every scenario, so that a packager which does not pack all boxes has a gap */
	private static final int CONTAINERS = 8;

	private static final Map<String, Supplier<AbstractPackager<?>>> PACKAGERS = new LinkedHashMap<>();

	static {
		PACKAGERS.put("plain", () -> PlainPackager.newBuilder().build());
		PACKAGERS.put("laff", () -> LargestAreaFitFirstPackager.newBuilder().build());
		PACKAGERS.put("fastLaff", () -> FastLargestAreaFitFirstPackager.newBuilder().build());
		PACKAGERS.put("bruteForce", () -> BruteForcePackager.newBuilder().build());
		PACKAGERS.put("fastBruteForce", () -> FastBruteForcePackager.newBuilder().build());
		PACKAGERS.put("parallelBruteForce", () -> ParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		PACKAGERS.put("loadBruteForce", () -> LoadBruteForcePackager.newBuilder().build());
		PACKAGERS.put("loadFastBruteForce", () -> LoadFastBruteForcePackager.newBuilder().build());
		PACKAGERS.put("loadParallelBruteForce", () -> LoadParallelBoxItemBruteForcePackager.newBuilder().withThreads(2).withParallelizationCount(2).build());
		PACKAGERS.put("composite", () -> CompositePackager.newBuilder()
				.withPackager(PlainPackager.newBuilder().build())
				.withPackager(FastBruteForcePackager.newBuilder().build(), 1000)
				.build());
	}

	/**
	 * Expected status per scenario, one column per packager in the order of {@link #PACKAGERS}:
	 * S = supported, R = rejected, G = known gap.
	 */
	private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

	static {
		//                                         plain laff fastLaff bruteForce fastBruteForce parallelBruteForce loadBruteForce loadFastBruteForce loadParallelBruteForce composite
		EXPECTED.put("boxItems",                   "S S S S S S S S S S");
		EXPECTED.put("groups",                     "S S S S S S S S S S");
		EXPECTED.put("chronological",              "S S S S S S S S S S");
		EXPECTED.put("chronologicalGroups",        "S S S S S S S S S S");
		// fast brute force: skipping is not supported
		EXPECTED.put("allowSkipping",              "S S S S R S S R S S");
		EXPECTED.put("allowSkippingGroups",        "S S S S R S S R S S");
		EXPECTED.put("containerPriorities",        "S S S S S S S S S S");
		EXPECTED.put("containerPrioritiesGroups",  "S S S S S S S S S S");
		EXPECTED.put("extractionOrder",            "S S S S S S S S S S");
		EXPECTED.put("extractionOrderGroups",      "S S S S S S S S S S");
		EXPECTED.put("frontAccess",                "S S S S S S S S S S");
		EXPECTED.put("frontAccessGroups",          "S S S S S S S S S S");
		EXPECTED.put("obstacles",                  "S S S S S S S S S S");
		EXPECTED.put("obstaclesGroups",            "S S S S S S S S S S");
		EXPECTED.put("obstaclesTwoContainerTypes", "S S S S S S S S S S");
		EXPECTED.put("loadLimits",                 "S S S R R R S S S S");
		// LAFF: a box taller than its level starts a new level, where it must rest on the boxes below (load limits);
		// it is not placed when they cannot carry it, although it would fit beside them on the level's floor
		EXPECTED.put("loadLimitsGroups",           "S G G R R R S S S S");
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
	}

	private static Box box(Random random, int index, boolean load) {
		Box.Builder builder = Box.newBuilder()
				.withId("b" + index)
				.withSize(1 + random.nextInt(3), 1 + random.nextInt(3), 1 + random.nextInt(2))
				.withRotate2D()
				.withWeight(1 + random.nextInt(3));
		if(load) {
			// carries at most a little weight
			builder.withMaxLoadWeight(random.nextInt(3));
		}
		return builder.build();
	}

	private static Spec items(Random random) {
		return items(random, false);
	}

	private static Spec items(Random random, boolean load) {
		Spec spec = new Spec();
		spec.load = load;
		spec.boxItems = new ArrayList<>();
		int count = 4 + random.nextInt(3);
		for (int i = 0; i < count; i++) {
			spec.boxItems.add(new BoxItem(box(random, i, load), 1));
		}
		return spec;
	}

	private static Spec groups(Random random) {
		return groups(random, false);
	}

	private static Spec groups(Random random, boolean load) {
		Spec spec = new Spec();
		spec.load = load;
		spec.groups = new ArrayList<>();
		int index = 0;
		int groupCount = 2 + random.nextInt(2);
		for (int g = 0; g < groupCount; g++) {
			List<BoxItem> boxItems = new ArrayList<>();
			int count = 1 + random.nextInt(2);
			for (int i = 0; i < count; i++) {
				boxItems.add(new BoxItem(box(random, index++, load), 1));
			}
			spec.groups.add(new BoxItemGroup("g" + g, boxItems));
		}
		return spec;
	}

	private static Spec withOrder(Spec spec, Order order) {
		spec.order = order;
		return spec;
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
		return builder.build();
	}

	private static PackagerResult pack(AbstractPackager<?> packager, Spec spec, List<ContainerItem> containerItems) {
		var builder = packager.newResultBuilder();
		if(spec.obstacles.isEmpty()) {
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
		try (DefaultValidator validator = new DefaultValidator(); AbstractPackager<?> packager = PACKAGERS.get(packagerName).get()) {
			for (long seed = 0; seed < SEEDS && status == Status.SUPPORTED; seed++) {
				Spec spec = SCENARIOS.get(scenario).apply(new Random(seed));
				List<ContainerItem> containerItems = containerItems(spec);
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
