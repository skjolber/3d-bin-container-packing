package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.impl.ValidatingStack;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;

public class CompositePackagerTest extends AbstractPackagerTest {

	@Test
	void testSimpleStackingWithTwoTiers() {
		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(3, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container, 1)
				.build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();
		FastBruteForcePackager bruteForcePackager = FastBruteForcePackager.newBuilder().build();

		CompositePackager packager = CompositePackager.newBuilder()
				.withTier("cheap", plainPackager)
				.withTier("expensive", bruteForcePackager)
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();

			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 1, 1).withWeight(1).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.build();

			assertNotNull(result);
			assertFalse(result.getContainers().isEmpty());
			assertValid(result.getContainers());
		} finally {
			packager.close();
		}
	}

	@Test
	void testSingleTierPackager() {
		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(3, 2, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container, 1)
				.build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();

		CompositePackager packager = CompositePackager.newBuilder()
				.withTier("cheap", plainPackager)
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();

			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(1, 1, 1).withWeight(1).build(), 2));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.build();

			assertNotNull(result);
			assertFalse(result.getContainers().isEmpty());
			assertValid(result.getContainers());
		} finally {
			packager.close();
		}
	}

	@Test
	void testImpossiblePacking() {
		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(1, 1, 1)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container, 1)
				.build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();

		CompositePackager packager = CompositePackager.newBuilder()
				.withTier("cheap", plainPackager)
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();

			// Box is bigger than container
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(10, 10, 10).withWeight(1).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.build();

			assertNotNull(result);
			assertTrue(result.getContainers().isEmpty());
		} finally {
			packager.close();
		}
	}

	@Test
	void testMultipleContainersFit() {
		Container container = Container.newBuilder()
				.withDescription("1")
				.withEmptyWeight(1)
				.withSize(2, 2, 2)
				.withMaxLoadWeight(100)
				.withStack(new ValidatingStack())
				.build();

		List<ContainerItem> containerItems = ContainerItem
				.newListBuilder()
				.withContainer(container, 3)
				.build();

		PlainPackager plainPackager = PlainPackager.newBuilder().build();
		FastBruteForcePackager bruteForcePackager = FastBruteForcePackager.newBuilder().build();

		CompositePackager packager = CompositePackager.newBuilder()
				.withTier("cheap", plainPackager)
				.withTier("expensive", bruteForcePackager)
				.build();

		try {
			List<BoxItem> products = new ArrayList<>();

			// These boxes need multiple containers
			products.add(new BoxItem(Box.newBuilder().withDescription("A").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));
			products.add(new BoxItem(Box.newBuilder().withDescription("B").withRotate3D().withSize(2, 2, 2).withWeight(1).build(), 1));

			PackagerResult result = packager.newResultBuilder()
					.withContainerItems(containerItems)
					.withBoxItems(products)
					.withMaxContainerCount(3)
					.build();

			assertNotNull(result);
			assertFalse(result.getContainers().isEmpty());
			assertValid(result.getContainers());
		} finally {
			packager.close();
		}
	}

}
