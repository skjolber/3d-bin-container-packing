package com.github.skjolber.packing.points3d;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;
import com.github.skjolber.packing.ep.points3d.MarkResetPointCalculator3D;
import com.github.skjolber.packing.points.ValidatingPointCalculator3D;

class DefaultPointCalculator3DBatchTest {

	/*
	 * Front view (y = 1), inside a larger free region:
	 *
	 *     +---+
	 *     | A |
	 *     +---+
	 *     | A |         A: area 1, volume 4
	 *     +---+         B: area 2, volume 2
	 *     | A |
	 *     +---+-------+
	 *     | A |   B   |
	 *     +---+-------+
	 *
	 * Area and volume minima must be calculated independently: insertion uses area 1
	 * and volume 2, then the larger remaining-item limits are installed.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void usesIndependentFixedMinimaAndRetainsAbsolutePlacements(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 6, 4, 6);
		List<Placement> batch = List.of(placement(1, 1, 4, 1, 1, 0), placement(2, 1, 1, 2, 1, 0));
		assertThat(calculator.add(calculator.get(0), batch, 10, 50)).isTrue();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(10);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(50);
		DefaultPointCalculator3D reference = calculator(immutable, 6, 4, 6);
		reference.setMinimumAreaAndVolumeLimit(1, 2);
		for(Placement child : batch) {
			assertThat(reference.addObstacle(child)).isTrue();
		}
		reference.setMinimumAreaAndVolumeLimit(10, 50);
		assertThat(geometry(calculator)).containsExactlyElementsOf(geometry(reference));
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
		assertThat(calculator.getPlacements().get(0)).isSameAs(batch.get(0));
		assertThat(batch.get(0).getAbsoluteX()).isEqualTo(1);
		assertThat(batch.get(1).getAbsoluteX()).isEqualTo(2);
		assertThat(calculator.calculateUsedVolume()).isEqualTo(6);
		assertThat(calculator.calculateUsedWeight()).isEqualTo(2);
		for(Point point : calculator.getAll()) {
			assertThat(point.getArea()).isGreaterThanOrEqualTo(1);
			assertThat(point.getVolume()).isGreaterThanOrEqualTo(2);
			for(Placement child : batch) {
				assertThat(point.fits3D(child)).isFalse();
			}
		}
	}

	/*
	 * Initial free regions (separated):
	 *
	 *   +-+        +---------+
	 *   |s|        | source  |
	 *   +-+        +---------+
	 *  index 0      index 1 -> index 0 after minimum filtering
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void resolvesSourceAgainWhenMinimumFilteringChangesItsIndex(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 8, 4, 4);
		calculator.setPoints(List.of(new DefaultPoint3D(0, 0, 0, 0, 0, 0), new DefaultPoint3D(2, 0, 0, 7, 3, 3)));
		calculator.clear();
		Point source = calculator.get(1);
		List<Placement> batch = List.of(placement(2, 1, 1, 2, 0, 0), placement(2, 1, 1, 4, 0, 0));
		assertThat(calculator.add(source, batch, 2, 2)).isTrue();
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(2);
	}

	/*
	 * Same arrangement in every insertion order:
	 *
	 * z = 1                   z = 0
	 * +---+-----------+       +---+-----------+
	 * | A |     D     |       | A |     C     |
	 * |   +-----------+       |   +-----------+
	 * | A |     D     |       | A |     B     |
	 * +---+-----------+       +---+-----------+
	 *
	 * D can be inserted first, floating above B/C. All children still must fit
	 * during subsequent insertion, regardless of their list order.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void allOrdersMatchSequentialInsertionAndPreserveRemainingChildReachability(boolean immutable) {
		List<Placement> children = new ArrayList<>(List.of(
				placement(1, 2, 2, 1, 1, 1),
				placement(3, 1, 1, 2, 1, 1),
				placement(3, 1, 1, 2, 2, 1),
				placement(3, 2, 1, 2, 1, 2)));
		checkOrders(children, 0, immutable);
	}

	protected void checkOrders(List<Placement> children, int offset, boolean immutable) {
		if(offset < children.size()) {
			for(int i = offset; i < children.size(); i++) {
				Collections.swap(children, offset, i);
				checkOrders(children, offset + 1, immutable);
				Collections.swap(children, offset, i);
			}
			return;
		}
		DefaultPointCalculator3D batch = calculator(immutable, 6, 4, 4);
		DefaultPointCalculator3D sequential = calculator(immutable, 6, 4, 4);
		DefaultPointCalculator3D unfiltered = calculator(immutable, 6, 4, 4);
		sequential.setMinimumAreaAndVolumeLimit(2, 3);
		batch.add(0, children, 2, 3);
		for(int i = 0; i < children.size(); i++) {
			assertThat(sequential.addObstacle(children.get(i))).isTrue();
			assertThat(unfiltered.addObstacle(children.get(i))).isTrue();
			for(int j = i + 1; j < children.size(); j++) {
				assertThat(canInsert(sequential, children.get(j))).isTrue();
				assertThat(canInsert(unfiltered, children.get(j))).isTrue();
			}
		}
		assertThat(geometry(batch)).containsExactlyElementsOf(geometry(sequential));
		assertThat(batch.getPlacements()).containsExactlyElementsOf(children);
	}

	/*
	 *       +-------+-------+
	 *       |   A   |   B   |   exact container fill
	 *       +-------+-------+
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void returnsFalseOnlyAfterWholeBatchFillsContainer(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 2, 1, 1);
		List<Placement> batch = List.of(placement(1, 1, 1, 0, 0, 0), placement(1, 1, 1, 1, 0, 0));
		assertThat(calculator.add(0, batch, 1, 1)).isFalse();
		assertThat(calculator.isEmpty()).isTrue();
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
	}

	/*
	 *       +-------+-------+
	 *       |   A   |       |   a batch of one behaves like a single insertion
	 *       |   A   |       |
	 *       +-------+-------+
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void singleElementBatchMatchesExistingInsertion(boolean immutable) {
		Placement child = placement(2, 2, 1, 0, 0, 0);
		DefaultPointCalculator3D batch = calculator(immutable, 4, 4, 4);
		DefaultPointCalculator3D single = calculator(immutable, 4, 4, 4);
		single.setMinimumAreaAndVolumeLimit(4, 4);
		single.add(0, child);
		assertThat(batch.add(0, List.of(child), 4, 4)).isTrue();
		assertThat(geometry(batch)).containsExactlyElementsOf(geometry(single));
	}

	/*
	 *       +---+---+---+---+ +---+
	 *       | A |   |   |   | | B |   B is outside the 4 x 4 x 4 container
	 *       +---+---+---+---+ +---+
	 *
	 * Neither an empty batch nor a batch with B changes points or limits.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void rejectsEmptyOrOutsideInputBeforeChangingState(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 4, 4, 4);
		calculator.setMinimumAreaAndVolumeLimit(3, 3);
		List<String> before = geometry(calculator);
		assertThatThrownBy(() -> calculator.add(0, List.of(), 3, 3)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> calculator.add(0, List.of(placement(1, 1, 1, 0, 0, 0), placement(1, 1, 1, 4, 0, 0)), 3, 3))
				.isInstanceOf(IllegalArgumentException.class);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(3);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(3);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(geometry(calculator)).containsExactlyElementsOf(before);
	}

	/*
	 *     mark          batch            reset
	 *   +-------+     +-+-+---+        +-------+
	 *   |       | --> |A|B|   |  -->   |       |
	 *   +-------+     +-+-+---+        +-------+
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void markResetRestoresBatchAndPreviousLimits(boolean immutable) {
		MarkResetPointCalculator3D calculator = new MarkResetPointCalculator3D(immutable, 1);
		calculator.clearToSize(4, 4, 4);
		calculator.setMinimumAreaAndVolumeLimit(4, 8);
		List<String> before = geometry(calculator);
		calculator.mark();
		calculator.add(0, List.of(placement(1, 1, 1, 0, 0, 0), placement(1, 1, 1, 1, 0, 0)), 1, 1);
		calculator.reset();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(8);
		assertThat(geometry(calculator)).containsExactlyElementsOf(before);
	}

	protected static boolean canInsert(DefaultPointCalculator3D calculator, Placement placement) {
		for(Point point : calculator.getAll()) {
			if(point.fits3D(placement)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * Two dominoes leave a unit gap needed by the NEXT item, not this batch:
	 *
	 *       +-------+-------+---+
	 *       |   A   |   B   | C |
	 *       +-------+-------+---+
	 *
	 * Child-only minima would discard C's space permanently. Remaining-item minima
	 * participate before either domino is inserted.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void preservesSpaceForSmallerRemainingItem(boolean immutable) {
		List<Placement> batch = List.of(placement(2, 1, 1, 0, 0, 0), placement(2, 1, 1, 2, 0, 0));
		DefaultPointCalculator3D safe = calculator(immutable, 5, 1, 1);
		assertThat(safe.add(safe.get(0), batch, 1, 1)).isTrue();
		assertThat(safe.getMinAreaLimit()).isEqualTo(1);
		assertThat(safe.getMinVolumeLimit()).isEqualTo(1);
		assertThat(safe.findPoint(4, 0, 0)).isGreaterThanOrEqualTo(0);
		Placement remaining = placement(1, 1, 1, 4, 0, 0);
		assertThat(safe.add(safe.findPoint(4, 0, 0), List.of(remaining), Long.MAX_VALUE, Long.MAX_VALUE)).isFalse();
		assertThat(safe.getPlacements()).hasSize(3);
	}

	/*
	 *       +---+---+-----------+
	 *       | A | B |   next    |
	 *       +---+---+-----------+
	 *
	 * The next box is larger: inserting A must not filter out B's smaller space.
	 * Only after B is inserted may the remaining-item limits be installed.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void appliesLargerRemainingLimitsOnlyAfterWholeBatch(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 4, 1, 1);
		List<Placement> batch = List.of(placement(1, 1, 1, 0, 0, 0), placement(1, 1, 1, 1, 0, 0));
		assertThat(calculator.add(0, batch, 2, 2)).isTrue();
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(2);
		assertThat(calculator.findPoint(2, 0, 0)).isGreaterThanOrEqualTo(0);
	}

	/*
	 * Front view (y = 1), as in the first test:
	 *
	 *     +---+
	 *     | A |
	 *     +---+
	 *     | A |         remaining items: area 2, volume 1
	 *     +---+         insertion limits: area 1 (A), volume 1
	 *     | A |
	 *     +---+-------+
	 *     | A |   B   |
	 *     +---+-------+
	 *
	 * The result matches sequential obstacles with the same limits; reset restores
	 * the marked points and limits.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void remainingMinimaAreIndependentAndMarkResetRestoresThem(boolean immutable) {
		MarkResetPointCalculator3D calculator = new MarkResetPointCalculator3D(immutable, 4);
		calculator.clearToSize(6, 4, 6);
		calculator.setMinimumAreaAndVolumeLimit(4, 8);
		List<String> before = geometry(calculator);
		calculator.mark();
		List<Placement> batch = List.of(placement(1, 1, 4, 1, 1, 0), placement(2, 1, 1, 2, 1, 0));
		DefaultPointCalculator3D reference = calculator(immutable, 6, 4, 6);
		reference.setMinimumAreaAndVolumeLimit(1, 1);
		for(Placement child : batch) {
			assertThat(reference.addObstacle(child)).isTrue();
		}
		reference.setMinimumAreaAndVolumeLimit(2, 1);
		calculator.add(0, batch, 2, 1);
		assertThat(geometry(calculator)).containsExactlyElementsOf(geometry(reference));
		calculator.reset();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(8);
		assertThat(geometry(calculator)).containsExactlyElementsOf(before);
		assertThatThrownBy(() -> calculator.add(0, batch, -1, 1)).isInstanceOf(IllegalArgumentException.class);
		assertThat(geometry(calculator)).containsExactlyElementsOf(before);
	}

	/*
	 * Three equal free points share the minimum corner (1, 0, 0); a small point before
	 * them is removed by the batch limits, shifting the indexes:
	 *
	 *   +-+ +---------------------------+
	 *   |s| | source (last of three)    |
	 *   +-+ +---------------------------+
	 *
	 * The source must be found by identity, not by its (shared) coordinates.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void findsSourceSharingItsCornerWithOtherPoints(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 8, 4, 8);
		calculator.setPoints(List.of(
				new DefaultPoint3D(0, 0, 0, 0, 0, 0),
				new DefaultPoint3D(1, 0, 0, 4, 3, 4),
				new DefaultPoint3D(1, 0, 0, 4, 3, 4),
				new DefaultPoint3D(1, 0, 0, 4, 3, 4)));
		calculator.clear();
		assertThat(calculator.size()).isEqualTo(4);
		Point source = calculator.get(3);
		List<Placement> batch = List.of(placement(2, 2, 2, 1, 0, 0), placement(2, 2, 2, 3, 0, 0));
		calculator.add(source, batch, 2, 2);
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
	}

	/*
	 * A point whose stored index is stale (as after pop or redo on a stack calculator)
	 * still identifies the point to insert at:
	 *
	 *   index 0          index 1
	 *   +-+              +---------+
	 *   | |              | source  |   source.getIndex() == 0
	 *   +-+              +---------+
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void ignoresStaleStoredIndex(boolean immutable) {
		DefaultPointCalculator3D calculator = calculator(immutable, 8, 4, 4);
		calculator.setPoints(List.of(new DefaultPoint3D(0, 0, 0, 0, 0, 0), new DefaultPoint3D(2, 0, 0, 7, 3, 3)));
		calculator.clear();
		Point source = calculator.get(1);
		source.setIndex(0);
		List<Placement> batch = List.of(placement(2, 1, 1, 2, 0, 0));
		calculator.add(source, batch, 1, 1);
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
	}

	/*
	 * Front view of a 6 x 7 x 7 container with floating children, which split
	 * many points at once:
	 *
	 *     +-----------+
	 *     |       F   |
	 *     |   D  E    |
	 *     | A A A A A |
	 *     |       B   |
	 *     +-----------+
	 *
	 * The copy buffers must grow; previously the fourth child overflowed them.
	 */
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void floatingChildrenDoNotOverflowBuffers(boolean immutable) {
		ValidatingPointCalculator3D calculator = new ValidatingPointCalculator3D(immutable, 16);
		calculator.clearToSize(6, 7, 7);
		List<Placement> batch = List.of(
				placement(6, 3, 1, 0, 0, 2),
				placement(1, 2, 6, 5, 5, 0),
				placement(2, 4, 1, 3, 0, 6),
				placement(1, 3, 3, 1, 4, 3),
				placement(1, 2, 2, 4, 2, 3),
				placement(1, 1, 7, 4, 4, 0));
		calculator.add(0, batch, 1, 4);
		assertThat(calculator.getPlacements()).containsExactlyElementsOf(batch);
		for(Point point : calculator.getAll()) {
			for(Placement child : batch) {
				assertThat(point.fits3D(child)).isFalse();
			}
		}
	}

	protected static List<String> geometry(DefaultPointCalculator3D calculator) {
		List<String> result = new ArrayList<>();
		for(Point point : calculator.getAll()) {
			result.add(point.getMinX() + "," + point.getMinY() + "," + point.getMinZ() + ":" + point.getMaxX() + "," + point.getMaxY() + "," + point.getMaxZ());
		}
		return result;
	}

	protected static DefaultPointCalculator3D calculator(boolean immutable, int dx, int dy, int dz) {
		DefaultPointCalculator3D result = new DefaultPointCalculator3D(immutable, 16);
		result.clearToSize(dx, dy, dz);
		return result;
	}

	protected static Placement placement(int dx, int dy, int dz, int x, int y, int z) {
		return new Placement(Box.newBuilder()
				.withSize(dx, dy, dz)
				.withWeight(1)
				.build().getStackValue(0), -1, x, y, z, false);
	}
}
