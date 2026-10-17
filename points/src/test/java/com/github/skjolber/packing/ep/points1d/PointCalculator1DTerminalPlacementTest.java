package com.github.skjolber.packing.ep.points1d;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.PointCalculator;

class PointCalculator1DTerminalPlacementTest {

	// [first][terminal][free] -> clear or restore -> retry from the restored state.
	@ParameterizedTest
	@ValueSource(booleans = {false, true})
	void terminalHistoryAndTotalsSurviveUntilClearOrRestore(boolean indexed) {
		DefaultPointCalculator1D calculator = new DefaultPointCalculator1D(4, DefaultPointCalculator1D.Axis.X);
		calculator.clearToSize(3, 1, 1);
		Placement first = placement(0);
		calculator.add(0, first);
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

		calculator.clear();
		assertThat(calculator.getPlacements()).isEmpty();
		Placement retry = placement(0);
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
