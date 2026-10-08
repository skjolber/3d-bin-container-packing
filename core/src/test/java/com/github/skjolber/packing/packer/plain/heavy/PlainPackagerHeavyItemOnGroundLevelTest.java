package com.github.skjolber.packing.packer.plain.heavy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.github.skjolber.packing.test.ascii.PackagerResultFigures.figure;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.AbstractPackagerTest;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.test.assertj.PackagerResultAssert;

public class PlainPackagerHeavyItemOnGroundLevelTest extends AbstractPackagerTest {

	@Test
	void testStackHeavyItemsOnTheFloor() {
		
		int maxWeight = 2; // above this weight counts as heavy.

		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(2, 1, 3)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		PlainPackager packager = PlainPackager.newBuilder().withPlacementControlsBuilderFactory( (c) -> {
			c.withPlacementComparator(new HeavyItemsOnGroundLevelPlacementComparator(maxWeight));
			c.withBoxItemComparator(new HeavyItemsBestBoxItemComparator(maxWeight));
		}).build();
		
		try {
			List<BoxItem> products = new ArrayList<>();
	
			products.add(new BoxItem(Box.newBuilder().withId("B").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("A").withRotate3D().withSize(1, 1, 1).withWeight(3).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withId("C").withRotate3D().withSize(1, 2, 1).withWeight(1).build(), 1));

			PackagerResult build = packager.newResultBuilder().withContainerItem( b -> {
				b.withContainerItem(new ContainerItem(container, 1));
				
				// strictly not necessary but included
				b.withPointControlsBuilderFactory(HeavyItemsOnGroundLevelPointControls.newFactory(maxWeight));
			}).withBoxItems(products).build();
			// <figure>
			//   z                         z                         y                         z
			//                             3 +---------------+       1 +---------------+       3 +-------+
			//   | /---------------|         |               |         |               |         |       |
			//   |/               /|         |       C       |         |       C       |         |   C   |
			// 3 |---------------| |         |               |         |               |         |       |
			//   |               | |       2 +---------------+       0 +---------------+       2 +-------+
			//   |       C       | |         |               |         0       1       2   x     |       |
			//   |               |/|         |       B       |                                   |   B   |
			// 2 |---------------| |         |               |                                   |       |
			//   |               | |       1 +-------+-------+                                 1 +-------+
			//   |       B       | |   y     |       |                                           |       |
			//   |               |/          |   A   |                                           |   A   |
			// 1 |-------|-------|   /       |       |                                           |       |
			//   |       | |        /      0 +-------+                                         0 +-------+
			//   |   A   | |       / 1       0       1       2   x                               0       1   y
			//   |       |/       /
			// 0 |-------|---------- x
			//   0       1       2
			// </figure>
			figure(build);
			
			assertTrue(build.isSuccess());
			
			List<Container> containers = build.getContainers();
			assertEquals(containers.size(), 1);
			
			Placement firstPlacement = containers.get(0).getStack().getPlacements().get(0);
			
			// a is smaller, so it should not be stacked first if not for heavy constraints.
			assertEquals("A", firstPlacement.getStackValue().getBox().getId());
			assertEquals(0, firstPlacement.getAbsoluteZ());
			
			PackagerResultAssert.assertThat(build).isStackedWithinConstraints();
		} finally {
			packager.close();
		}
	}
	
}
