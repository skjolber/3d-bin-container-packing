package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;

class PointCalculator3DBatchStackTest {

	/*
	 *       +-------+
	 *       |   C   |           second search step
	 *       +-------+-------+
	 *       |   A   |   B   |   first search step
	 *       +-------+-------+
	 *
	 * Three physical placements, but only two search frames.
	 */
	@Test
	void recursiveFramesRestoreAllChildrenAndMinimums() {
		PointCalculator3DStack calculator = new PointCalculator3DStack(4);
		calculator.reset(4, 2, 3);
		calculator.setMinimumAreaAndVolumeLimit(4, 4);
		var origin = calculator.get(0);
		calculator.push();
		Placement a = placement(0, 0, 0);
		Placement b = placement(1, 0, 0);
		calculator.add(origin, List.of(a, b));
		assertThat(calculator.getPoints()).containsExactly(origin);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(1);
		calculator.push();
		calculator.add(calculator.findPoint(0, 0, 1), placement(0, 0, 1));
		assertThat(calculator.getPlacements()).hasSize(3);
		calculator.redo();
		assertThat(calculator.getPlacements()).containsExactly(a, b);
		calculator.pop();
		assertThat(calculator.getPlacements()).containsExactly(a, b);
		calculator.redo();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(4);
		calculator.add(0, List.of(a, b));
		calculator.pop();
		assertThat(calculator.getPlacements()).isEmpty();
		calculator.reset(4, 2, 3);
		assertThat(calculator.getPlacements()).isEmpty();
	}

	@Test
	void ordinaryRedoKeepsExistingNextItemFilteringBehavior() {
		PointCalculator3DStack calculator = new PointCalculator3DStack(3);
		calculator.reset(4, 4, 4);
		calculator.push();
		calculator.add(0, placement(0, 0, 0));
		calculator.setMinimumAreaAndVolumeLimit(2, 3);
		calculator.redo();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(3);
	}

	@Test
	void fastFramesRestoreWholeBatchThenOrdinaryInsertion() {
		FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(4);
		calculator.clearToSize(4, 2, 3);
		calculator.setMinimumAreaAndVolumeLimit(4, 4);
		var origin = calculator.get(0);
		Placement a = placement(0, 0, 0);
		Placement b = placement(1, 0, 0);
		calculator.add(origin, List.of(a, b));
		assertThat(calculator.getPoints()).containsExactly(origin);
		calculator.add(calculator.findPoint(0, 0, 1), placement(0, 0, 1));
		assertThat(calculator.getPlacements()).hasSize(3);
		calculator.setStackSize(1);
		assertThat(calculator.getPlacements()).containsExactly(a, b);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(1);
		calculator.setStackSize(0);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(4);
		assertThat(calculator.getPoints()).isEmpty();
		calculator.add(0, List.of(a, b));
		assertThat(calculator.getPlacements()).hasSize(2);
	}

	@Test
	void fastCheckpointRestoresPartiallyInsertedInvalidBatch() {
		FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(2);
		calculator.clearToSize(2, 2, 2);
		calculator.setMinimumAreaAndVolumeLimit(2, 2);
		Placement child = placement(0, 0, 0);
		assertThatThrownBy(() -> calculator.add(0, List.of(child, child))).isInstanceOf(IllegalArgumentException.class);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getPoints()).isEmpty();
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(2);
		calculator.add(0, List.of(child));
		assertThat(calculator.getPlacements()).containsExactly(child);
	}

	protected static Placement placement(int x, int y, int z) {
		return new Placement(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, x, y, z, false);
	}

	/* Two children are one undo frame, regardless of which batch overload is used. */
	@Test
	void recursiveRemainingMinimaOverloadCheckpointsOnce() {
		PointCalculator3DStack calculator = new PointCalculator3DStack(3);
		calculator.reset(4, 2, 3);
		calculator.setMinimumAreaAndVolumeLimit(4, 8);
		var origin = calculator.get(0);
		List<Placement> children = List.of(placement(0, 0, 0), placement(1, 0, 0));
		calculator.push();
		calculator.add(origin, children, 2, 3);
		assertThat(calculator.getPoints()).containsExactly(origin);
		assertThat(calculator.getPlacements()).hasSize(2);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(3);
		calculator.redo();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(8);
		calculator.add(0, children, 1, 1);
		calculator.pop();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.size()).isEqualTo(1);
	}

	@Test
	void fastRemainingMinimaOverloadCheckpointsOnceAndRecoversFromFailure() {
		FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(3);
		calculator.clearToSize(4, 2, 3);
		calculator.setMinimumAreaAndVolumeLimit(4, 8);
		var origin = calculator.get(0);
		Placement a = placement(0, 0, 0);
		Placement b = placement(1, 0, 0);
		calculator.add(origin, List.of(a, b), 2, 3);
		assertThat(calculator.getPoints()).containsExactly(origin);
		assertThat(calculator.getPlacements()).hasSize(2);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(3);
		calculator.setStackSize(0);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		assertThat(calculator.getMinVolumeLimit()).isEqualTo(8);
		assertThatThrownBy(() -> calculator.add(0, List.of(a, a), 0, 0)).isInstanceOf(IllegalArgumentException.class);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getPoints()).isEmpty();
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.getMinAreaLimit()).isEqualTo(4);
		calculator.add(0, List.of(a, b), 1, 1);
		assertThat(calculator.getPlacements()).hasSize(2);
	}
}
