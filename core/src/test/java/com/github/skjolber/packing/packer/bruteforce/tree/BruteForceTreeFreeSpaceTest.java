package com.github.skjolber.packing.packer.bruteforce.tree;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;

class BruteForceTreeFreeSpaceTest {

	private static BoxStackValue[] box(int dx, int dy, int dz) {
		return Box.newBuilder().withId(dx + "x" + dy + "x" + dz).withSize(dx, dy, dz).withWeight(1).build().getStackValues();
	}

	/** a free space of a container with one box of each type */
	private static BruteForceTreeFreeSpace freeSpace(int dx, int dy, int dz, BoxStackValue[]... types) {
		int[] counts = new int[types.length];
		java.util.Arrays.fill(counts, 1);
		return new BruteForceTreeFreeSpace(types.length, dx, dy, dz, counts, types);
	}

	@Test
	void cutsTheContainerAtTheSidesOfTheBox() {
		// types: the box placed, then the probes
		BruteForceTreeFreeSpace freeSpace = freeSpace(10, 10, 10, box(4, 4, 4), box(6, 10, 10), box(10, 6, 10), box(10, 10, 6), box(7, 7, 7), box(3, 10, 10), box(4, 10, 10),
				box(10, 3, 10), box(10, 10, 3));
		freeSpace.place(0, 0, 0, 0, 0, 0);

		// x past the box, y past the box and above it
		assertThat(freeSpace.size(1)).isEqualTo(3);
		assertThat(freeSpace.fits(1, 1)).isTrue();
		assertThat(freeSpace.fits(1, 2)).isTrue();
		assertThat(freeSpace.fits(1, 3)).isTrue();
		assertThat(freeSpace.fits(1, 4)).isFalse();

		// a box in the middle cuts all six sides (each holding a remaining box)
		freeSpace.place(0, 3, 3, 3, 0, 0);
		assertThat(freeSpace.size(1)).isEqualTo(6);
		assertThat(freeSpace.fits(1, 5)).isTrue();
		assertThat(freeSpace.fits(1, 6)).isFalse();
	}

	@Test
	void keepsTheSpaceAcrossTwoBoxes() {
		// two boxes with a gap between them, as deep as the container: a bar across both fits above them, though no free point of either holds it
		BruteForceTreeFreeSpace freeSpace = freeSpace(10, 4, 10, box(4, 4, 4), box(4, 4, 4), box(10, 4, 6), box(2, 4, 10), box(10, 4, 7), box(3, 4, 7));
		freeSpace.place(0, 0, 0, 0, 0, 0);
		freeSpace.place(1, 6, 0, 0, 1, 0);

		// the gap and the space above
		assertThat(freeSpace.size(2)).isEqualTo(2);
		assertThat(freeSpace.fits(2, 2)).isTrue();
		assertThat(freeSpace.fits(2, 3)).isTrue();
		assertThat(freeSpace.fits(2, 4)).isFalse();
		assertThat(freeSpace.fits(2, 5)).isFalse();
	}

	@Test
	void recomputesTheLevelsBelowAnotherPlacement() {
		BruteForceTreeFreeSpace freeSpace = freeSpace(10, 10, 10, box(10, 10, 5), box(10, 5, 5), box(10, 5, 5), box(10, 6, 5), box(10, 10, 2), box(10, 10, 3), box(10, 5, 8),
				box(10, 6, 4));
		freeSpace.place(0, 0, 0, 0, 0, 0);
		freeSpace.place(1, 0, 0, 5, 1, 0);
		assertThat(freeSpace.fits(2, 2)).isTrue();
		assertThat(freeSpace.fits(2, 3)).isFalse();

		// another box at the first level: the second level's spaces are of the new path
		freeSpace.place(0, 0, 0, 0, 4, 0);
		assertThat(freeSpace.fits(2, 5)).isTrue();
		assertThat(freeSpace.fits(2, 6)).isTrue();
		assertThat(freeSpace.fits(2, 7)).isFalse();
	}

	@Test
	void dropsThePiecesWhichHoldNoRemainingBox() {
		BruteForceTreeFreeSpace freeSpace = freeSpace(10, 10, 10, box(8, 8, 7), box(10, 10, 2));
		freeSpace.place(0, 0, 0, 0, 0, 0);
		// the 2-wide pieces beside the box hold no remaining box: only the piece above remains
		assertThat(freeSpace.size(1)).isEqualTo(1);
		assertThat(freeSpace.fits(1, 1)).isTrue();

		// nothing remains below the second level: every piece is dropped
		freeSpace.place(1, 0, 0, 7, 1, 0);
		assertThat(freeSpace.size(2)).isEqualTo(0);
	}
}
