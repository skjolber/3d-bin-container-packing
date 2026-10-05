package com.github.skjolber.packing.packer.bruteforce;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Placement;

public class PointCalculator3DStackTest {

	/*
	 *       +-------+
	 *       |   A   |      a search for two boxes, interrupted at the second level
	 *       +-------+
	 *
	 * The next search starts at the first level, in an empty container.
	 */
	@Test
	public void clearToSizeStartsAtTheFirstLevel() {
		PointCalculator3DStack calculator = new PointCalculator3DStack(3);
		calculator.reset(4, 4, 4);
		calculator.push();
		calculator.add(0, placement(0, 0, 0));
		calculator.push();

		calculator.clearToSize(2, 2, 2);
		assertThat(calculator.getStackIndex()).isZero();
		assertThat(calculator.getPlacements()).isEmpty();
		assertThat(calculator.size()).isEqualTo(1);
		assertThat(calculator.get(0).getMaxX()).isEqualTo(1);

		// both levels are available
		calculator.push();
		calculator.add(0, placement(0, 0, 0));
		calculator.push();
		assertThat(calculator.getStackIndex()).isEqualTo(2);
	}

	private static Placement placement(int x, int y, int z) {
		return new Placement(Box.newBuilder()
				.withSize(1, 1, 1)
				.withWeight(1)
				.build().getStackValue(0), -1, x, y, z);
	}
}
