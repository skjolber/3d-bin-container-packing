package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

class FastPointCalculator3DTerminalPlacementTest {

	/*
	 * +------+------+------+
	 * | root | leaf |      |    the leaf is the final placement
	 * +------+------+------+
	 *
	 * Terminal and ordinary insertion should expose the same placements and points.
	 */
	@Test
	void terminalPlacementByPointMatchesOrdinaryPlacement() {
		FastPointCalculator3DStack ordinary = calculator();
		FastPointCalculator3DStack terminal = calculator();
		ordinary.add(0, placement(0), ordinary.get(0));
		terminal.add(0, placement(0), terminal.get(0));

		SimplePoint3D ordinaryPoint = ordinary.get(ordinary.findPoint(1, 0, 0));
		ordinary.add(ordinary.findPoint(1, 0, 0), placement(1), ordinaryPoint);
		terminal.addLast(terminal.get(terminal.findPoint(1, 0, 0)), placement(1));

		assertSamePlacements(terminal, ordinary);
	}

	@Test
	void terminalPlacementByIndexMatchesOrdinaryPlacement() {
		FastPointCalculator3DStack ordinary = calculator();
		FastPointCalculator3DStack terminal = calculator();
		ordinary.add(0, placement(0), ordinary.get(0));
		terminal.add(0, placement(0), terminal.get(0));

		ordinary.add(ordinary.findPoint(1, 0, 0), placement(1));
		terminal.addLast(terminal.findPoint(1, 0, 0), placement(1));

		assertSamePlacements(terminal, ordinary);
	}

	/*
	 * [root] -> [terminal leaf] -- setStackSize(1) restores [root], then another leaf is placed
	 */
	@Test
	void setStackSizeAfterTerminalPlacementByIndexRestoresThePrecedingDepth() {
		FastPointCalculator3DStack calculator = calculator();
		FastPointCalculator3DStack reference = calculator();
		Placement root = placement(0);
		calculator.add(0, root, calculator.get(0));
		reference.add(0, placement(0), reference.get(0));

		calculator.addLast(calculator.findPoint(1, 0, 0), placement(1));
		assertThat(calculator.getPlacements()).hasSize(2);

		calculator.setStackSize(1);

		assertThat(calculator.getPlacements()).containsExactly(root);
		assertThat(pointCoordinates(calculator.getPoints())).containsExactly(List.of(0, 0, 0));
		assertThat(calculator.calculateUsedVolume()).isEqualTo(reference.calculateUsedVolume());
		assertThat(calculator.calculateUsedWeight()).isEqualTo(reference.calculateUsedWeight());
		assertThat(freePoints(calculator)).isEqualTo(freePoints(reference));

		// the restored depth can be used again
		calculator.add(calculator.findPoint(1, 0, 0), placement(1));
		reference.add(reference.findPoint(1, 0, 0), placement(1));
		assertSamePlacements(calculator, reference);
		assertThat(freePoints(calculator)).isEqualTo(freePoints(reference));
	}

	@Test
	void setStackSizeAfterTerminalPlacementByPointRestoresThePrecedingDepth() {
		FastPointCalculator3DStack calculator = calculator();
		FastPointCalculator3DStack reference = calculator();
		Placement root = placement(0);
		calculator.add(0, root, calculator.get(0));
		reference.add(0, placement(0), reference.get(0));

		calculator.addLast(calculator.get(calculator.findPoint(1, 0, 0)), placement(1));
		assertThat(pointCoordinates(calculator.getPoints())).containsExactly(List.of(0, 0, 0), List.of(1, 0, 0));

		calculator.setStackSize(1);

		assertThat(calculator.getPlacements()).containsExactly(root);
		assertThat(pointCoordinates(calculator.getPoints())).containsExactly(List.of(0, 0, 0));
		assertThat(freePoints(calculator)).isEqualTo(freePoints(reference));

		// another final placement at the restored depth
		calculator.addLast(calculator.get(calculator.findPoint(1, 0, 0)), placement(1));
		calculator.setStackSize(1);
		assertThat(calculator.getPlacements()).containsExactly(root);
		assertThat(freePoints(calculator)).isEqualTo(freePoints(reference));
	}

	/*
	 * [root] -> [second] -> [terminal leaf] -- setStackSize(1) and setStackSize(0) restore earlier depths
	 */
	@Test
	void setStackSizeAfterTerminalPlacementRestoresEarlierDepths() {
		FastPointCalculator3DStack calculator = calculator();
		FastPointCalculator3DStack root = calculator();
		FastPointCalculator3DStack empty = calculator();
		Placement rootPlacement = placement(0);
		calculator.add(0, rootPlacement, calculator.get(0));
		root.add(0, placement(0), root.get(0));
		calculator.add(calculator.findPoint(1, 0, 0), placement(1));
		calculator.addLast(calculator.findPoint(2, 0, 0), placement(2));
		assertThat(calculator.getPoints()).hasSize(3);

		calculator.setStackSize(1);
		assertThat(calculator.getPlacements()).containsExactly(rootPlacement);
		assertThat(freePoints(calculator)).isEqualTo(freePoints(root));

		calculator.setStackSize(0);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getPoints()).isEmpty();
		assertThat(freePoints(calculator)).isEqualTo(freePoints(empty));
	}

	@Test
	void clearToSizeAfterTerminalPlacementStartsAFreshSearch() {
		FastPointCalculator3DStack calculator = calculator();
		calculator.addLast(0, placement(0));
		assertThat(calculator.getPoints()).hasSize(1);

		calculator.clearToSize(3, 1, 1);

		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.getPoints()).isEmpty();
		assertThat(freePoints(calculator)).isEqualTo(freePoints(calculator()));
	}

	private static void assertSamePlacements(FastPointCalculator3DStack actual, FastPointCalculator3DStack expected) {
		assertThat(placementCoordinates(actual.getPlacements())).isEqualTo(placementCoordinates(expected.getPlacements()));
		assertThat(pointCoordinates(actual.getPoints())).isEqualTo(pointCoordinates(expected.getPoints()));
		assertThat(actual.calculateUsedVolume()).isEqualTo(expected.calculateUsedVolume());
		assertThat(actual.calculateUsedWeight()).isEqualTo(expected.calculateUsedWeight());
	}

	private static FastPointCalculator3DStack calculator() {
		FastPointCalculator3DStack calculator = new FastPointCalculator3DStack(4);
		calculator.clearToSize(3, 1, 1);
		return calculator;
	}

	private static Placement placement(int x) {
		return new Placement(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, x, 0, 0, false);
	}

	private static List<List<Integer>> freePoints(FastPointCalculator3DStack calculator) {
		List<List<Integer>> coordinates = new ArrayList<>(calculator.size());
		for(int i = 0; i < calculator.size(); i++) {
			SimplePoint3D point = calculator.get(i);
			coordinates.add(List.of(point.getMinX(), point.getMinY(), point.getMinZ(), point.getMaxX(), point.getMaxY(), point.getMaxZ()));
		}
		return coordinates;
	}

	private static List<List<Integer>> pointCoordinates(List<? extends Point> points) {
		List<List<Integer>> coordinates = new ArrayList<>(points.size());
		for(Point point : points) {
			coordinates.add(List.of(point.getMinX(), point.getMinY(), point.getMinZ()));
		}
		return coordinates;
	}

	private static List<List<Integer>> placementCoordinates(List<Placement> placements) {
		List<List<Integer>> coordinates = new ArrayList<>(placements.size());
		for(Placement placement : placements) {
			coordinates.add(List.of(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(),
					placement.getStackValue().getDx(), placement.getStackValue().getDy(), placement.getStackValue().getDz()));
		}
		return coordinates;
	}
}
