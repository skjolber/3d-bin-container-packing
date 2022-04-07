package com.github.skjolber.packing.packer;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Dimension;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.junit.runner.RunWith;

import static java.util.Collections.singletonList;

@RunWith(JUnitQuickcheck.class)
public abstract class AbstractPackagerProperties extends AbstractPackagerTest {
	protected abstract AbstractPackager<?,?> buildPackager(Container container);

	@Property
	public void eightBoxes2x2x2(@From(DimensionGenerator.class) Dimension boxSize,
															@InRange(min = "0", max = "9") int xVariation,
															@InRange(min = "0", max = "9") int yVariation,
															@InRange(min = "0", max = "9") int zVariation
	) {
		final int count = 8;
		Dimension containerSize = Dimension.newInstance(
			2 * boxSize.getDx() + xVariation,
			2 * boxSize.getDy() + yVariation,
			2 * boxSize.getDz() + zVariation);

		runTest(containerSize, boxSize, count
		);
	}

	@Property
	public void fourBoxes2x2x1(@From(DimensionGenerator.class) Dimension boxSize,
														 @InRange(min = "0", max = "9") int xVariation,
														 @InRange(min = "0", max = "9") int yVariation,
														 @InRange(min = "0", max = "9") int zVariation) {
		final int count = 4;
		Dimension containerSize = Dimension.newInstance(
			boxSize.getDx() + xVariation,
			2 * boxSize.getDy() + yVariation,
			2 * boxSize.getDz() + zVariation);

		runTest(containerSize, boxSize, count
		);
	}

	private void runTest(final Dimension containerSize, final Dimension boxSize, final int count) {
		final Container container = Container.newBuilder()
			.withDescription(containerSize.encode())
			.withEmptyWeight(0)
			.withSize(containerSize.getDx(), containerSize.getDy(), containerSize.getDz())
			.withMaxLoadWeight(count)
			.withStack(new ValidatingStack())
			.build();
		AbstractPackager<?, ?> packager = buildPackager(container);

		final Box box = Box.newBuilder()
			.withDescription(boxSize.encode())
			.withRotate3D()
			.withSize(boxSize.getDx(), boxSize.getDy(), boxSize.getDz())
			.withWeight(1)
			.build();
		Container fits = packager.pack(singletonList(new StackableItem(box, count)));
		assertValid(fits);
	}


}
