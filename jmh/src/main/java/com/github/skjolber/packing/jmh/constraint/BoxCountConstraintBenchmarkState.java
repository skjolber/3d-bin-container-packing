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
 * JMH state for the {@code maxLoadBoxCount} constraint benchmark.
 * <p>
 * Scenario: 20 boxes (4 columns × 5 layers of 10×10×1) packed into a single
 * 20×20×5 container.  Each box declares {@code maxLoadBoxCount=4}, allowing
 * exactly 4 boxes above any base box — each 5-layer column reaches the limit
 * exactly (depth=4 ≤ 4).  The count check fires on every placement above a
 * constrained box without ever triggering rejection.
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
 *    5 +----------+  depth=4  ✓  (at limit — 4 boxes above the base)
 *    4 +----------+  depth=3  ✓
 *    3 +----------+  depth=2  ✓
 *    2 +----------+  depth=1  ✓
 *    1 +----------+  maxLoadBoxCount=4
 *    0
 *        0        10 x
 * </pre>
 */
@State(Scope.Benchmark)
public class BoxCountConstraintBenchmarkState {

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> items;

	/** Scenario 2: two box types with different {@code maxLoadBoxCount} limits. */
	private List<ContainerItem> containers2;
	private List<BoxItem> items2;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();

		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("boxcount-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box box = Box.newBuilder().withId("boxcount-box")
				.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(4).build();
		items = List.of(new BoxItem(box, 20));

		buildMixedScenario();
	}

	/**
	 * Scenario 2: two box types with different {@code maxLoadBoxCount} limits
	 * in the same 20×20×5 container.
	 * <ul>
	 *   <li><b>tight</b>: maxLoadBoxCount=4 — exactly 4 above allowed, col of 5</li>
	 *   <li><b>generous</b>: maxLoadBoxCount=8 — up to 8 above allowed, col of 5 well within</li>
	 * </ul>
	 * The packager must look up a different count limit for each box type.
	 *
	 * <pre>
	 *   z
	 *   5 +----------+  tight: depth=4 ≤ 4  ✓ (at limit)   generous: depth=4 ≤ 8  ✓
	 *   4 +----------+
	 *   3 +----------+
	 *   2 +----------+
	 *   1 +----------+  tight: maxLoadBoxCount=4  /  generous: maxLoadBoxCount=8
	 *   0
	 *       0        10 x  (×4 columns — 2 tight, 2 generous)
	 * </pre>
	 */
	private void buildMixedScenario() {
		containers2 = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("boxcount-mixed-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box tight = Box.newBuilder().withId("boxcount-tight")
				.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(4).build();
		Box generous = Box.newBuilder().withId("boxcount-generous")
				.withSize(10, 10, 1).withWeight(1).withMaxLoadBoxCount(8).build();
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
