package com.github.skjolber.packing.packer.bruteforce.reference;

import java.util.ArrayList;
import java.util.List;

import com.github.skjolber.packing.v4.api.Box;
import com.github.skjolber.packing.v4.api.BoxItem;
import com.github.skjolber.packing.v4.api.Container;
import com.github.skjolber.packing.v4.api.Placement;
import com.github.skjolber.packing.v4.api.Rotation;
import com.github.skjolber.packing.v4.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.v4.iterator.BoxItemPermutationRotationIterator;
import com.github.skjolber.packing.v4.iterator.DefaultBoxItemPermutationRotationIterator;
import com.github.skjolber.packing.v4.packer.ControlledContainerItem;
import com.github.skjolber.packing.v4.packer.PackagerInterruptedException;
import com.github.skjolber.packing.v4.packer.bruteforce.BruteForceIntermediatePackagerResult;
import com.github.skjolber.packing.v4.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.v4.packer.bruteforce.PointCalculator3DStack;

/**
 * The reference for the differential tests: the 4.x brute force search into a single container and its permutation and
 * rotation iterator, run from the relocated 4.x (module shadowed-v4 in legacy/v4, packages {@code com.github.skjolber.packing.v4}).
 * <p>
 * The search is the attempt of the 4.x brute force packager for one container, as its box item adapter makes it
 * ({@code BruteForcePackager.pack(..)}): for each permutation and rotation state of the iterator, the boxes are placed one by one,
 * in order, at every free point which fits, and the best packing is kept, whether or not all boxes fit. The free points are those of
 * the 4.x point calculator, so the reference shares no code with 5.x.
 */
final class Version4Reference {

	/** Only the given orientation, i.e. a single rotation. */
	static final Rotation NO_ROTATION = Rotation.newBuilder().withBottomAtZeroDegrees().build();

	/** A placed box, copied from the 4.x placement (which is a work object of the search) */
	record Placed(String id, int x, int y, int z, int dx, int dy, int dz) {

		int endX() {
			return x + dx - 1;
		}

		int endY() {
			return y + dy - 1;
		}

		int endZ() {
			return z + dz - 1;
		}

		boolean intersects(Placed other) {
			return x <= other.endX() && other.x <= endX() && y <= other.endY() && other.y <= endY() && z <= other.endZ() && other.z <= endZ();
		}
	}

	/**
	 * The best packing of a search.
	 *
	 * @param loadVolume the volume of the placed boxes
	 * @param loadWeight the weight of the placed boxes
	 * @param placements the placed boxes
	 */
	record Result(long loadVolume, long loadWeight, List<Placed> placements) {

		int boxCount() {
			return placements.size();
		}

		boolean isEmpty() {
			return placements.isEmpty();
		}
	}

	private Version4Reference() {
	}

	/**
	 * @param rotation one of the rotations of the scenarios: {@link ReferenceSupport#NO_ROTATION}, two or three dimensions
	 * @return the same rotation in 4.x
	 */
	static Rotation rotation(com.github.skjolber.packing.api.Rotation rotation) {
		if(rotation == ReferenceSupport.NO_ROTATION) {
			return NO_ROTATION;
		}
		if(rotation == com.github.skjolber.packing.api.Rotation.TWO_D) {
			return Rotation.TWO_D;
		}
		if(rotation == com.github.skjolber.packing.api.Rotation.THREE_D) {
			return Rotation.THREE_D;
		}
		throw new IllegalArgumentException("No 4.x rotation for " + rotation);
	}

	static BoxItem item(String id, int dx, int dy, int dz, Rotation rotation, int weight, int count) {
		return new BoxItem(Box.newBuilder().withId(id).withSize(dx, dy, dz).withRotation(rotation).withWeight(weight).build(), count);
	}

	static Container container(int dx, int dy, int dz, int maxLoadWeight) {
		return Container.newBuilder().withId("container").withSize(dx, dy, dz).withMaxLoadWeight(maxLoadWeight).build();
	}

	/**
	 * @return the iterator over the permutation and rotation states of the boxes which fit the container
	 */
	static DefaultBoxItemPermutationRotationIterator iterator(Container container, List<BoxItem> boxItems) {
		return DefaultBoxItemPermutationRotationIterator.newBuilder()
				.withLoadSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz())
				.withMaxLoadWeight(container.getMaxLoadWeight())
				.withBoxItems(boxItems)
				.build();
	}

	/**
	 * @param millis milliseconds from now
	 * @return an interrupt which is raised once the time has passed, so that a regression cannot hang the build
	 */
	static PackagerInterruptSupplier interruptAfter(long millis) {
		long deadline = System.nanoTime() + millis * 1_000_000L;
		return () -> System.nanoTime() - deadline > 0;
	}

	static Result pack(Container container, List<BoxItem> boxItems, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		return pack(container, iterator(container, boxItems), interrupt);
	}

	/**
	 * @param container the container
	 * @param iterator the permutation and rotation states to search, for example those of {@link #iterator(Container, List)}
	 * @param interrupt the interrupt
	 * @return the best packing
	 */
	static Result pack(Container container, BoxItemPermutationRotationIterator iterator, PackagerInterruptSupplier interrupt) throws PackagerInterruptedException {
		if(iterator.length() == 0) {
			// as the 4.x adapter: there is nothing to search
			return new Result(0, 0, List.of());
		}
		// the work objects of the 4.x adapter for one container
		PointCalculator3DStack pointCalculator = new PointCalculator3DStack(iterator.length() + 1);
		pointCalculator.reset(1, 1, 1);
		List<Placement> stackPlacements = new ArrayList<>(iterator.length() + 1);
		for (int i = 0; i < iterator.length() + 1; i++) {
			stackPlacements.add(new Placement());
		}

		BruteForcePackager packager = BruteForcePackager.newBuilder().build();
		BruteForceIntermediatePackagerResult result = packager.pack(pointCalculator, stackPlacements, new ControlledContainerItem(container, 1), 0, iterator, interrupt);

		// read the placements before the work objects are reused
		List<Placement> placements = result.getStack().getPlacements();
		List<Placed> placed = new ArrayList<>(placements.size());
		for (Placement placement : placements) {
			placed.add(new Placed(placement.getStackValue().getBox().getId(), placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ(),
					placement.getStackValue().getDx(), placement.getStackValue().getDy(), placement.getStackValue().getDz()));
		}
		return new Result(result.getLoadVolume(), result.getLoadWeight(), placed);
	}
}
