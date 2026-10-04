package com.github.skjolber.packing.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class BoxTest {

	@Test
	void globalBoxItemIndexIsImmutableAfterAssignment() {
		BoxItem item = new BoxItem(Box.newBuilder().withSize(1, 1, 1).withWeight(1).build());

		item.setGlobalIndex(3);
		item.setGlobalIndex(3);

		assertThrows(IllegalStateException.class, () -> item.setGlobalIndex(4));
	}

	@Test
	public void testCalculatePressure() {
		assertEquals(2.5, Box.calculatePressure(4, 10), 0.0);
		assertEquals(0.0, Box.calculatePressure(0, 10), 0.0);
		assertEquals(1.0 / 3.0, Box.calculatePressure(3, 1), 0.0);
	}

	@Test
	public void testMinimumPressureUsesMaximumArea() {
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();

		assertSame(Box.getMaximumArea(box.getStackValues()), Box.getMinimumPressure(box.getStackValues()));
	}

	@Test
	public void testLargeAggregateWeightsDoNotOverflow() {
		Box box = Box.newBuilder().withSize(1, 1, 1).withWeight(1_500_000_000).build();
		assertEquals(3_000_000_000L, new BoxItem(box, 2).getWeight());

		Stack stack = new Stack();
		stack.add(new Placement(box.getStackValue(0), 0, 0, 0, 0));
		stack.add(new Placement(box.getStackValue(0), 0, 1, 0, 0));
		assertEquals(3_000_000_000L, stack.getWeight());
	}

	@Test
	public void testBuilder1() {

		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate3D().withWeight(1).build();

		BoxStackValue[] stackValues = box.getStackValues();

		assertEquals(stackValues.length, 6);

		assertUniqueValues(stackValues);

		assertEquals(stackValues[0].getDx(), 1);
		assertEquals(stackValues[0].getDy(), 2);
		assertEquals(stackValues[0].getDz(), 3);

		assertEquals(stackValues[1].getDx(), 2);
		assertEquals(stackValues[1].getDy(), 1);
		assertEquals(stackValues[1].getDz(), 3);
	}

	@Test
	public void testBuilder2() {
		Box box = Box.newBuilder().withSize(1, 2, 3).withRotate2D().withWeight(1).build();

		BoxStackValue[] stackValues = box.getStackValues();
		assertEquals(stackValues.length, 2);
		assertUniqueValues(stackValues);
	}

	private void assertUniqueValues(BoxStackValue[] stackValues) {
		for (int i = 0; i < stackValues.length; i++) {
			BoxStackValue box1 = stackValues[i];

			for (int j = 0; j < stackValues.length; j++) {
				BoxStackValue box2 = stackValues[j];

				if(box1 != box2) {

					if(box1.dx == box2.dx && box1.dy == box2.dy && box1.dz == box2.dz) {
						fail();
					}

				}

			}
		}
	}

	@Test
	public void testContainerCopyPreservesMotion() {
		Motion motion = new Motion();
		Container container = new Container("id", "description", 1, 2, 3, 4, 1, 2, 3, 5, new Stack(), motion);

		Container clone = container.copy();

		assertSame(motion, clone.getMotion());
	}
	@Test
	void copyCopiesDerivedValues() {
		Box box = Box.newBuilder()
				.withId("box")
				.withSize(3, 5, 7)
				.withWeight(11)
				.withRotate3D()
				.withMaxLoadWeight(100)
				.withMaxLoadBoxCount(4)
				.build();

		Box clone = box.copy();

		assertEquals(box.getId(), clone.getId());
		assertEquals(box.getVolume(), clone.getVolume());
		assertEquals(box.getWeight(), clone.getWeight());
		assertEquals(box.getStackValues().length, clone.getStackValues().length);
		assertEquals(box.getMinimumArea(), clone.getMinimumArea());
		assertEquals(box.getMaximumArea(), clone.getMaximumArea());
		assertEquals(box.getMinimumDx(), clone.getMinimumDx());
		assertEquals(box.getMinimumDy(), clone.getMinimumDy());
		assertEquals(box.getMinimumDz(), clone.getMinimumDz());
		assertEquals(box.getMaximumDz(), clone.getMaximumDz());
		assertEquals(box.getMinimumPressure(), clone.getMinimumPressure(), 0.0);
		assertEquals(box.getMaximumPressure(), clone.getMaximumPressure(), 0.0);
		assertEquals(box.isMaxLoadWeight(), clone.isMaxLoadWeight());
		assertEquals(box.isMaxLoadBoxCount(), clone.isMaxLoadBoxCount());
		assertEquals(box.isMaxLoadPressure(), clone.isMaxLoadPressure());
		assertEquals(box.isLoadIdenticalBoxOnly(), clone.isLoadIdenticalBoxOnly());
		for(int i = 0; i < clone.getStackValues().length; i++) {
			// the clone's stack values belong to the clone
			assertSame(clone, clone.getStackValues()[i].getBox());
			assertSame(box, box.getStackValues()[i].getBox());
		}
	}
}
