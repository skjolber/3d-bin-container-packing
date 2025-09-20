package com.github.skjolber.packing.packer;

import static java.util.Collections.singletonList;

import java.util.List;

import org.junit.Assert;
import org.junit.runner.RunWith;

import com.github.skjolber.packing.api.AbstractPackagerResultBuilder;
import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;

@RunWith(JUnitQuickcheck.class)
public class AbstractPackagerProperties extends AbstractPackagerTest {

	AbstractPackager<?, ?> plainPackager = PlainPackager.newBuilder().build();
	AbstractPackager<?, ?> bruteForcePackager = BruteForcePackager.newBuilder().build();
	AbstractPackager<?, ?> fastBruteForcePackager = FastBruteForcePackager.newBuilder().build();
	AbstractPackager<?, ?> parallelBruteForcePackager = ParallelBruteForcePackager.newBuilder().build();
	AbstractPackager<?, ?> largestAreaFitFirstPackager = LargestAreaFitFirstPackager.newBuilder().build();

	@Property
	public void eightBoxesTight2x2x2(@From(DimensionGenerator.class) Dimension boxSize) {
		final int count = 8;
		Dimension containerSize = Dimension.newInstance(
				2 * boxSize.getDx(),
				2 * boxSize.getDy(),
				2 * boxSize.getDz());

		System.out.println("Test " + containerSize);
		
		runTest(containerSize, boxSize, count,
				bruteForcePackager,
				fastBruteForcePackager,
				parallelBruteForcePackager
				);
	}

	@Property
	public void fiveBoxesTightRow(@From(DimensionGenerator.class) Dimension boxSize) {
		final int count = 5;
		Dimension containerSize = Dimension.newInstance(
				5 * boxSize.getDx(),
				boxSize.getDy(),
				boxSize.getDz());

		runTest(containerSize, boxSize, count,
				bruteForcePackager,
				fastBruteForcePackager,
				parallelBruteForcePackager);
	}

	@Property
	public void eightBoxesWithExtraSpace(@From(DimensionGenerator.class) Dimension boxSize,
			@InRange(min = "0", max = "9") int xVariation,
			@InRange(min = "0", max = "9") int yVariation,
			@InRange(min = "0", max = "9") int zVariation) {
		final int count = 8;
		Dimension containerSize = Dimension.newInstance(
				2 * boxSize.getDx() + xVariation,
				2 * boxSize.getDy() + yVariation,
				2 * boxSize.getDz() + zVariation);

		runTest(containerSize, boxSize, count,
				bruteForcePackager,
				fastBruteForcePackager,
				parallelBruteForcePackager);
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

		runTest(containerSize, boxSize, count,
				bruteForcePackager,
				fastBruteForcePackager,
				parallelBruteForcePackager);
	}

	private void runTest(final Dimension containerSize,
			final Dimension boxSize,
			final int count,
			AbstractPackager<?, ?>... packagers) {
		for (final AbstractPackager<?, ?> packager : packagers) {
			final Container container = Container.newBuilder()
					.withDescription(containerSize.encode())
					.withEmptyWeight(0)
					.withSize(containerSize.getDx(), containerSize.getDy(), containerSize.getDz())
					.withMaxLoadWeight(count)
					.withStack(new ValidatingStack())
					.build();

			final Box box = Box.newBuilder()
					.withId(boxSize.encode())
					.withRotate3D()
					.withSize(boxSize.getDx(), boxSize.getDy(), boxSize.getDz())
					.withWeight(1)
					.build();

			List<ContainerItem> containers = ContainerItem.newListBuilder()
					.withContainer(container, 1)
					.build();

			AbstractPackagerResultBuilder builder = packager.newResultBuilder()
					.withBoxItems(singletonList(new BoxItem(box, count)))
					.withContainerItems(containers);
			
			PackagerResult build = builder.build();
			Assert.assertTrue(packager.getClass().getSimpleName() + " is expected to pack", build.isSuccess());
			Container fits = build.get(0);
			
			// identifies which packager has failed
			Assert.assertNotNull(packager.getClass().getSimpleName() + " is expected to pack", fits);
			assertValid(fits);
		}
	}

}
