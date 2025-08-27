package com.github.skjolber.packing.visualizer.packaging;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.packer.AbstractPackager;
import com.github.skjolber.packing.packer.bruteforce.DefaultThreadFactory;
import com.github.skjolber.packing.packer.bruteforce.ParallelBruteForcePackager;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCode;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodeLine;
import com.github.skjolber.packing.test.bouwkamp.BouwkampCodes;

public class AbstractPackagerTest {

	protected ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors(), new DefaultThreadFactory());

	protected void write(PackagerResult result) throws Exception {
		write(result, true);
	}

	protected void write(PackagerResult result, boolean calculatePoints) throws Exception {
		write(result.getContainers(), calculatePoints);
	}

	protected void write(Container container) throws Exception {
		write(container, true);
	}

	protected void write(Container container, boolean calculatePoints) throws Exception {
		write(Arrays.asList(container), calculatePoints);
	}

	protected void write(List<Container> packList) throws Exception {
		write(packList, true);
	}

	protected void write(List<Container> packList, boolean calculatePoints) throws Exception {
		DefaultPackagingResultVisualizerFactory p = new DefaultPackagingResultVisualizerFactory(calculatePoints);

		File file = new File("../viewer/public/assets/containers.json");
		p.visualize(packList, file);
	}

	protected <T> void pack(List<BouwkampCodes> codes, AbstractPackager packager) throws Exception {
		for (BouwkampCodes bouwkampCodes : codes) {
			for (BouwkampCode bouwkampCode : bouwkampCodes.getCodes()) {
				
				/*
				if(!bouwkampCode.getName().equals("65x47A")) {
					continue;
				}
				*/
				System.out.println("Package " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder());
				long timestamp = System.currentTimeMillis();
				pack(bouwkampCode, packager);
				System.out.println("Packaged " + bouwkampCode.getName() + " order " + bouwkampCode.getOrder() + " in " + (System.currentTimeMillis() - timestamp));

				Thread.sleep(5000);
			}
		}
	}

	protected void pack(BouwkampCode bouwkampCode, AbstractPackager packager) throws Exception {
		List<ContainerItem> containers = ContainerItem
				.newListBuilder()
				.withContainer(Container.newBuilder().withDescription("1").withEmptyWeight(1).withSize(bouwkampCode.getWidth(), bouwkampCode.getDepth(), 1)
						.withMaxLoadWeight(bouwkampCode.getWidth() * bouwkampCode.getDepth()).build(), 1)
				.build();

		List<Integer> squares = new ArrayList<>();
		for (BouwkampCodeLine bouwkampCodeLine : bouwkampCode.getLines()) {
			squares.addAll(bouwkampCodeLine.getSquares());
		}

		// map similar items to the same stack item - this actually helps a lot
		Map<Integer, Integer> frequencyMap = new TreeMap<>();
		squares.forEach(word -> frequencyMap.merge(word, 1, (v, newV) -> v + newV));

		List<BoxItem> products = new ArrayList<>();
		for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
			int square = entry.getKey();
			int count = entry.getValue();
			products.add(new BoxItem(Box.newBuilder().withDescription(Integer.toString(square)).withSize(square, square, 1).withRotate3D().withWeight(1).build(), count));
		}

		// shuffle
		//Collections.shuffle(products);

		PackagerResult result = packager.newResultBuilder().withContainerItems(containers).withBoxItems(products).withMaxContainerCount(1).build();

		Container fits = result.get(0);
		assertNotNull(bouwkampCode.getName(), fits);
		assertEquals(bouwkampCode.getName(), fits.getStack().size(), squares.size());

		for (StackPlacement stackPlacement : fits.getStack().getPlacements()) {
			BoxStackValue stackValue = stackPlacement.getStackValue();
			System.out.println(stackPlacement.getAbsoluteX() + "x" + stackPlacement.getAbsoluteY() + "x" + stackPlacement.getAbsoluteZ() + " " + stackValue.getDx() + "x" + stackValue.getDy() + "x"
					+ stackValue.getDz());
		}

		write(fits);
	}
}
