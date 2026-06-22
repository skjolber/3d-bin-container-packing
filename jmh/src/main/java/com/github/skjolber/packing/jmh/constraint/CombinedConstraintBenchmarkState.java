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
 * JMH state for the combined-constraint benchmark (all four constraints active).
 * <p>
 * Scenario: 20 boxes (4 columns × 5 layers of 10×10×1) packed into a single
 * 20×20×5 container.  Each box declares all four constraints with generous
 * limits so every box fits and all constraint checks fire on every placement
 * without triggering rejection.
 * <ul>
 *   <li>{@code maxLoadWeight=100}  — max accumulated weight above base:
 *       4×w=8, well below 100</li>
 *   <li>{@code maxLoadPressure=1}  — per-placement: 2 ≤ 1×100 (area=100)</li>
 *   <li>{@code maxLoadBoxCount=4}  — column depth: 4 ≤ 4 (at limit)</li>
 * </ul>
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
 *    5 +----------+  w=2  weight ok (8≤100) · pressure ok (2≤100) · count ok (4≤4)
 *    4 +----------+  w=2
 *    3 +----------+  w=2
 *    2 +----------+  w=2
 *    1 +----------+  w=2  maxLoadWeight=100, maxLoadPressure=1, maxLoadBoxCount=4
 *    0
 *        0        10 x   (10×10 base area=100)
 * </pre>
 */
@State(Scope.Benchmark)
public class CombinedConstraintBenchmarkState {

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> items;

	/** Scenario 2: two box types sharing the same container with distinct constraint budgets. */
	private List<ContainerItem> containers2;
	private List<BoxItem> items2;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();

		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("combined-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box box = Box.newBuilder().withId("combined-box")
				.withSize(10, 10, 1).withWeight(2)
				.withMaxLoadWeight(100).withMaxLoadPressure(1).withMaxLoadBoxCount(4)
				.build();
		items = List.of(new BoxItem(box, 20));

		buildMixedScenario();
	}

	/**
	 * Scenario 2: two box types with different budgets for every constraint in
	 * the same 20×20×5 container.
	 * <ul>
	 *   <li><b>tight</b>: w=2, maxLoadWeight=8, maxLoadPressure=0.1, maxLoadBoxCount=4
	 *       — all limits tight; 4 boxes above (w=8, pressure=2&lt;10, count=4)</li>
	 *   <li><b>generous</b>: w=2, maxLoadWeight=100, maxLoadPressure=1, maxLoadBoxCount=8
	 *       — all limits generous; same 4 boxes above are well within budget</li>
	 * </ul>
	 * The packager evaluates four distinct constraint types on every candidate placement.
	 *
	 * <pre>
	 *       col-T1   col-T2   col-G1   col-G2
	 *   z=5 [ T ]    [ T ]    [ G ]    [ G ]   weight: 4×2=8 ≤ 8 ✓
	 *   z=4 [ T ]    [ T ]    [ G ]    [ G ]   pressure: 2 ≤ 0.1×100=10 ✓
	 *   z=3 [ T ]    [ T ]    [ G ]    [ G ]   count: 4 ≤ 4 ✓  (tight, at limit)
	 *   z=2 [ T ]    [ T ]    [ G ]    [ G ]
	 *   z=1 [ T ]    [ T ]    [ G ]    [ G ]   T=tight / G=generous
	 *       x=0-10  x=10-20  x=0-10  x=10-20  (y: 0-10 = tight, 10-20 = generous)
	 * </pre>
	 */
	private void buildMixedScenario() {
		containers2 = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("combined-mixed-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box tight = Box.newBuilder().withId("combined-tight")
				.withSize(10, 10, 1).withWeight(2)
				.withMaxLoadWeight(8).withMaxLoadPressure(0.1).withMaxLoadBoxCount(4)
				.build();
		Box generous = Box.newBuilder().withId("combined-generous")
				.withSize(10, 10, 1).withWeight(2)
				.withMaxLoadWeight(100).withMaxLoadPressure(1).withMaxLoadBoxCount(8)
				.build();
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
