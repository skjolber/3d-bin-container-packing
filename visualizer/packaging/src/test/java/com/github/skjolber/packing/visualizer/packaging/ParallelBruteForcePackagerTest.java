package com.github.skjolber.packing.visualizer.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeDirectory;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class ParallelBruteForcePackagerTest extends AbstractPackagerTest {

	@Test
	public void testSimpleImperfectSquaredRectangles() throws Exception {
		// if you do not have a lot of CPU cores, this will take quite some time

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().withExecutorService(executorService).withParallelizationCount(256).build();

		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		int level = 10;

		pack(directory.getSimpleImperfectSquaredRectangles(level), packager);

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimpleImperfectSquaredSquares(level), packager);

		directory = BouwkampCodeDirectory.getInstance();

		pack(directory.getSimplePerfectSquaredRectangles(level), packager);
	}

	@Test
	public void testBowcampCodes() throws Exception {
		BouwkampCodeDirectory directory = BouwkampCodeDirectory.getInstance();

		List<BouwkampCodes> codes = directory.codesForCount(9);

		BouwkampCodes bouwkampCodes = codes.get(0);

		BouwkampCode bouwkampCode = bouwkampCodes.getCodes().get(0);

		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1)
						.withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build(), 1)
				.build();

		ParallelBruteForcePackager packager = ParallelBruteForcePackager.newBuilder().build();

		List<BoxItem> products = new ArrayList<>();

		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			List<Integer> squares = bouwkampCodeLine.getSquares();

			for (Integer square : squares) {
				products.add(new BoxItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), 1));
			}
		}

		PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).withMaxContainerCount(1).build();

		Container fits = result.get(0);
		assertNotNull(fits);
		assertEquals(fits.getStack().getSize(), products.size());

		write(fits);
	}

}
