package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.Arrays;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Rotation;
import com.github.skjolber.packing.api.interrupt.PackagerInterruptSupplier;

/**
 * Test helpers shared by the reference implementation self-tests and the differential tests.
 */

final class ReferenceSupport {

	/** Only the given orientation, i.e. a single rotation. */
	static final Rotation NO_ROTATION = Rotation.newBuilder().withBottomAtZeroDegrees().build();

	static final int WEIGHT_UNLIMITED = 1_000_000;

	private ReferenceSupport() {
	}

	/**
	 * @param millis milliseconds from now
	 * @return an interrupt which is raised once the time has passed, so that a regression cannot hang the build
	 */
	static PackagerInterruptSupplier interruptAfter(long millis) {
		long deadline = System.nanoTime() + millis * 1_000_000L;
		return () -> System.nanoTime() - deadline > 0;
	}

	static Box box(String id, int dx, int dy, int dz, Rotation rotation) {
		return box(id, dx, dy, dz, rotation, 1);
	}

	static Box box(String id, int dx, int dy, int dz, Rotation rotation, int weight) {
		return Box.newBuilder().withId(id).withSize(dx, dy, dz).withRotation(rotation).withWeight(weight).build();
	}

	static BoxItem item(String id, int dx, int dy, int dz, Rotation rotation, int count) {
		return new BoxItem(box(id, dx, dy, dz, rotation), count);
	}

	static Container container(int dx, int dy, int dz) {
		return container(dx, dy, dz, WEIGHT_UNLIMITED);
	}

	static Container container(int dx, int dy, int dz, int maxLoadWeight) {
		return Container.newBuilder().withId("container").withSize(dx, dy, dz).withMaxLoadWeight(maxLoadWeight).build();
	}

	/**
	 * @return the permutation and rotation state as one string, with permutations as the box item indexes, rotations as the
	 *         indexes within each box's fitting rotations
	 */
	static String stateKey(int[] permutations, int[] rotations) {
		return Arrays.toString(permutations) + "/" + Arrays.toString(rotations);
	}

}
