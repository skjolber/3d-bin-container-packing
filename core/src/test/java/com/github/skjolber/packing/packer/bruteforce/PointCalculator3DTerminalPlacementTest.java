package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.SimplePoint3D;

class PointCalculator3DTerminalPlacementTest {

	/*
	 *              [leaf]
	 *                |
	 * [root] -- redo + retry -- pop restores [root]
	 *
	 * Terminal and ordinary insertion should expose the same placement snapshots,
	 * while redo/pop must still restore the enclosing search frame.
	 */
	@Test
	void terminalPlacementMatchesOrdinaryPlacementSnapshots() {
		PointCalculator3DStack ordinary = calculator();
		PointCalculator3DStack terminal = calculator();
		Placement ordinaryPlacement = placement(0);
		Placement terminalPlacement = placement(0);
		SimplePoint3D ordinaryPoint = ordinary.get(0);
		SimplePoint3D terminalPoint = terminal.get(0);

		ordinary.push();
		ordinary.add(0, ordinaryPlacement, ordinaryPoint);
		ordinary.updateBest();
		terminal.push();
		terminal.addLast(terminalPlacement, terminalPoint);
		terminal.updateBest();

		assertThat(ordinary.getPlacements()).containsExactly(ordinaryPlacement);
		assertThat(terminal.getPlacements()).containsExactly(terminalPlacement);
		assertThat(placementCoordinates(terminal.getPlacements())).isEqualTo(placementCoordinates(ordinary.getPlacements()));
		assertThat(pointCoordinates(terminal.getPoints())).isEqualTo(pointCoordinates(ordinary.getPoints()));
		assertThat(pointCoordinates(terminal.getBestPoints())).isEqualTo(pointCoordinates(ordinary.getBestPoints()));
	}

	@Test
	void redoAllowsAnotherLeafAndPopRestoresTheParentFrame() {
		PointCalculator3DStack calculator = calculator();
		Placement rootPlacement = placement(0);
		Placement firstLeafPlacement = placement(1);
		Placement retryLeafPlacement = placement(1);
		SimplePoint3D rootPoint = calculator.get(0);

		calculator.push();
		calculator.add(0, rootPlacement, rootPoint);
		calculator.push();
		int firstLeafIndex = calculator.findPoint(1, 0, 0);
		assertThat(firstLeafIndex).isNotNegative();
		SimplePoint3D firstLeafPoint = calculator.get(firstLeafIndex);
		calculator.addLast(firstLeafPlacement, firstLeafPoint);
		assertThat(calculator.getPlacements()).containsExactly(rootPlacement, firstLeafPlacement);

		calculator.redo();
		assertThat(calculator.getPlacements()).containsExactly(rootPlacement);
		int retryLeafIndex = calculator.findPoint(1, 0, 0);
		assertThat(retryLeafIndex).isNotNegative();
		SimplePoint3D retryLeafPoint = calculator.get(retryLeafIndex);
		calculator.add(retryLeafIndex, retryLeafPlacement, retryLeafPoint);
		assertThat(calculator.getPlacements()).containsExactly(rootPlacement, retryLeafPlacement);
		assertThat(pointCoordinates(calculator.getPoints())).containsExactly(List.of(0, 0, 0), List.of(1, 0, 0));

		calculator.pop();
		assertThat(calculator.getStackIndex()).isEqualTo(1);
		assertThat(calculator.getPlacements()).containsExactly(rootPlacement);
		assertThat(calculator.getPoints()).containsExactly(rootPoint);
	}

	/*
	 * [root] -> [terminal leaf] -- interrupt before next iteration
	 * clearToSize abandons the active leaf and starts the next search at a fresh root.
	 */
	@Test
	void clearToSizeAfterAnInterruptedLeafDropsEveryFrameAndPlacement() {
		PointCalculator3DStack calculator = calculator();
		Placement leafPlacement = placement(0);

		calculator.push();
		calculator.addLast(leafPlacement, calculator.get(0));
		assertThat(calculator.getStackIndex()).isEqualTo(1);
		assertThat(calculator.getPlacements()).containsExactly(leafPlacement);

		calculator.clearToSize(3, 1, 1);

		assertThat(calculator.getStackIndex()).isZero();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.getPoints()).isEmpty();

		SimplePoint3D freshPoint = calculator.get(0);
		Placement nextLeafPlacement = placement(0);
		calculator.push();
		calculator.addLast(nextLeafPlacement, freshPoint);
		assertThat(calculator.getPoints()).containsExactly(freshPoint);
		assertThat(calculator.getPlacements()).containsExactly(nextLeafPlacement);
	}

	private static PointCalculator3DStack calculator() {
		PointCalculator3DStack calculator = new PointCalculator3DStack(4);
		calculator.reset(3, 1, 1);
		return calculator;
	}

	private static Placement placement(int x) {
		return new Placement(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, x, 0, 0, false);
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
