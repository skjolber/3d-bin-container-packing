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
 * JMH state for the {@code maxLoadWeight} constraint benchmark.
 * <p>
 * Scenario: 20 boxes (4 columns × 5 layers of 10×10×1) packed into a single
 * 20×20×5 container.  Each box weighs 5 and declares {@code maxLoadWeight=100},
 * so the maximum accumulated weight above any base box (4 boxes × 5 = 20) is
 * well within the limit — all boxes fit and the constraint is evaluated on
 * every placement without ever triggering rejection.
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
 *    5 +----------+  w=5  cumulative load on base = 20 ≤ maxLoadWeight=100  ✓
 *    4 +----------+  w=5
 *    3 +----------+  w=5
 *    2 +----------+  w=5
 *    1 +----------+  w=5  maxLoadWeight=100
 *    0
 *        0        10 x
 * </pre>
 */
@State(Scope.Benchmark)
public class WeightConstraintBenchmarkState {

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> items;

	/** Scenario 2: two box types with different {@code maxLoadWeight} limits. */
	private List<ContainerItem> containers2;
	private List<BoxItem> items2;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();

		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("weight-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box box = Box.newBuilder().withId("weight-box")
				.withSize(10, 10, 1).withWeight(5).withMaxLoadWeight(100).build();
		items = List.of(new BoxItem(box, 20));

		buildMixedScenario();
	}

	/**
	 * Scenario 2: two box types with different {@code maxLoadWeight} limits in
	 * the same 20×20×5 container.
	 * <ul>
	 *   <li><b>tight</b>: w=3, maxLoadWeight=12 — 4 boxes may stack above (4×3=12, at limit)</li>
	 *   <li><b>generous</b>: w=3, maxLoadWeight=100 — same footprint, much higher limit</li>
	 * </ul>
	 * The packager must evaluate two different limit values per placement candidate.
	 *
	 * <pre>
	 *   z
	 *   5 +----------+  cumulative above tight base = 4×3=12 ≤ maxLoadWeight=12  ✓ (at limit)
	 *   4 +----------+
	 *   3 +----------+
	 *   2 +----------+
	 *   1 +----------+  tight: maxLoadWeight=12  /  generous: maxLoadWeight=100
	 *   0
	 *       0        10 x  (×4 columns — 2 tight, 2 generous)
	 * </pre>
	 */
	private void buildMixedScenario() {
		containers2 = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("weight-mixed-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box tight = Box.newBuilder().withId("weight-tight")
				.withSize(10, 10, 1).withWeight(3).withMaxLoadWeight(12).build();
		Box generous = Box.newBuilder().withId("weight-generous")
				.withSize(10, 10, 1).withWeight(3).withMaxLoadWeight(100).build();
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
