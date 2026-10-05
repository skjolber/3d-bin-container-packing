package com.github.skjolber.packing.visualizer.packaging;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * A showcase for the viewer: writes one scenario after another to the viewer's {@code containers.json}, with a delay
 * between them, so that the features can be seen in turn. Start the viewer ({@code npm start} in
 * {@code visualizer/viewer}), then run this test by hand, for example with
 * <pre>
 * ./mvnw -B -ntp -Pdev -pl visualizer/packaging -am -Dtest=ShowcaseVisualizationTest -Dsurefire.failIfNoSpecifiedTests=false -Dshowcase.delay=20 test
 * </pre>
 * System properties: {@code showcase.delay}, seconds per scenario (default 15), and {@code showcase.rounds}, the number
 * of times to cycle through the scenarios (default 1).
 */
public class ShowcaseVisualizationTest extends AbstractPackagerTest {

	@FunctionalInterface
	private interface Scenario {
		void write() throws Exception;
	}

	@Test
	void showcase() throws Exception {
		long delay = Long.getLong("showcase.delay", 15) * 1000;
		int rounds = Integer.getInteger("showcase.rounds", 1);

		// what to look at, and the scenario
		Map<String, Scenario> scenarios = new LinkedHashMap<>();
		scenarios.put("Load limits: boxes which carry at most a given weight; hover a box for its load and supporters",
				() -> new WeightConstraintVisualizationTest().plainPackager());
		scenarios.put("Validation: an overloaded column; invalid boxes are outlined in red, with the reasons in the summary",
				() -> new InvalidResultVisualizationTest().overloadedColumn());
		scenarios.put("Groups and support: press C for the group, support and load colour modes; the sphere is the centre of gravity",
				() -> new SupportAndGroupsVisualizationTest().groupsWithSupport());
		scenarios.put("Insertion order through a door (orange): press R for the search order (invalid) and the insertion order; step with A and D",
				() -> new InsertionOrderVisualizationTest().searchOrderAndInsertionOrder());
		scenarios.put("Container costs: press R; with costs, two cheap containers instead of one expensive",
				() -> new ContainerCostVisualizationTest().withAndWithoutCosts());
		scenarios.put("Virtual boxes: press R; identical boxes grouped into layouts before packing",
				() -> new VirtualBoxVisualizationTest().directAndWithVirtualBoxes());
		scenarios.put("Comparing packagers: press R or click a row; the composite packager fits the order in a smaller container",
				() -> new PackagerComparisonVisualizationTest().compareOnOneOrder());

		for (int round = 0; round < rounds; round++) {
			int index = 1;
			for (Map.Entry<String, Scenario> entry : scenarios.entrySet()) {
				System.out.println("Showcase " + index++ + "/" + scenarios.size() + ": " + entry.getKey());
				entry.getValue().write();
				Thread.sleep(delay);
			}
		}
	}
}
