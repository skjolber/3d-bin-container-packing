package com.github.skjolber.packing.jmh.constraint;

import java.util.List;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

/**
 * JMH state for the {@code maxLoadPressure} constraint benchmark.
 * <p>
 * Scenario: 20 boxes (4 columns × 5 layers of 10×10×1) packed into a single
 * 20×20×5 container.  Each box weighs 2 and declares {@code maxLoadPressure=1}
 * on a 10×10 base (area=100), giving a per-placement weight limit of 100.
 * Each placement adds 2 units — well within the limit — so all 20 boxes fit
 * and the pressure check fires on every placement without triggering rejection.
 *
 * <pre>
 *   Top view (20×20 base — 4 columns of 10×10):
 *    y
 *   20 +----------+----------+
 *      |  col 3   |  col 4   |
 *   10 +----------+----------+
 *      |  col 1   |  col 2   |
 *    0 +----------+----------+
 *                            20 x
 *
 *   Side view (one column, 5 layers):
 *    z
 *    5 +----------+  w=2  pressure check: 2 ≤ maxLoadPressure×area = 1×100 = 100  ✓
 *    4 +----------+  w=2
 *    3 +----------+  w=2
 *    2 +----------+  w=2
 *    1 +----------+  w=2  10×10 base (area=100), maxLoadPressure=1
 *    0
 *        0        10 x
 * </pre>
 */
@State(Scope.Benchmark)
public class PressureConstraintBenchmarkState {

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> items;

	/** Scenario 2: two box types with different {@code maxLoadPressure} limits. */
	private List<ContainerItem> containers2;
	private List<BoxItem> items2;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();

		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("pressure-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box box = Box.newBuilder().withId("pressure-box")
				.withSize(10, 10, 1).withWeight(2).withMaxLoadPressure(1).build();
		items = List.of(new BoxItem(box, 20));

		buildMixedScenario();
	}

	/**
	 * Scenario 2: two box types with different {@code maxLoadPressure} limits
	 * in the same 20×20×5 container.
	 * <ul>
	 *   <li><b>tight</b>: w=3, maxLoadPressure=0.1 — area=100, maxWeight=10; 3&lt;10 ✓</li>
	 *   <li><b>generous</b>: w=3, maxLoadPressure=1 — area=100, maxWeight=100; 3&lt;100 ✓</li>
	 * </ul>
	 * The packager evaluates two different pressure limits per placement candidate.
	 *
	 * <pre>
	 *   z
	 *   5 +----------+  tight: 3 ≤ 0.1×100 = 10  ✓   generous: 3 ≤ 1×100 = 100  ✓
	 *   4 +----------+
	 *   3 +----------+
	 *   2 +----------+
	 *   1 +----------+  tight: maxLoadPressure=0.1  /  generous: maxLoadPressure=1
	 *   0
	 *       0        10 x  (10×10 base, area=100; ×4 columns — 2 tight, 2 generous)
	 * </pre>
	 */
	private void buildMixedScenario() {
		containers2 = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("pressure-mixed-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box tight = Box.newBuilder().withId("pressure-tight")
				.withSize(10, 10, 1).withWeight(3).withMaxLoadPressure(0.1).build();
		Box generous = Box.newBuilder().withId("pressure-generous")
				.withSize(10, 10, 1).withWeight(3).withMaxLoadPressure(1).build();
		items2 = List.of(new BoxItem(tight, 10), new BoxItem(generous, 10));
	}

	@TearDown(Level.Trial)
	public void shutdown() {
		plainPackager.close();
		laffPackager.close();
	}

	public PlainPackager getPlainPackager() {
		return plainPackager;
	}

	public LargestAreaFitFirstPackager getLaffPackager() {
		return laffPackager;
	}

	public List<ContainerItem> getContainers() {
		return containers;
	}

	public List<BoxItem> getItems() {
		return items;
	}

	public List<ContainerItem> getContainers2() {
		return containers2;
	}

	public List<BoxItem> getItems2() {
		return items2;
	}
}
