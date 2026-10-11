package com.github.skjolber.packing.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class ContainerTest {

	private static final Container CONTAINER = Container.newBuilder().withSize(4, 4, 4).withMaxLoadWeight(100).build();

	private static BoxItem boxItem(int dx, int dy, int dz, int count) {
		return new BoxItem(Box.newBuilder().withSize(dx, dy, dz).withWeight(1).build(), count);
	}

	@Test
	void canLoadBoxRequiresOneStackValueWithinTheLoadSize() {
		assertTrue(CONTAINER.canLoad(Box.newBuilder().withSize(4, 4, 4).withWeight(1).build()));
		assertFalse(CONTAINER.canLoad(Box.newBuilder().withSize(5, 1, 1).withWeight(1).build()));
		assertFalse(CONTAINER.canLoad(Box.newBuilder().withSize(1, 1, 1).withWeight(101).build()));
		assertFalse(CONTAINER.canLoad(Box.newBuilder().withSize(5, 1, 1).withRotate3D().withWeight(1).build()));
		// only a rotated stack value fits
		assertTrue(Container.newBuilder().withSize(4, 1, 5).withMaxLoadWeight(100).build()
				.canLoad(Box.newBuilder().withSize(5, 1, 1).withRotate3D().withWeight(1).build()));
	}

	@Test
	void canLoadBoxItemChecksTheWeightAndVolumeOfAllItsBoxes() {
		assertTrue(CONTAINER.canLoad(boxItem(1, 1, 1, 64)));
		assertFalse(CONTAINER.canLoad(boxItem(1, 1, 1, 65)));
		assertFalse(CONTAINER.canLoad(boxItem(5, 1, 1, 1)));
	}

	@Test
	void canLoadGroupRequiresEveryBoxToFit() {
		BoxItemGroup fitsAndNot = new BoxItemGroup("a", List.of(boxItem(1, 1, 1, 1), boxItem(5, 1, 1, 1)));
		BoxItemGroup allFit = new BoxItemGroup("b", List.of(boxItem(1, 1, 1, 1), boxItem(2, 2, 2, 1)));

		assertFalse(CONTAINER.canLoad(fitsAndNot));
		assertTrue(CONTAINER.canLoad(allFit));
	}

	@Test
	void canLoadAtLeastOneBoxOfGroupRequiresAnyBoxToFit() {
		BoxItemGroup fitsAndNot = new BoxItemGroup("a", List.of(boxItem(1, 1, 1, 1), boxItem(5, 1, 1, 1)));
		BoxItemGroup noneFit = new BoxItemGroup("b", List.of(boxItem(5, 1, 1, 1), boxItem(1, 5, 1, 1)));
		BoxItemGroup tooHeavy = new BoxItemGroup("c", List.of(boxItem(1, 1, 1, 64), boxItem(1, 1, 1, 64)));

		assertTrue(CONTAINER.canLoadAtLeastOneBox(fitsAndNot));
		assertFalse(CONTAINER.canLoadAtLeastOneBox(noneFit));
		// the group as a whole does not fit within the volume
		assertFalse(CONTAINER.canLoadAtLeastOneBox(tooHeavy));
	}

	@Test
	void canLoadAtLeastOneOfSeveral() {
		BoxItem fits = boxItem(1, 1, 1, 1);
		BoxItem doesNotFit = boxItem(5, 1, 1, 1);

		assertTrue(CONTAINER.canLoadAtLeastOneBox(List.of(doesNotFit, fits)));
		assertFalse(CONTAINER.canLoadAtLeastOneBox(List.of(doesNotFit)));

		BoxItemGroup fitsAndNot = new BoxItemGroup("a", List.of(fits, doesNotFit));
		BoxItemGroup allFit = new BoxItemGroup("b", List.of(fits));
		assertTrue(CONTAINER.canLoadAtLeastOneGroup(List.of(fitsAndNot, allFit)));
		assertFalse(CONTAINER.canLoadAtLeastOneGroup(List.of(fitsAndNot)));
	}

	@Test
	void builderSetsMotion() {
		assertNull(CONTAINER.getMotion());

		Motion motion = new Motion();
		Container container = Container.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).withMotion(motion).build();

		assertSame(motion, container.getMotion());
		assertSame(motion, container.copy().getMotion());
	}
}
