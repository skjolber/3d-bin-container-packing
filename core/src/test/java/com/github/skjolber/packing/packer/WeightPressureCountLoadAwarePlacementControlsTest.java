package com.github.skjolber.packing.packer;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.VolumeWeightAreaMinZPlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

/**
 * Tests for {@link WeightPressureCountLoadAwarePlacementControls}, which extends
 * weight-based load checking with pressure (weight-per-area) and box-count constraints.
 */
public class WeightPressureCountLoadAwarePlacementControlsTest {

	private DefaultBoxItemSource boxItems;
	private DefaultPointCalculator3D pointCalculator;
	private PointControls pointControls;
	private Container container;
	private Stack stack;
	private Order order = Order.NONE;
	private PlacementComparator placementComparator;
	private Comparator<BoxItem> boxItemComparator;
	private boolean fullSupport = false;
	private WeightPressureCountLoadAwarePlacementControls ctrl;

	@BeforeEach
	public void setup() {
		boxItems = new DefaultBoxItemSource();
		boxItems.setValues(new ArrayList<BoxItem>());

		pointCalculator = new DefaultPointCalculator3D(false, 10);
		pointCalculator.clearToSize(10, 10, 10);

		pointControls = new DefaultPointControls(pointCalculator);

		stack = new Stack();

		boxItemComparator = new VolumeThenWeightBoxItemComparator();
		placementComparator = new VolumeWeightAreaMinZPlacementComparator();

		container = Container.newBuilder().withSize(10, 10, 10).withMaxLoadWeight(1000).build();

		ctrl = new WeightPressureCountLoadAwarePlacementControls(
			boxItems, pointControls, pointCalculator, container, stack,
			order, placementComparator, boxItemComparator, fullSupport
		);

		ctrl.initialize(10);
	}

	// --- Weight limit tests (inherited from weight-only behaviour) ----------------

	/**
	 * A 10×10×1 floor tile (maxLoadWeight=5) has a candidate of the same
	 * footprint but w=6 placed on top — weight limit exceeded.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *  |  | tooHeavy |   ← tooHeavy (10×10, w=6): REJECTED — w=6 > maxLoadWeight=5
	 *  1  +----------+
	 *  0  +----------+   ← bottom (10×10, w=1, maxLoadWeight=5)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testAboveWeightLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(5).build();
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box tooHeavy = Box.newBuilder().withSize(10, 10, 1).withWeight(6).build();
		boxItems.add(new BoxItem(tooHeavy));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	/**
	 * Same floor tile; candidate w=1 is within the weight budget.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+
	 *  |  |  light   |   ← light (10×10, w=1): ACCEPTED — w=1 ≤ maxLoadWeight=5
	 *  1  +----------+
	 *  0  +----------+   ← bottom (10×10, w=1, maxLoadWeight=5)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testBelowWeightLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(5).build();
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box light = Box.newBuilder().withSize(10, 10, 1).withWeight(1).build();
		boxItems.add(new BoxItem(light));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	/**
	 * A tiny bottom (2×2) supports a heavy overhang (10×10×9, w=200).
	 * Any new box at floor level falls under the overhang and receives an
	 * effective weight share that exceeds its maxLoadWeight=1.
	 *
	 * <pre>
	 *  z
	 *  |
	 * 10  +----------+
	 *  |  |          |
	 *  |  | overhang |   10×10×9, w=200
	 *  |  |          |
	 *  1  +-+--------+   ← overhang rests on bottom at z=1
	 *  0  +-+            ← bottom (2×2, mLW=200) | candidate (1×1, mLW=1) here
	 *     0 2         10  x
	 *
	 *  candidate bears part of overhang load
	 *  effective weight > maxLoadWeight=1 → REJECTED
	 * </pre>
	 */
	@Test
	public void testSupporteeMaxWeightLimitReached() {
		Box bottom = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withMaxLoadWeight(200).withId("First").build();
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box overhang = Box.newBuilder().withSize(10, 10, 9).withWeight(200).withId("Second").build();
		Placement overhangPlacement = new Placement(overhang.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), overhangPlacement);
		stack.add(overhangPlacement);
		wholeButtom.addLoad(overhangPlacement, 4, overhang.getWeight());

		Box tooLowMaxLoadLimit = Box.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).withWeight(1).build();
		boxItems.add(new BoxItem(tooLowMaxLoadLimit));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	/**
	 * Same geometry as above, but the candidate's maxLoadWeight=300 is large
	 * enough to absorb its share of the overhang.
	 *
	 * <pre>
	 *  z
	 *  |
	 * 10  +----------+
	 *  |  |          |
	 *  |  | overhang |   10×10×9, w=200
	 *  |  |          |
	 *  1  +-+--------+   ← overhang rests on bottom at z=1
	 *  0  +-+            ← bottom (2×2, mLW=200) | candidate (1×1, mLW=300) here
	 *     0 2         10  x
	 *
	 *  effective weight ≤ maxLoadWeight=300 → ACCEPTED
	 * </pre>
	 */
	@Test
	public void testSupporteeMaxWeightLimitNotReached() {
		Box bottom = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withMaxLoadWeight(200).withId("First").build();
		Placement buttomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, buttomPlacement);
		stack.add(buttomPlacement);

		Box overhang = Box.newBuilder().withSize(10, 10, 9).withWeight(200).withId("Second").build();
		Placement overhangPlacement = new Placement(overhang.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), overhangPlacement);
		stack.add(overhangPlacement);
		buttomPlacement.addLoad(overhangPlacement, 4, overhang.getWeight());

		Box highLimit = Box.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(300).withWeight(1).build();
		boxItems.add(new BoxItem(highLimit));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	/**
	 * Three-level stack: floor carries level-2, level-2 carries the candidate.
	 * The floor's maxLoadWeight=2 already has 1 unit consumed by level-2, so
	 * only 1 unit of capacity remains for the candidate.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  [  cand  ]      ← candidate at z=2
	 *  2  +----------+
	 *  |  |  level2  |   10×10×1, w=1 (already placed)
	 *  1  +----------+
	 *  0  +----------+   ← floor (10×10, w=1, maxLoadWeight=2)
	 *     0          10  x
	 *
	 *  floor.maxLoadWeight=2; level-2 already occupies 1 unit of capacity
	 *  candidate w=2: floor total = 1+2=3 > 2 → REJECTED
	 *  candidate w=1: floor total = 1+1=2 ≤ 2 → ACCEPTED
	 * </pre>
	 */
	@Test
	public void testWeightLimitForChained() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(2).withId("First").build();
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box wholeLevel2 = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withId("Second").build();
		Placement wholeLevel2Placement = new Placement(wholeLevel2.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), wholeLevel2Placement);
		stack.add(wholeLevel2Placement);
		wholeButtom.addLoad(wholeLevel2Placement, 100, wholeLevel2.getWeight());

		Box tooHeavy = Box.newBuilder().withSize(1, 1, 1).withWeight(2).build();
		boxItems.add(new BoxItem(tooHeavy));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);

		Box notTooHeavy = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
		boxItems.remove(0);
		boxItems.add(new BoxItem(notTooHeavy));

		placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	/**
	 * A corner box (2×2×1) already bears the full load of a large overhang
	 * (10×10×8, w=100). Because of this relief, the floor's remaining capacity
	 * is not consumed by the overhang, and a new candidate is still accepted.
	 *
	 * <pre>
	 *  z
	 *  |
	 * 10  +----------+
	 *  |  |          |
	 *  |  | overhang |   10×10×8, w=100
	 *  |  |          |
	 *  2  +-+--------+   ← overhang rests on corner at z=2
	 *  1  +-+            ← corner (2×2, w=1) | candidate (1×1, mLW=1000) here
	 *  0  +----------+   ← floor (10×10, w=1, maxLoadWeight=102)
	 *     0 2         10  x
	 *
	 *  corner already carries overhang → relief reduces floor's effective load
	 *  net load on floor ≤ 102 → candidate ACCEPTED
	 * </pre>
	 */
	@Test
	public void testSupporteeMaxWeightLimitMustCalculateRelief() {
		Box bottomBox = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(102).withId("First").build();
		Placement wholeButtomPlacement = new Placement(bottomBox.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtomPlacement);
		wholeButtomPlacement.setIndex(stack.size());
		stack.add(wholeButtomPlacement);

		Box cornerBox = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withId("Corner").build();
		Placement cornerPlacement = new Placement(cornerBox.getStackValue(0), 0, 0, 0, 1);
		pointCalculator.add(0, cornerPlacement);
		cornerPlacement.setIndex(stack.size());
		stack.add(cornerPlacement);
		wholeButtomPlacement.addLoad(cornerPlacement, 4, cornerBox.getWeight());

		Box overhangBox = Box.newBuilder().withSize(10, 10, 8).withWeight(100).withId("Second").build();
		Placement overhangPlacement = new Placement(overhangBox.getStackValue(0), stack.size(), 0, 0, 2);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 2), overhangPlacement);
		overhangPlacement.setIndex(stack.size());
		stack.add(overhangPlacement);
		cornerPlacement.addLoad(overhangPlacement, 4, overhangBox.getWeight());

		Box candidate = Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1000).build();
		boxItems.add(new BoxItem(candidate));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	// --- Pressure limit tests (new for this class) --------------------------------

	/**
	 * Pressure = weight / area. A 10×10 floor tile with maxLoadPressure=1 allows
	 * at most weight=100 (pressure=100/100=1.0). Candidate w=101 produces
	 * pressure=1.01 and is rejected.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← new (10×10, w=101): REJECTED
	 *  |  |   new    |       pressure = 101/100 = 1.01 > maxLoadPressure=1
	 *  1  +----------+
	 *  0  +----------+   ← bottom (10×10, w=0, maxLoadPressure=1)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testAbovePressureLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(0).withMaxLoadPressure(1).build();
		Placement bottomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, bottomPlacement);
		stack.add(bottomPlacement);

		// weight=101 on area=100 → pressure=1.01 exceeds maxLoadPressure=1
		Box tooHeavy = Box.newBuilder().withSize(10, 10, 1).withWeight(101).build();
		boxItems.add(new BoxItem(tooHeavy));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	/**
	 * Same floor tile; candidate w=100 produces pressure=1.0 which exactly
	 * equals the limit and is therefore accepted (boundary case).
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← new (10×10, w=100): ACCEPTED
	 *  |  |   new    |       pressure = 100/100 = 1.0 = maxLoadPressure=1 (at limit)
	 *  1  +----------+
	 *  0  +----------+   ← bottom (10×10, w=0, maxLoadPressure=1)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testBelowPressureLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(0).withMaxLoadPressure(1).build();
		Placement bottomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, bottomPlacement);
		stack.add(bottomPlacement);

		// weight=100 on area=100 → pressure=1.0, exactly at limit
		Box withinPressure = Box.newBuilder().withSize(10, 10, 1).withWeight(100).build();
		boxItems.add(new BoxItem(withinPressure));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	// --- Box-count limit tests (new for this class) --------------------------------

	/**
	 * Floor allows at most 1 box above it (maxLoadBoxCount=1). Level-2 is
	 * already placed, consuming the entire 1-box budget. A new box at z=2
	 * would be the second box above the floor and is rejected.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+   ← new (10×10): REJECTED — would be 2nd box above floor
	 *  |  |   new    |       count would be 2, exceeds maxLoadBoxCount=1
	 *  2  +----------+
	 *  1  +----------+   ← level2 (10×10, already placed, count=1/1)
	 *  0  +----------+   ← bottom (10×10, maxLoadBoxCount=1)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testAboveBoxCountLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(0).withMaxLoadBoxCount(1).build();
		Placement bottomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, bottomPlacement);
		stack.add(bottomPlacement);

		Box level2Box = Box.newBuilder().withSize(10, 10, 1).withWeight(0).build();
		Placement level2 = new Placement(level2Box.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), level2);
		stack.add(level2);
		bottomPlacement.addLoad(level2, 100, level2Box.getWeight()); // bottom supports level2

		Box newBox = Box.newBuilder().withSize(10, 10, 1).withWeight(0).build();
		boxItems.add(new BoxItem(newBox));

		// would be 2nd level above bottom, exceeds maxLoadBoxCount=1
		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	/**
	 * Same three-level layout but floor allows up to 2 boxes above it
	 * (maxLoadBoxCount=2). Level-2 occupies slot 1; the new box fills slot 2
	 * and is accepted.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  3  +----------+
	 *  |  |   new    |   ← new (10×10): ACCEPTED — 2nd box above floor (count=2/2)
	 *  2  +----------+
	 *  1  +----------+   ← level2 (10×10, already placed, count=1/2)
	 *  0  +----------+   ← bottom (10×10, maxLoadBoxCount=2)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testBelowBoxCountLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(0).withMaxLoadBoxCount(2).build();
		Placement bottomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, bottomPlacement);
		stack.add(bottomPlacement);

		Box level2Box = Box.newBuilder().withSize(10, 10, 1).withWeight(0).build();
		Placement level2 = new Placement(level2Box.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), level2);
		stack.add(level2);
		bottomPlacement.addLoad(level2, 100, level2Box.getWeight()); // bottom supports level2

		Box newBox = Box.newBuilder().withSize(10, 10, 1).withWeight(0).build();
		boxItems.add(new BoxItem(newBox));

		// would be 2nd level above bottom, within maxLoadBoxCount=2
		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	/**
	 * Both maxLoadWeight and maxLoadPressure are set on the floor. The weight
	 * check (w=6 > maxLoadWeight=5) fires first; the candidate is rejected even
	 * though pressure=6/100=0.06 would be well within maxLoadPressure=2.
	 *
	 * <pre>
	 *  z
	 *  |
	 *  2  +----------+   ← new (10×10, w=6): REJECTED — w=6 > maxLoadWeight=5
	 *  |  |   new    |       (pressure=0.06 < maxLoadPressure=2 would be ok)
	 *  1  +----------+
	 *  0  +----------+   ← bottom (10×10, w=0, maxLoadWeight=5, maxLoadPressure=2)
	 *     0          10  x
	 * </pre>
	 */
	@Test
	public void testWeightRejectedEvenWithPressureConfigured() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(0).withMaxLoadWeight(5).withMaxLoadPressure(2).build();
		Placement bottomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, bottomPlacement);
		stack.add(bottomPlacement);

		Box tooHeavy = Box.newBuilder().withSize(10, 10, 1).withWeight(6).build();
		boxItems.add(new BoxItem(tooHeavy));

		Placement placement = ctrl.getPlacement(0, boxItems.size());
		assertNull(placement);
	}
}
