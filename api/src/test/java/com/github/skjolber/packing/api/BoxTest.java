package com.github.skjolber.packing.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;

import org.junit.jupiter.api.Test;

public class BoxTest {

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

	/**
	 * A stack value belongs to one box: building another box with it would make the first box's stack value refer to
	 * the other box. A copy belongs to no box.
	 */
	@Test
	public void stackValueBelongsToOneBox() {
		Box box = Box.newBuilder().withSize(1, 2, 3).withWeight(1).build();
		BoxStackValue stackValue = box.getStackValue(0);

		assertThrows(IllegalArgumentException.class, () -> new Box("other", null, 6, 1, new BoxStackValue[] { stackValue }, Collections.emptyMap()));
		assertSame(box, stackValue.getBox());

		BoxStackValue copy = stackValue.copy();
		Box other = new Box("other", null, 6, 1, new BoxStackValue[] { copy }, Collections.emptyMap());
		assertSame(other, copy.getBox());
		assertSame(box, stackValue.getBox());
	}

	@Test
	public void loadBoxBuilderBuildsBoxesWithStackValuesOfTheirOwn() {
		Box.LoadBoxBuilder builder = new Box.LoadBoxBuilder().withRotation(r -> r.withDimensions(1, 2, 3)).withWeight(1);
		Box first = builder.build();
		Box second = builder.build();
		assertSame(first, first.getStackValue(0).getBox());
		assertSame(second, second.getStackValue(0).getBox());
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

		Container copy = container.copy();

		assertSame(motion, copy.getMotion());
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

		Box copy = box.copy();

		assertEquals(box.getId(), copy.getId());
		assertEquals(box.getVolume(), copy.getVolume());
		assertEquals(box.getWeight(), copy.getWeight());
		assertEquals(box.getStackValues().length, copy.getStackValues().length);
		assertEquals(box.getMinimumArea(), copy.getMinimumArea());
		assertEquals(box.getMaximumArea(), copy.getMaximumArea());
		assertEquals(box.getMinimumDx(), copy.getMinimumDx());
		assertEquals(box.getMinimumDy(), copy.getMinimumDy());
		assertEquals(box.getMinimumDz(), copy.getMinimumDz());
		assertEquals(box.getMaximumDz(), copy.getMaximumDz());
		assertEquals(box.getMinimumPressure(), copy.getMinimumPressure(), 0.0);
		assertEquals(box.getMaximumPressure(), copy.getMaximumPressure(), 0.0);
		assertEquals(box.isMaxLoadWeight(), copy.isMaxLoadWeight());
		assertEquals(box.isMaxLoadBoxCount(), copy.isMaxLoadBoxCount());
		assertEquals(box.isMaxLoadPressure(), copy.isMaxLoadPressure());
		assertEquals(box.isLoadIdenticalBoxOnly(), copy.isLoadIdenticalBoxOnly());
		for(int i = 0; i < copy.getStackValues().length; i++) {
			// the copy's stack values belong to the copy
			assertSame(copy, copy.getStackValues()[i].getBox());
			assertSame(box, box.getStackValues()[i].getBox());
		}
	}
}
