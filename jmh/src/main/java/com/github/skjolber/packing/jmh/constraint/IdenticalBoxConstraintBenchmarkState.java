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
 * JMH state for the {@code maxLoadIdenticalBoxCount} constraint benchmark.
 * <p>
 * Scenario: 20 identical boxes (4 columns × 5 layers of 10×10×1) packed into
 * a single 20×20×5 container.  Each box declares
 * {@code maxLoadIdenticalBoxCount=4}, so each 5-layer column reaches the limit
 * exactly (identical chain depth=4 ≤ 4).  The type-identity and depth checks
 * fire on every stacking attempt without ever triggering rejection.
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
 *   Side view (one column, 5 identical boxes):
 *    z
 *    5 +----------+  A5 — 4th identical above A1  depth=4 ≤ 4  ✓  (at limit)
 *    4 +----------+  A4 — depth=3  ✓
 *    3 +----------+  A3 — depth=2  ✓
 *    2 +----------+  A2 — depth=1  ✓
 *    1 +----------+  A1  maxLoadIdenticalBoxCount=4
 *    0
 *        0        10 x
 * </pre>
 */
@State(Scope.Benchmark)
public class IdenticalBoxConstraintBenchmarkState {

	private PlainPackager plainPackager;
	private LargestAreaFitFirstPackager laffPackager;

	private List<ContainerItem> containers;
	private List<BoxItem> items;

	/** Scenario 2: type-B (base, no identical restriction) and type-A (identical-only, tight limit) stacked above. */
	private List<ContainerItem> containers2;
	private List<BoxItem> items2;

	@Setup(Level.Trial)
	public void init() {
		plainPackager = PlainPackager.newBuilder().build();
		laffPackager = LargestAreaFitFirstPackager.newBuilder().build();

		containers = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("identical-container")
						.withEmptyWeight(0)
						.withSize(20, 20, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box box = Box.newBuilder().withId("identical-box")
				.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(4).build();
		items = List.of(new BoxItem(box, 20));

		buildMixedScenario();
	}

	/**
	 * Scenario 2: two box types in the same 20×10×5 container (2 columns of 10×10).
	 * <ul>
	 *   <li><b>type-B</b> (base layer): weight=2, {@code maxLoadBoxCount=8} — no type restriction;
	 *       4 boxes above ≤ 8 ✓. Heavier so it is placed first at floor positions.</li>
	 *   <li><b>type-A</b> (upper layers): weight=1, {@code maxLoadIdenticalBoxCount=3} — only A
	 *       above A; depth=3 ≤ 3 ✓ (at limit).</li>
	 * </ul>
	 * B is placed at floor level (z=0) in both columns; A stacks above B and above A.
	 * The packager evaluates both the identical-type check (for A above A) and the box-count
	 * check (for A above B) on every placement.
	 *
	 * <pre>
	 *        col-1    col-2
	 *   z=4 [ A ]    [ A ]   A: depth=3 ≤ 3 ✓ (at limit)
	 *   z=3 [ A ]    [ A ]   A: depth=2 ≤ 3 ✓
	 *   z=2 [ A ]    [ A ]   A: depth=1 ≤ 3 ✓
	 *   z=1 [ A ]    [ A ]   A above B — type-B has no identical restriction → allowed ✓
	 *   z=0 [ B ]    [ B ]   B: maxLoadBoxCount=8, 4 boxes above ≤ 8 ✓
	 *       x=0-10  x=10-20  (container 20×10×5, two columns)
	 * </pre>
	 */
	private void buildMixedScenario() {
		containers2 = ContainerItem.newListBuilder()
				.withContainer(Container.newBuilder()
						.withDescription("identical-mixed-container")
						.withEmptyWeight(0)
						.withSize(20, 10, 5)
						.withMaxLoadWeight(1_000_000)
						.build(), 1)
				.build();

		Box typeA = Box.newBuilder().withId("identical-A")
				.withSize(10, 10, 1).withWeight(1).withMaxLoadIdenticalBoxCount(3).build();
		Box typeB = Box.newBuilder().withId("identical-B")
				.withSize(10, 10, 1).withWeight(2).withMaxLoadBoxCount(8).build();
		items2 = List.of(new BoxItem(typeB, 2), new BoxItem(typeA, 8));
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
