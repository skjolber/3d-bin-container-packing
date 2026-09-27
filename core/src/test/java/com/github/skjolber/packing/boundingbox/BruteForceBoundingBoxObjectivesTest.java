package com.github.skjolber.packing.boundingbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.validator.ValidatorResultReason;
import com.github.skjolber.packing.boundingbox.BruteForceBoundingBoxResult.Termination;
import com.github.skjolber.packing.validator.load.IdenticalBoxOnlyLoadValidator;
import com.github.skjolber.packing.validator.load.MaxBoxCountLoadValidator;
import com.github.skjolber.packing.validator.load.MaxPressureLoadValidator;
import com.github.skjolber.packing.validator.load.WeightLoadValidator;

class BruteForceBoundingBoxObjectivesTest {

	/*
	 * Candidate 1:    goal A reached     goal B pending
	 *                       |                 |
	 *                       +--------+--------+
	 *                                |
	 *                           interruption
	 *                                |
	 *                       both layouts survive
	 */
	@Test
	void interruptionPreservesReachedAndPendingObjectiveWinners() {
		AtomicBoolean interrupt = new AtomicBoolean();
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withInterrupt(interrupt::get)
					.withObjective("accepted", bounds -> true, BoundingBox.MIN_X)
					.withObjective("pending", bounds -> { interrupt.set(true); return false; }, BoundingBox.MIN_Y).build();
			assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertThat(result.getReachedGoals()).containsExactly("accepted");
			assertThat(result.getObjectiveResults().keySet()).containsExactly("accepted", "pending");
			assertThat(result.getResults()).hasSize(1);
			assertLayouts(result, 2);
		}
	}

	/*
	 * One cube:     objective A accepts ---+
	 *                                     +----> one shared snapshot, stop
	 *               objective B accepts ---+
	 */
	@Test
	void acceptsAllGoalsOnOneLayoutAndReplacesDuplicateNames() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResultBuilder builder = search.newResultBuilder()
					.withBoxItems(item(1, 1, 1)).withContainer(container(2, 2, 2))
					.withObjective("a", bounds -> false, BoundingBox.MIN_X)
					.withObjective("b", bounds -> true, BoundingBox.MIN_Y)
					.withObjective("a", bounds -> true, BoundingBox.MIN_X);
			BruteForceBoundingBoxResult result = builder.build();
			assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertThat(result.getReachedGoals()).containsExactly("a", "b");
			assertThat(result.getResults()).hasSize(1);
			assertThatThrownBy(() -> builder.withGoal(bounds -> true)).isInstanceOf(IllegalStateException.class);
			assertThatThrownBy(() -> builder.withComparator(BoundingBox.MIN_VOLUME)).isInstanceOf(IllegalStateException.class);
			assertThatThrownBy(() -> builder.withObjective(" ", null, BoundingBox.MIN_X)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> builder.withObjective("x", null, null)).isInstanceOf(NullPointerException.class);
		}
	}

	/*
	 * A rotating 3 x 2 x 1 box reaches these goals in different layouts:
	 *
	 *       narrow X               narrow Y               short Z
	 *       1 x ? x ?              ? x 1 x ?               ? x ? x 1
	 *           |                      |                       |
	 *           +----------------------+-----------------------+
	 *                                  |
	 *                        stop only after all three
	 *
	 * There is no single orientation satisfying all three goals.
	 */
	@Test
	void reachesEveryIndependentGoalBeforeStopping() {
		for(boolean load : new boolean[] {false, true}) {
			AtomicInteger xCalls = new AtomicInteger();
			BoxItem box = new BoxItem(Box.newBuilder().withSize(3, 2, 1).withRotate3D().withWeight(1).build());
			try(BruteForceBoundingBox search = load ? new LoadBruteForceBoundingBox() : new BruteForceBoundingBox()) {
				BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(box).withContainer(container(3, 3, 3))
						.withObjective("x", bounds -> { xCalls.incrementAndGet(); return bounds.dx() == 1; }, BoundingBox.MIN_X)
						.withObjective("y", bounds -> bounds.dy() == 1, BoundingBox.MIN_Y)
						.withObjective("z", bounds -> bounds.dz() == 1, BoundingBox.MIN_Z).build();
				assertThat(result.getTermination()).isEqualTo(Termination.GOAL_REACHED);
				assertThat(result.getReachedGoals()).containsExactly("x", "y", "z");
				assertThat(result.getObjectiveResults().keySet()).containsExactly("x", "y", "z");
				assertThat(result.getObjectiveResults().get("x").getBoundingBox().dx()).isEqualTo(1);
				assertThat(result.getObjectiveResults().get("y").getBoundingBox().dy()).isEqualTo(1);
				assertThat(result.getObjectiveResults().get("z").getBoundingBox().dz()).isEqualTo(1);
				assertThat(xCalls.get()).isPositive();
				assertThatThrownBy(() -> result.getObjectiveResults().clear()).isInstanceOf(UnsupportedOperationException.class);
				assertThatThrownBy(() -> result.getReachedGoals().clear()).isInstanceOf(UnsupportedOperationException.class);
			}
		}
	}

	/*
	 * First layout ----> goal "done" retained
	 *       |
	 *       +---------> impossible goal stays pending ----> exhaustion
	 *
	 * A comparator-worse candidate can still satisfy a pending goal.
	 */
	@Test
	void retainsSatisfiedGoalsWhileUnreachableGoalsExhaustTheSearch() {
		AtomicInteger doneCalls = new AtomicInteger();
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3))
					.withObjective("done", bounds -> { doneCalls.incrementAndGet(); return true; }, BoundingBox.MIN_VOLUME)
					.withObjective("nine", bounds -> bounds.getVolume() == 9, (left, right) -> Long.compare(right.getVolume(), left.getVolume()))
					.withObjective("unreachable", bounds -> false, BoundingBox.MIN_X).build();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertThat(result.getReachedGoals()).containsExactly("done", "nine");
			assertThat(doneCalls.get()).isEqualTo(1);
			assertThat(result.getObjectiveResults().get("nine").getBoundingBox().getVolume()).isEqualTo(9);
		}
	}

	/*
	 * Explicit single objective ----> specialized search ----> same winner
	 * Implicit primary objective --> legacy shorthand ------> same winner
	 */
	@Test
	void explicitSingleObjectiveMatchesTheSingleObjectiveSpecialization() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult explicit = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withObjective("volume", bounds -> bounds.getVolume() == 9, BoundingBox.MIN_VOLUME).build();
			BruteForceBoundingBoxResult implicit = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withGoal(bounds -> bounds.getVolume() == 9).build();
			assertThat(explicit.getBoundingBox()).isEqualTo(implicit.getBoundingBox());
			assertThat(explicit.getTermination()).isEqualTo(Termination.GOAL_REACHED);
			assertThat(explicit.getReachedGoals()).containsExactly("volume");
			assertThat(explicit.getAdditionalResults()).isEmpty();
		}
	}

	/*
	 * One 3 x 2 x 1 box with all rotations:
	 *
	 *                       ONE SEARCH
	 *                           |
	 *          +----------------+----------------+
	 *          |                |                |
	 *          v                v                v
	 *     minimum X        minimum Y        minimum Z
	 *      1 x 3 x 2        3 x 1 x 2        3 x 2 x 1
	 *
	 * All volumes are 6. Axis ties use the usual volume/surface/dimension order.
	 * Primary and minimum Z select the same retained layout, not two copies.
	 */
	@Test
	void returnsSeparateDimensionWinnersAndSharesCoincidentSnapshots() {
		BoxItem item = new BoxItem(Box.newBuilder().withSize(3, 2, 1).withRotate3D().withWeight(1).build());
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item).withContainer(container(3, 3, 3))
					.withMinimumDimensions().build();
			assertThat(result.getAdditionalResults().keySet()).containsExactly("x", "y", "z");
			assertThat(result.getBoundingBox()).isEqualTo(new BoundingBox(3, 2, 1));
			assertThat(result.getAdditionalResults().get("x").getBoundingBox()).isEqualTo(new BoundingBox(1, 3, 2));
			assertThat(result.getAdditionalResults().get("y").getBoundingBox()).isEqualTo(new BoundingBox(3, 1, 2));
			assertThat(result.getAdditionalResults().get("z").getStack()).isSameAs(result.getStack());
			assertThat(result.getResults()).hasSize(3);
			assertThat(result.getResults().get(0).getStack()).isSameAs(result.getStack());
			assertThatThrownBy(() -> result.getAdditionalResults().clear()).isInstanceOf(UnsupportedOperationException.class);
			assertThatThrownBy(() -> result.getResults().clear()).isInstanceOf(UnsupportedOperationException.class);
			assertLayouts(result, 1);
		}
	}

	/*
	 * Side views: A = 3 x 3 x 1, B = 2 x 2 x 1. Container = 5 x 3 x 2.
	 *
	 * MINIMUM VOLUME                         MINIMUM WIDTH
	 *                                       +-------------------+
	 *                                       |         B         |
	 * +-------------------+-------------+   +-------------------+---------+
	 * |         A         |      B      |   |              A              |
	 * +-------------------+-------------+   +-----------------------------+
	 *
	 * Bounds: 5 x 3 x 1, volume 15          Bounds: 3 x 3 x 2, volume 18
	 *
	 * A primary volume winner must not exclude the narrower, larger-volume winner.
	 */
	@Test
	void keepsLargerVolumeWhenItImprovesAnotherObjective() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(3, 3, 1), item(2, 2, 1))
					.withContainer(container(5, 3, 2)).withMinimumX().build();
			assertThat(result.getBoundingBox()).isEqualTo(new BoundingBox(5, 3, 1));
			assertThat(result.getAdditionalResults().get("x").getBoundingBox()).isEqualTo(new BoundingBox(3, 3, 2));
			assertLayouts(result, 2);
		}
	}

	/*
	 *                      SAME SEARCH SPACE
	 *                            |
	 *               +------------+------------+
	 *               |                         |
	 *               v                         v
	 *      one multi-objective run     separate unpruned runs
	 *        volume / X / Y / Z          volume / X / Y / Z
	 *               |                         |
	 *               +------------+------------+
	 *                            |
	 *                     all winners agree
	 */
	@Test
	void matchesIndependentSingleObjectiveRunsIncludingTieBreaks() {
		List<List<BoxItem>> cases = List.of(List.of(item(3, 3, 1), item(2, 2, 1)),
				List.of(item(2, 2, 1), item(2, 1, 1), item(1, 1, 1)),
				List.of(new BoxItem(Box.newBuilder().withSize(2, 1, 1).withWeight(1).withRotate3D().build(), 3)));
		Map<String, BoundingBoxComparator> objectives = Map.of("x", BoundingBox.MIN_X, "y", BoundingBox.MIN_Y, "z", BoundingBox.MIN_Z);
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			for(List<BoxItem> items : cases) {
				BruteForceBoundingBoxResult multi = search.newResultBuilder().withBoxItems(items).withContainer(container(5, 3, 3)).withMinimumDimensions().build();
				for(var entry : objectives.entrySet()) {
					BruteForceBoundingBoxResult single = search.newResultBuilder().withBoxItems(items).withContainer(container(5, 3, 3))
							.withComparator(entry.getValue()).withGoal(bounds -> false).build();
					assertThat(multi.getAdditionalResults().get(entry.getKey()).getBoundingBox()).isEqualTo(single.getBoundingBox());
				}
				BruteForceBoundingBoxResult single = search.newResultBuilder().withBoxItems(items).withContainer(container(5, 3, 3)).withGoal(bounds -> false).build();
				assertThat(multi.getBoundingBox()).isEqualTo(single.getBoundingBox());
			}
		}
	}

	/*
	 * Unit cube + rotating 2 x 1 x 1 box, in a 3 x 3 x 3 container:
	 *
	 *       2 orders x 3 rotations x 3 second-box points
	 *                            |
	 *                            v
	 *                   18 goal invocations
	 *                            |
	 *              +-------------+-------------+
	 *              v             v             v
	 *          minimum X     minimum Y     minimum Z
	 *
	 * Adding objectives must not rerun the search for each objective.
	 */
	@Test
	void evaluatesTheGoalOncePerCandidateRegardlessOfObjectiveCount() {
		AtomicInteger visited = new AtomicInteger();
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(1, 1, 1),
					new BoxItem(Box.newBuilder().withSize(2, 1, 1).withRotate3D().withWeight(1).build()))
					.withContainer(container(3, 3, 3)).withMinimumDimensions()
					.withGoal(bounds -> { visited.incrementAndGet(); return false; }).build();
			assertThat(visited.get()).isEqualTo(18);
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertLayouts(result, 2);
		}
	}

	/*
	 *            SAME COMPLETE CANDIDATES
	 *                       |
	 *             +---------+---------+
	 *             v                   v
	 *        primary: volume 8    largest: volume 12
	 *
	 * An arbitrary additional comparator must disable unsafe volume-only pruning.
	 */
	@Test
	void supportsNamedCustomObjectivesAndReplacement() {
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResultBuilder builder = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withAdditionalObjective("largest", BoundingBox.MIN_VOLUME).withMinimumX()
					.withAdditionalObjective("largest", (left, right) -> Long.compare(right.getVolume(), left.getVolume()));
			BruteForceBoundingBoxResult result = builder.build();
			assertThat(result.getBoundingBox().getVolume()).isEqualTo(8);
			assertThat(result.getAdditionalResults().get("largest").getBoundingBox().getVolume()).isEqualTo(12);
			assertThat(result.getAdditionalResults().keySet()).containsExactly("largest", "x");
			assertThatThrownBy(() -> builder.withAdditionalObjective(" ", BoundingBox.MIN_X)).isInstanceOf(IllegalArgumentException.class);
			assertThatThrownBy(() -> builder.withAdditionalObjective("null", null)).isInstanceOf(NullPointerException.class);
			assertLayouts(result, 2);
		}
	}

	/*
	 * Maximum-volume objective: first retain a volume-12 layout.
	 *
	 *       volume 12 found ---> volume 9 satisfies stopping goal
	 *              |                           |
	 *              v                           v
	 *       additional winner             primary result
	 *       remains volume 12             is goal layout 9
	 *
	 * The primary goal is frozen at 9; the other objectives continue to exhaustion.
	 */
	@Test
	void retainsAdditionalWinnersWhenTheGoalOverridesThePrimaryObjective() {
		BoundingBoxComparator largest = (left, right) -> Long.compare(right.getVolume(), left.getVolume());
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withComparator(largest).withAdditionalObjective("largest", largest)
					.withMinimumDimensions().withGoal(bounds -> bounds.getVolume() == 9).build();
			assertThat(result.getTermination()).isEqualTo(Termination.EXHAUSTED);
			assertThat(result.getReachedGoals()).containsExactly("primary");
			assertThat(result.getBoundingBox().getVolume()).isEqualTo(9);
			assertThat(result.getAdditionalResults().get("largest").getBoundingBox().getVolume()).isGreaterThanOrEqualTo(12);
			assertLayouts(result, 2);
		}
	}

	/*
	 * First complete layout ----> save all objective winners ----> interrupt
	 *                                     |
	 *                                     v
	 *                            one shared snapshot survives
	 *
	 * Expired deadline or impossible input ----> no objective winners
	 */
	@Test
	void preservesAllBestSoFarResultsAndHandlesEmptySearches() {
		AtomicBoolean stop = new AtomicBoolean();
		try(BruteForceBoundingBox search = new BruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item(3, 2, 1), item(2, 1, 1))
					.withContainer(container(5, 4, 3)).withMinimumDimensions()
					.withGoal(bounds -> { stop.set(true); return false; }).withInterrupt(stop::get).build();
			assertThat(result.getTermination()).isEqualTo(Termination.INTERRUPTED);
			assertThat(result.getAdditionalResults()).hasSize(3);
			assertThat(result.getResults()).hasSize(1);
			assertLayouts(result, 2);
			BruteForceBoundingBoxResult expired = search.newResultBuilder().withBoxItems(item(1, 1, 1)).withContainer(container(2, 2, 2))
					.withMinimumDimensions().withInterruptDeadline(0).build();
			assertThat(expired.getResults()).isEmpty();
			assertThat(expired.getAdditionalResults()).isEmpty();
			BruteForceBoundingBoxResult impossible = search.newResultBuilder().withBoxItems(item(2, 2, 2)).withContainer(container(1, 1, 1))
					.withMinimumDimensions().build();
			assertThat(impossible.getResults()).isEmpty();
			assertThat(impossible.getAdditionalResults()).isEmpty();
			assertLayouts(result, 2); // Later operations cannot mutate saved results.
		}
	}

	/*
	 * Three identical cubes, allowing load 2 and two identical levels above:
	 *
	 *           +---------+
	 *           |    A    |                 minimum X: a narrow layout
	 *           +---------+
	 *           |    A    |
	 *           +---------+
	 *           |    A    |
	 *           +---------+
	 *
	 *           +---------+---------+---------+
	 *           |    A    |    A    |    A    |   minimum Z: one layer
	 *           +---------+---------+---------+
	 *
	 * Every retained layout must have its own correct support graph, not references
	 * into the mutable search or another, differently arranged objective winner.
	 */
	@Test
	void retainsValidLoadGraphsForEveryObjective() {
		BoxItem item = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1)
				.withMaxLoadWeight(2).withMaxLoadPressure(2).withMaxLoadIdenticalBoxCount(2).build(), 3);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item).withContainer(container(3, 1, 3))
					.withMinimumDimensions().build();
			assertThat(result.getAdditionalResults().get("x").getBoundingBox()).isEqualTo(new BoundingBox(1, 1, 3));
			assertThat(result.getAdditionalResults().get("z").getBoundingBox()).isEqualTo(new BoundingBox(3, 1, 1));
			assertLayouts(result, 3);
			for(BoundingBoxLayout layout : result.getResults()) {
				List<Placement> placements = layout.getStack().getPlacements();
				List<ValidatorResultReason> reasons = new ArrayList<>();
				assertThat(new WeightLoadValidator().isValid(placements, reasons)).isTrue();
				assertThat(new MaxPressureLoadValidator().isValid(placements, reasons)).isTrue();
				assertThat(new MaxBoxCountLoadValidator().isValid(placements, reasons)).isTrue();
				assertThat(new IdenticalBoxOnlyLoadValidator().isValid(placements, reasons)).isTrue();
				assertThat(reasons).isEmpty();
				for(Placement placement : placements) {
					assertThat(placement.getBoxItem()).isSameAs(item);
					assertThat(placement.getLoadWeight()).isEqualTo(layout.getBoundingBox().dz() == 3 ? 2 - placement.getAbsoluteZ() : 0);
					assertThat(placement.getSupporters()).hasSize(placement.getAbsoluteZ() == 0 ? 0 : 1);
				}
			}
		}
	}

	/*
	 * Three cubes, maximum load zero, in a 3 x 1 x 3 container:
	 *
	 *         stacked candidates              one-layer candidate
	 *                |                                |
	 *                v                                v
	 *           load-invalid                     load-valid
	 *                |                                |
	 *                v                                v
	 *       no custom comparator call       custom comparator may run
	 */
	@Test
	void customObjectivesOnlySeeLoadValidCandidates() {
		BoxItem item = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(0).build(), 3);
		try(LoadBruteForceBoundingBox search = new LoadBruteForceBoundingBox()) {
			BruteForceBoundingBoxResult result = search.newResultBuilder().withBoxItems(item).withContainer(container(3, 1, 3))
					.withMinimumDimensions().withAdditionalObjective("checked", (left, right) -> {
						assertThat(left.dz()).isEqualTo(1);
						assertThat(right.dz()).isEqualTo(1);
						return BoundingBox.MIN_VOLUME.compare(left, right);
					}).build();
			assertLayouts(result, 3);
			assertThat(result.getAdditionalResults().values()).allSatisfy(layout -> assertThat(layout.getBoundingBox().dz()).isEqualTo(1));
		}
	}

	private static void assertLayouts(BruteForceBoundingBoxResult result, int count) {
		assertThat(result.isSuccess()).isTrue();
		for(BoundingBoxLayout layout : result.getResults()) {
			List<Placement> placements = layout.getStack().getPlacements();
			assertThat(placements).hasSize(count);
			int dx = 0, dy = 0, dz = 0;
			for(int i = 0; i < placements.size(); i++) {
				Placement placement = placements.get(i);
				dx = Math.max(dx, placement.getAbsoluteEndX() + 1);
				dy = Math.max(dy, placement.getAbsoluteEndY() + 1);
				dz = Math.max(dz, placement.getAbsoluteEndZ() + 1);
				for(int j = 0; j < i; j++) {
					assertThat(placement.intersects3D(placements.get(j))).isFalse();
				}
			}
			assertThat(layout.getBoundingBox()).isEqualTo(new BoundingBox(dx, dy, dz));
		}
	}

	private static BoxItem item(int dx, int dy, int dz) {
		return new BoxItem(Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build());
	}

	private static Container container(int dx, int dy, int dz) {
		return Container.newBuilder().withSize(dx, dy, dz).withMaxLoadWeight(100).build();
	}
}
