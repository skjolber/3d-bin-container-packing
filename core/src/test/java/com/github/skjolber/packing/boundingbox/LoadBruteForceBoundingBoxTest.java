package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;
import com.github.skjolber.packing.packer.PackagerInterruptedException;
import com.github.skjolber.packing.validator.load.IdenticalBoxOnlyLoadValidator;
import com.github.skjolber.packing.validator.load.MaxBoxCountLoadValidator;
import com.github.skjolber.packing.validator.load.MaxPressureLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;

/*
 * Diagram conventions: schematic side views, with height increasing upwards
 * and depth one unit unless stated otherwise. W is a box's own weight;
 * load means weight resting on top, excluding the box's own weight.
 */
class LoadBruteForceBoundingBoxTest {

	/*
	 * Three unit cubes, each weighing 1 and allowing load 2.
	 * Load excludes the box's own weight and includes ALL boxes above.
	 *
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 0
	 *            |                   |
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 1
	 *            |                   |
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 2
	 *            |                   |
	 *            +-------------------+
	 *     ================================= floor
	 *
	 * PASS: every load is within the limit of 2.
	 * The snapshot must also preserve the original item identity and indexes.
	 */
	@Test
	void accumulatesWeightThroughTheEntireTower() {
		BoxItem boxes = new BoxItem(unit().withMaxLoadWeight(2).build(), 3, 7, 19);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 1, 3)).build();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertValid(result);
			for(Placement placement : result.getObjectiveResults().get("default").getStack()) {
				assertThat(placement.getLoadWeight()).isEqualTo(2 - placement.getAbsoluteZ());
				assertThat(placement.getBoxItem()).isSameAs(boxes);
			}
			assertThat(boxes.getCount()).isEqualTo(3);
			assertThat(boxes.getLocalIndex()).isEqualTo(7);
			assertThat(boxes.getGlobalIndex()).isEqualTo(19);
			assertThat(boxes.getBox().getBoxItem()).isSameAs(boxes);
		}
	}

	/*
	 * Three unit cubes, each weighing 1, but now allowing only load 1.
	 *
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 0
	 *            |                   |
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 1
	 *            |                   |
	 *            +-------------------+
	 *            |                   |
	 *            |       W = 1       |     Load from above = 2
	 *            |     limit = 1     |     FAIL: 2 > 1
	 *            +-------------------+
	 *     ================================= floor
	 *
	 * The bottom must carry both upper boxes, not just its direct supportee.
	 * No valid complete assembly exists in this narrow container.
	 * Invalid assemblies must never reach the goal callback.
	 */
	@Test
	void rejectsAnOverloadedTowerWithoutCallingTheGoal() {
		BoxItem boxes = new BoxItem(unit().withMaxLoadWeight(1).build(), 3);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 1, 3))
					.withGoal(bounds -> { throw new AssertionError("Invalid loads must not reach the goal"); }).build();
			assertThat(result.isSuccess()).isFalse();
			assertThat(result.getResults()).isEmpty();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
		}
	}

	/*
	 * Each box is 2 x 1 x 1, weighs 1, and permits pressure up to 0.5.
	 * Every horizontal contact has area 2.
	 *
	 *     TWO BOXES                         THREE BOXES
	 *
	 *                                       +-----------------------+
	 *                                       |                       |
	 *                                       |         W = 1         |
	 *                                       |                       |
	 *     +-----------------------+         +-----------------------+
	 *     |                       |         |                       |
	 *     |         W = 1         |         |         W = 1         |
	 *     |                       |         |                       |
	 *     +-----------------------+         +-----------------------+
	 *     |                       |         |                       |
	 *     |         W = 1         |         |         W = 1         |
	 *     |                       |         |                       |
	 *     +-----------------------+         +-----------------------+
	 *     =========================         =========================
	 *
	 *     Bottom load     = 1               Bottom load     = 2
	 *     Contact area    = 2               Contact area    = 2
	 *     Bottom pressure = 0.5             Bottom pressure = 1.0
	 *
	 *     PASS                              FAIL
	 *
	 * The topmost box's weight must contribute to the bottom contact pressure.
	 */
	@Test
	void checksPressureIncludingIndirectLoad() {
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			for(int count : new int[] {2, 3}) {
				BoxItem boxes = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadPressure(0.5).build(), count);
				BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(boxes).withContainer(container(2, 1, count)).build();
				assertThat(result.isSuccess()).isEqualTo(count == 2);
				if(result.isSuccess()) {
					assertValid(result);
				}
			}
		}
	}

	/*
	 * Despite its name, maxLoadBoxCount measures the longest supportee chain.
	 * The base permits one level above it, not merely one direct supportee.
	 *
	 *     TWO LEVELS ABOVE                   ONE LEVEL ABOVE
	 *
	 *     +---------------+
	 *     |               |
	 *     |       C       |
	 *     |               |
	 *     +---------------+
	 *     |               |                 +-------------+-------------+
	 *     |       B       |                 |             |             |
	 *     |               |                 |      B      |      C      |
	 *     +---------------+                 |             |             |
	 *     |               |                 +-------------+-------------+
	 *     |       A       |                 |                           |
	 *     |               |                 |             A             |
	 *     +---------------+                 |                           |
	 *     =================                 +---------------------------+
	 *                                       =============================
	 *
	 *     A has depth 2                     A has depth 1
	 *     FAIL                              PASS
	 *
	 * On the right, the base has two direct supportees but only one level.
	 * Both upper boxes have limit 0, preventing them from supporting another box.
	 */
	@Test
	void checksStackDepthRatherThanTheNumberOfDirectSupportees() {
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BoxItem tower = new BoxItem(unit().withMaxLoadBoxCount(1).build(), 3);
			assertThat(search.newResultBuilder().withBoxItems(tower).withContainer(container(1, 1, 3)).build().isSuccess()).isFalse();
			BoxItem base = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadBoxCount(1).build());
			BoxItem tops = new BoxItem(unit().withMaxLoadBoxCount(0).build(), 2);
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(base, tops).withContainer(container(2, 1, 2)).build();
			assertValid(result);
			Placement bottom = null;
			for(Placement placement : result.getObjectiveResults().get("default").getStack().getPlacements()) {
				if(placement.getBoxItem() == base) {
					bottom = placement;
					break;
				}
			}
			assertThat(bottom).isNotNull();
			assertThat(bottom.getSupportees()).hasSize(2);
		}
	}

	/*
	 * A and B have equal dimensions and the same textual ID ("same"),
	 * but are different BoxItem instances. Both allow only identical items above.
	 *
	 *     SAME ITEM REPEATED                 DIFFERENT ITEM INCLUDED
	 *
	 *                                       +-------------------+
	 *                                       |                   |
	 *                                       |         B         |
	 *                                       |                   |
	 *     +-------------------+             +-------------------+
	 *     |                   |             |                   |
	 *     |         A         |             |         A         |
	 *     |                   |             |                   |
	 *     +-------------------+             +-------------------+
	 *     |                   |             |                   |
	 *     |         A         |             |         A         |
	 *     |                   |             |                   |
	 *     +-------------------+             +-------------------+
	 *     |                   |             |                   |
	 *     |         A         |             |         A         |
	 *     |                   |             |                   |
	 *     +-------------------+             +-------------------+
	 *     =====================             =====================
	 *
	 *     PASS                              FAIL
	 *
	 * Reordering the four boxes cannot fix the narrow right-hand tower:
	 * somewhere an A/B contact remains, regardless of matching dimensions or IDs.
	 */
	@Test
	void identicalMeansTheSameItemNotMatchingDimensionsOrIds() {
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BoxItem identical = new BoxItem(unit().withId("same").withMaxLoadIdenticalBoxCount(-1).build(), 3);
			assertValid(search.newResultBuilder().withBoxItems(identical).withContainer(container(1, 1, 3)).build());
			BoxItem different = new BoxItem(unit().withId("same").withMaxLoadIdenticalBoxCount(-1).build());
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(identical, different).withContainer(container(1, 1, 4)).build();
			assertThat(result.isSuccess()).isFalse();
		}
	}

	/*
	 * The same cube has two allowed stack values with identical dimensions:
	 * F = fragile orientation, maximum load 0, original stack-value index 13.
	 * S = strong orientation, maximum load 1, original stack-value index 29.
	 *
	 *     FRAGILE BASE                      STRONG BASE
	 *
	 *     +-------------------+             +-------------------+
	 *     |                   |             |                   |
	 *     |     F, W = 1      |             |     F, W = 1      |
	 *     |                   |             |                   |
	 *     +-------------------+             +-------------------+
	 *     |                   |             |                   |
	 *     |     F, W = 1      |             |     S, W = 1      |
	 *     |     limit = 0     |             |     limit = 1     |
	 *     +-------------------+             +-------------------+
	 *     =====================             =====================
	 *
	 *     Load 1 exceeds limit 0            Load 1 meets limit 1
	 *     FAIL                              PASS
	 *
	 * Geometrically identical orientations must remain distinguishable.
	 * Stop at the first valid assembly and retain the original strong stack value.
	 */
	@Test
	void respectsOrientationSpecificLoadLimits() {
		BoxStackValue fragile = BoxStackValue.newBuilder().withDimensions(1, 1, 1).withMaxLoadWeight(0).withIndex(13).build();
		BoxStackValue strong = BoxStackValue.newBuilder().withDimensions(1, 1, 1).withMaxLoadWeight(1).withIndex(29).build();
		BoxItem boxes = new BoxItem(new Box(unit().build(), List.of(fragile, strong)), 2);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 1, 2))
					.withGoal(bounds -> true).build();
			assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertValid(result);
			for(Placement placement : result.getObjectiveResults().get("default").getStack().getPlacements()) {
				if(placement.getAbsoluteZ() == 0) {
					assertThat(placement.getStackValue()).isSameAs(strong);
				}
			}
		}
	}

	/*
	 * Three unit cubes each allow maximum load 0.
	 *
	 *     INVALID CANDIDATE
	 *
	 *     +---------------+
	 *     |               |
	 *     |       A       |
	 *     |               |
	 *     +---------------+
	 *     |               |
	 *     |       A       |
	 *     |               |
	 *     +---------------+
	 *     |               |
	 *     |       A       |
	 *     |               |
	 *     +---------------+
	 *     =================
	 *
	 *     Rejected before calling the goal.
	 *
	 *
	 *     VALID CANDIDATE
	 *
	 *     +---------------+---------------+---------------+
	 *     |               |               |               |
	 *     |       A       |       A       |       A       |
	 *     |               |               |               |
	 *     +---------------+---------------+---------------+
	 *     =================================================
	 *
	 *     Bounding box: 3 x 1 x 1
	 *     The goal callback may inspect this layout and must see height 1.
	 *
	 * A subsequent search in a narrow tower container fails.
	 * That later operation must not change the previously returned valid snapshot.
	 */
	@Test
	void onlyCallsGoalOnValidAssembliesAndKeepsAnIndependentSnapshot() {
		BoxItem boxes = new BoxItem(unit().withMaxLoadWeight(0).build(), 3);
		AtomicInteger goals = new AtomicInteger();
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(boxes).withContainer(container(3, 1, 3))
					.withGoal(bounds -> {
						assertThat(bounds.dz()).isEqualTo(1);
						goals.incrementAndGet();
						return false;
					}).build();
			assertThat(goals.get()).isPositive();
			assertValid(result);
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(new BoundingBox(3, 1, 1));
			search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 1, 3)).build();
			assertValid(result);
		}
	}

	/*
	 * The custom comparator deliberately prefers greater height.
	 * Three unit cubes, each allowing load 3, can form this valid height-3 tower.
	 *
	 *              +-------------------+
	 *              |                   |
	 *              |         A         |
	 *              |                   |
	 *              +-------------------+
	 *              |                   |
	 *              |         A         |       Height = 3
	 *              |                   |
	 *              +-------------------+
	 *              |                   |
	 *              |         A         |
	 *              |                   |
	 *              +-------------------+
	 *              =====================
	 *
	 * Separate operations verify both interruption outcomes:
	 *
	 *     Search
	 *        |
	 *        +--> Valid complete assembly found
	 *        |               |
	 *        |               v
	 *        |       Save placements and
	 *        |       independent load graph
	 *        |
	 *        +--> Interrupt
	 *                        |
	 *                        v
	 *                 Return saved result
	 *
	 *
	 *     Already-expired deadline
	 *                 |
	 *                 v
	 *          No search performed
	 *                 |
	 *                 v
	 *          No complete result
	 *
	 * The interrupted operation need not return the tower pictured above.
	 * Its own valid snapshot, and the earlier custom-objective snapshot, must survive.
	 */
	@Test
	void supportsCustomObjectivesAndRetainsLoadGraphOnInterruption() {
		BoxItem boxes = new BoxItem(unit().withMaxLoadWeight(3).build(), 3);
		AtomicBoolean interrupt = new AtomicBoolean();
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult best = search.newResultBuilder().withBoxItems(boxes).withContainer(container(3, 3, 3))
					.withComparator((left, right) -> Integer.compare(right.dz(), left.dz())).build();
			assertThat(best.getObjectiveResults().get("default").getBoundingBox().dz()).isEqualTo(3);
			assertValid(best);
			BruteForceBoundingBoxResult stopped = search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 2, 3))
					.withGoal(bounds -> { interrupt.set(true); return false; }).withInterrupt(interrupt::get).build();
			assertThat(stopped.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertValid(stopped);
			BruteForceBoundingBoxResult expired = search.newResultBuilder().withBoxItems(boxes).withContainer(container(1, 1, 3))
					.withInterruptDeadline(0).build();
			assertThat(expired.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertThat(expired.isSuccess()).isFalse();
			assertValid(best);
		}
	}

	/*
	 * Inputs: a unit cube and a 2 x 1 x 1 box, without load restrictions.
	 * Container: 3 x 3 x 3.
	 *
	 *     TWO ITEM ORDERS
	 *
	 *          Cube --> Long box
	 *
	 *      Long box --> Cube
	 *
	 *
	 *     THREE LONG-BOX ORIENTATIONS
	 *
	 *              Along X
	 *
	 *          +-----------------------+
	 *          |                       |
	 *          |       LONG BOX        |
	 *          +-----------------------+
	 *
	 *
	 *              Along Z
	 *
	 *          +-----------+
	 *          |           |
	 *          |           |
	 *          | LONG BOX  |
	 *          |           |
	 *          |           |
	 *          +-----------+
	 *
	 *
	 *              Along Y
	 *
	 *          Extends into the page:
	 *          dimensions 1 x 2 x 1
	 *
	 *
	 *     THREE FITTING POINTS FOR THE SECOND BOX
	 *
	 *                   Above
	 *                     |
	 *                     v
	 *
	 *                 +-------+
	 *                 | FIRST |
	 *                 +-------+ ----> Beside
	 *
	 *                     \
	 *                      \----> Behind
	 *
	 * Expect 2 orders x 3 orientations x 3 fitting points = 18 complete candidates.
	 * The optimum must also match the geometric bounding-box implementation.
	 */
	@Test
	void checksAllPermutationsRotationsAndPointsWithoutLoadRestrictions() {
		BoxItem cube = new BoxItem(unit().build());
		BoxItem rotated = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withRotate3D().build());
		AtomicInteger leaves = new AtomicInteger();
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox(); BruteForceBoundingBox geometric = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(cube, rotated).withContainer(container(3, 3, 3))
					.withGoal(bounds -> { leaves.incrementAndGet(); return false; }).build();
			assertThat(leaves.get()).isEqualTo(2 * rotated.getBox().getStackValues().length * 3);
			BruteForceBoundingBoxResult geometricResult = geometric.newResultBuilder().withBoxItems(cube, rotated).withContainer(container(3, 3, 3)).build();
			assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(geometricResult.getObjectiveResults().get("default").getBoundingBox());
			assertValid(result);
		}
	}

	/*
	 * The bridge weighs 2; each support can carry 1.
	 *
	 *     STEP 1: Place the left support
	 *
	 *     +-------------------+
	 *     |                   |
	 *     |   LEFT SUPPORT    |
	 *     |     limit = 1     |
	 *     +-------------------+
	 *     ========================================= floor
	 *
	 *
	 *     STEP 2: Place the bridge
	 *
	 *     +---------------------------------------+
	 *     |                                       |
	 *     |             BRIDGE, W = 2             |
	 *     |                                       |
	 *     +-------------------+-------------------+
	 *     |                   |
	 *     |   LEFT SUPPORT    |     Empty space
	 *     |     load = 2      |
	 *     +-------------------+
	 *     ========================================= floor
	 *
	 *     Temporarily overloaded: 2 > 1.
	 *     This is an incomplete search state, not an accepted result.
	 *
	 *
	 *     STEP 3: Place the right support
	 *
	 *     +---------------------------------------+
	 *     |                                       |
	 *     |             BRIDGE, W = 2             |
	 *     |                                       |
	 *     +-------------------+-------------------+
	 *     |                   |                   |
	 *     |   LEFT SUPPORT    |   RIGHT SUPPORT   |
	 *     |     load = 1      |     load = 1      |
	 *     +-------------------+-------------------+
	 *     ========================================= floor
	 *
	 *     Complete assembly: PASS
	 *
	 * Both construction orders must reach a valid complete candidate:
	 *
	 *     Left support --> Right support --> Bridge
	 *
	 *     Left support --> Bridge        --> Right support
	 *
	 * Rejecting the second order at its intermediate overload loses a valid branch.
	 * The bridge itself allows load 0, ruling out assemblies with boxes above it.
	 */
	@Test
	void enumeratesTheBranchWhichAddsLoadReliefAfterTheBridge() {
		BoxItem supports = new BoxItem(unit().withMaxLoadWeight(1).build(), 2);
		BoxItem bridge = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(2).withMaxLoadWeight(0).build());
		AtomicInteger leaves = new AtomicInteger();
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(supports, bridge).withContainer(container(2, 1, 2))
					.withGoal(bounds -> { leaves.incrementAndGet(); return false; }).build();
			// [support, support, bridge] and [support, bridge, support]. The latter
			// temporarily overloads its first supporter, before the last one shares it.
			assertThat(leaves.get()).isEqualTo(2);
			assertValid(result);
		}
	}

	/*
	 * Solve each small input through two independent paths:
	 *
	 *                  SAME BOXES AND CONTAINER
	 *                             |
	 *               +-------------+-------------+
	 *               |                           |
	 *               v                           v
	 *     +---------------------+     +-------------------------+
	 *     | Bounding-box search |     | Independent test search |
	 *     |                     |     |                         |
	 *     | Permutations        |     | Every integer position  |
	 *     | Rotations           |     | Every allowed rotation  |
	 *     | Extreme points      |     | Collision checks        |
	 *     | Numeric load checks |     | Existing load validators|
	 *     +---------------------+     +-------------------------+
	 *               |                           |
	 *               v                           v
	 *         Best bounding box           Best bounding box
	 *               |                           |
	 *               +-------------+-------------+
	 *                             |
	 *                             v
	 *                      Must be equal
	 *
	 * The three cases cover zero allowable load, identical-only stacking,
	 * and a bridge with pressure-limited supports, in a 3 x 1 x 2 container.
	 * This checks the chosen optimum, not just validity of the returned layout.
	 */
	@Test
	void agreesWithIndependentCoordinateEnumerationAndLoadValidators() {
		List<List<BoxItem>> cases = List.of(
				List.of(new BoxItem(unit().withMaxLoadWeight(0).build(), 3)),
				List.of(new BoxItem(unit().withMaxLoadIdenticalBoxCount(-1).build(), 2), new BoxItem(unit().build())),
				List.of(new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadBoxCount(0).build()),
						new BoxItem(unit().withMaxLoadPressure(0.5).build(), 2)));
		Container limits = container(3, 1, 2);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			for(List<BoxItem> items : cases) {
				List<Box> boxes = new ArrayList<>();
				for(BoxItem item : items) {
					for(int i = 0; i < item.getCount(); i++) {
						boxes.add(item.getBox());
					}
				}
				BoundingBox expected = enumerateCoordinates(boxes, limits, new ArrayList<>(), null);
				BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(items).withContainer(limits).build();
				if(expected == null) {
					assertThat(result.getObjectiveResults()).isEmpty();
				} else {
					assertThat(result.getObjectiveResults().get("default").getBoundingBox()).isEqualTo(expected);
				}
				assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
				if(expected != null) {
					assertValid(result);
				}
			}
		}
	}

	// Enumerate arbitrary integer coordinates, without extreme points, search
	// pruning or the production numeric load evaluator. Validate a fresh graph.
	private static BoundingBox enumerateCoordinates(List<Box> boxes, Container limits, List<Placement> placed, BoundingBox best) {
		if(placed.size() == boxes.size()) {
			for(Placement placement : placed) {
				placement.clearLoad();
			}
			List<Placement> ordered = new ArrayList<>(placed);
			for(int i = 1; i < ordered.size(); i++) {
				Placement value = ordered.get(i);
				int j = i;
				while(j > 0 && ordered.get(j - 1).getAbsoluteZ() > value.getAbsoluteZ()) {
					ordered.set(j, ordered.get(j - 1));
					j--;
				}
				ordered.set(j, value);
			}
			int dx = 0, dy = 0, dz = 0;
			for(Placement upper : ordered) {
				dx = Math.max(dx, upper.getAbsoluteEndX() + 1);
				dy = Math.max(dy, upper.getAbsoluteEndY() + 1);
				dz = Math.max(dz, upper.getAbsoluteEndZ() + 1);
				List<Placement> supporters = new ArrayList<>();
				long totalArea = 0;
				for(Placement lower : ordered) {
					if(lower.getAbsoluteEndZ() + 1 == upper.getAbsoluteZ() && lower.intersects2D(upper)) {
						supporters.add(lower);
						totalArea += overlap(lower, upper);
					}
				}
				for(Placement lower : supporters) {
					long area = overlap(lower, upper);
					lower.addLoad(upper, area, (double) upper.getWeight() * area / totalArea);
				}
			}
			List<ValidatorResultReason> reasons = new ArrayList<>();
			if(!new WeightLoadValidator().isValid(placed, reasons) || !new MaxPressureLoadValidator().isValid(placed, reasons)
					|| !new MaxBoxCountLoadValidator().isValid(placed, reasons) || !new IdenticalBoxOnlyLoadValidator().isValid(placed, reasons)) {
				return best;
			}
			BoundingBox candidate = new BoundingBox(dx, dy, dz);
			return best == null || BoundingBox.MIN_VOLUME.compare(candidate, best) < 0 ? candidate : best;
		}
		for(BoxStackValue value : boxes.get(placed.size()).getStackValues()) {
			for(int x = 0; x <= limits.getLoadDx() - value.getDx(); x++) {
				for(int y = 0; y <= limits.getLoadDy() - value.getDy(); y++) {
					for(int z = 0; z <= limits.getLoadDz() - value.getDz(); z++) {
						Placement candidate = new Placement(value, 0, x, y, z, true);
						boolean intersects = false;
						for(Placement existing : placed) {
							intersects |= candidate.intersects3D(existing);
						}
						if(!intersects) {
							placed.add(candidate);
							best = enumerateCoordinates(boxes, limits, placed, best);
							placed.remove(placed.size() - 1);
						}
					}
				}
			}
		}
		return best;
	}

	/*
	 * A bridge weighing 1 shares equal contact area with two supports.
	 * The placement array is ordered [left support, bridge, right support],
	 * so support-graph construction must not assume bottom-up insertion order.
	 *
	 *     +---------------------------------------+
	 *     |                                       |
	 *     |             BRIDGE, W = 1             |
	 *     |                                       |
	 *     +-------------------+-------------------+
	 *     |                   |                   |
	 *     |   LEFT SUPPORT    |   RIGHT SUPPORT   |
	 *     |                   |                   |
	 *     +-------------------+-------------------+
	 *     =========================================
	 *
	 *              0.5                  0.5
	 *               |                    |
	 *               v                    v
	 *        Load on left         Load on right
	 *
	 * The returned graph must retain both supporter links and fractional weights.
	 * Each support permits load 1 and pressure 1.
	 *
	 * A second fixture increases the bridge weight to 2:
	 * left support alone fails (load 2 > 1); both supports pass (load 1 each).
	 */
	@Test
	void handlesASupporterAddedAfterTheBridgeAndFractionalLoads() throws PackagerInterruptedException {
		BoxItem supports = new BoxItem(unit().withMaxLoadWeight(1).withMaxLoadPressure(1).build(), 2);
		BoxItem bridge = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadWeight(0).build());
		Placement left = at(supports, 0, 0);
		Placement above = at(bridge, 0, 1);
		Placement right = at(supports, 1, 0);
		LoadBruteForceBoundingBoxSearch state = state(left, above, right);
		assertThat(state.isValidLayout()).isTrue();
		Stack snapshot = state.createSnapshot();
		assertValid(snapshot);
		assertThat(snapshot.getPlacements().get(0).getLoadWeight()).isEqualTo(0.5);
		assertThat(snapshot.getPlacements().get(2).getLoadWeight()).isEqualTo(0.5);
		assertThat(snapshot.getPlacements().get(1).getSupporters()).hasSize(2);

		// An incomplete support graph would reject this bridge (2 > 1).
		BoxItem heavy = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(2).build());
		assertThat(state(left, at(heavy, 0, 1)).isValidLayout()).isFalse();
		LoadBruteForceBoundingBoxSearch shared = state(left, at(heavy, 0, 1), right);
		assertThat(shared.isValidLayout()).isTrue();
		assertValid(shared.createSnapshot());
	}

	/*
	 * The upper cube weighs 2. The base has top area 2 and pressure limit 1.
	 *
	 *     +-------------------+
	 *     |                   |
	 *     |    TOP, W = 2     |
	 *     |                   |
	 *     +-------------------+-------------------+
	 *     |                                       |
	 *     |                 BASE                  |
	 *     |          pressure limit = 1           |
	 *     |                                       |
	 *     +---------------------------------------+
	 *     =========================================
	 *
	 *     <--- contact = 1 --->
	 *     <----------- base area = 2 -------------->
	 *
	 * Correct contact pressure: 2 / 1 = 2 -> FAIL.
	 * Averaging over the whole base incorrectly gives 2 / 2 = 1.
	 *
	 * The test then moves the upper box onto the floor beside the base.
	 * That layout passes, proving stale load does not survive re-evaluation.
	 */
	@Test
	void checksContactPressureRatherThanAveragingOverTheWholeTop() throws PackagerInterruptedException {
		BoxItem base = new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withMaxLoadPressure(1).build());
		BoxItem top = new BoxItem(unit().withWeight(2).build());
		LoadBruteForceBoundingBoxSearch state = state(at(base, 0, 0), at(top, 0, 1));
		assertThat(state.isValidLayout()).isFalse(); // 2/1, not 2/2
		state.placements[1].setPoint(0, 2, 0, 0);
		assertThat(state.isValidLayout()).isTrue(); // No stale load survives a different layout.
		assertValid(state.createSnapshot());
	}

	/*
	 * Bottom A requires only A items anywhere above it.
	 * Middle A uses an unrestricted orientation of the same item.
	 *
	 *     +---------------------------+
	 *     |                           |
	 *     |             B             |
	 *     |       different item      |
	 *     +---------------------------+
	 *     |                           |
	 *     |             A             |
	 *     | unrestricted orientation  |
	 *     +---------------------------+
	 *     |                           |
	 *     |             A             |
	 *     |       A-only above        |
	 *     +---------------------------+
	 *     =============================
	 *
	 *     Bottom A --> Middle A --> B
	 *                               ^
	 *                               |
	 *                     Still violates bottom A
	 *
	 * FAIL: checking only immediate neighbours would miss this violation.
	 */
	@Test
	void identicalRestrictionIncludesIndirectSupportees() throws PackagerInterruptedException {
		BoxStackValue restricted = BoxStackValue.newBuilder().withDimensions(1, 1, 1).withMaxLoadIdenticalOnly(true).build();
		BoxStackValue unrestricted = BoxStackValue.newBuilder().withDimensions(1, 1, 1).build();
		BoxItem same = new BoxItem(new Box(unit().build(), List.of(restricted, unrestricted)), 2);
		BoxItem foreign = new BoxItem(unit().build());
		Placement middle = new Placement(unrestricted, 0, 0, 0, 1, false);
		assertThat(state(at(same, 0, 0), middle, at(foreign, 0, 2)).isValidLayout()).isFalse();
	}

	private static LoadBruteForceBoundingBoxSearch state(Placement... placements) {
		LoadBruteForceBoundingBoxSearch state = new LoadBruteForceBoundingBoxSearch(List.of(), container(4, 4, 4), BoundingBox.MIN_VOLUME, null, () -> false);
		state.placements = placements;
		for(Placement placement : placements) {
			state.originalValues.put(placement.getStackValue(), placement.getStackValue());
		}
		return state;
	}

	private static Placement at(BoxItem item, int x, int z) {
		return new Placement(item.getBox().getStackValues()[0], 0, x, 0, z, false);
	}

	private static Box.Builder unit() {
		return Box.newBuilder().withSize(1, 1, 1).withWeight(1);
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}

	private static void assertValid(BruteForceBoundingBoxResult result) {
		assertThat(result.isSuccess()).isTrue();
		assertValid(result.getObjectiveResults().get("default").getStack());
	}

	private static void assertValid(Stack stack) {
		List<Placement> placements = stack.getPlacements();
		List<ValidatorResultReason> reasons = new ArrayList<>();
		assertThat(new WeightLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
		assertThat(new MaxPressureLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
		assertThat(new MaxBoxCountLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
		assertThat(new IdenticalBoxOnlyLoadValidator().isValid(placements, reasons)).as("%s", reasons).isTrue();
		for(int i = 0; i < placements.size(); i++) {
			Placement upper = placements.get(i);
			assertThat(upper.getIndex()).isEqualTo(i);
			long supportedArea = 0;
			int supporters = 0;
			for(int j = 0; j < placements.size(); j++) {
				Placement lower = placements.get(j);
				if(i != j) {
					assertThat(lower.intersects3D(upper)).isFalse();
				}
				if(lower.getAbsoluteEndZ() + 1 != upper.getAbsoluteZ() || !lower.intersects2D(upper)) {
					continue;
				}
				long area = overlap(lower, upper);
				assertThat(upper.getSupporters()).anySatisfy(link -> {
					assertThat(link.getPlacement()).isSameAs(lower);
					assertThat(link.getArea()).isEqualTo(area);
				});
				assertThat(lower.getSupportees()).anySatisfy(link -> assertThat(link.getPlacement()).isSameAs(upper));
				supportedArea += area;
				supporters++;
			}
			assertThat(upper.getSupporters()).hasSize(supporters);
			assertThat(upper.getSupportedArea()).isEqualTo(upper.getAbsoluteZ() == 0 ? upper.getStackValue().getArea() : supportedArea);
			assertThat(upper.getLoadWeight()).isCloseTo(weightAbove(upper), offset(1e-9));
		}
	}

	private static double weightAbove(Placement lower) {
		double weight = 0;
		for(PlacementLoad link : lower.getSupportees()) {
			Placement upper = link.getPlacement();
			weight += (upper.getWeight() + weightAbove(upper)) * link.getArea() / upper.getSupportedArea();
		}
		return weight;
	}

	private static long overlap(Placement lower, Placement upper) {
		return (long) (Math.min(lower.getAbsoluteEndX(), upper.getAbsoluteEndX()) - Math.max(lower.getAbsoluteX(), upper.getAbsoluteX()) + 1)
				* (Math.min(lower.getAbsoluteEndY(), upper.getAbsoluteEndY()) - Math.max(lower.getAbsoluteY(), upper.getAbsoluteY()) + 1);
	}
}
