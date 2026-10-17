package com.github.skjolber.packing.ep.points2d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.PointCalculator;

class PointCalculator2DTerminalPlacementTest {

	// [first][terminal][free] -> clear or restore -> retry from the restored state.
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void terminalHistoryAndTotalsSurviveUntilClearOrRestore(boolean indexed) {
		MarkResetPointCalculator2D calculator = new MarkResetPointCalculator2D(true, 4);
		calculator.clearToSize(3, 1, 1);
		Placement first = placement(0);
		calculator.add(0, first);
		calculator.mark();
		Placement terminal = placement(1);
		PointCalculator api = calculator;
		if(indexed) {
			api.addLast(0, terminal);
		} else {
			api.addLast(api.get(0), terminal);
		}
		assertThat(api.getPlacements()).containsExactly(first, terminal);
		assertThat(api.calculateUsedVolume()).isEqualTo(2);
		assertThat(api.calculateUsedWeight()).isEqualTo(4);

		calculator.reset();
		assertThat(calculator.getPlacements()).containsExactly(first);
		Placement retry = placement(1);
		calculator.add(0, retry);
		assertThat(calculator.getPlacements()).last().isSameAs(retry);

		calculator.clearToSize(2, 1, 1);
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.calculateUsedVolume()).isZero();
		assertThat(calculator.calculateUsedWeight()).isZero();
		assertThat(calculator.get(0).getMinX()).isZero();
	}

	private static Placement placement(int x) {
		return new Placement(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(2)
				.build().getStackValue(0), -1, x, 0, 0, false);
	}
}
