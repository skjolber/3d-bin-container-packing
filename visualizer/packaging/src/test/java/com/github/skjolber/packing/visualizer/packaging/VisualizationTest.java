package com.github.skjolber.packing.visualizer.packaging;

import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.DefaultContainer;
import com.github.skjolber.packing.api.DefaultStack;
import com.github.skjolber.packing.api.StackableItem;
import com.github.skjolber.packing.packer.bruteforce.BruteForcePackager;
import com.github.skjolber.packing.packer.bruteforce.FastBruteForcePackager;
import com.github.skjolber.packing.packer.laff.FastLargestAreaFitFirstPackager;

public class VisualizationTest {

	@Test
	public void testPackager() throws Exception {


		List<Container> containers = new ArrayList<>();
		
		DefaultContainer container = Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 2, 1, 3, 2, 1, 100, null).withStack(new DefaultStack()).build();
		containers.add(container);
		
		
		FastLargestAreaFitFirstPackager packager = FastLargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(2, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		
		System.out.println(fits.getStack().getPlacements());
		System.out.println(container);
		ContainerProjection p = new ContainerProjection();
		
		List<Container> a = Arrays.asList(fits);

		System.out.println(a);
		
		File file = new File("../viewer/public/assets/containers.json");
		p.project(a , file);
	}
	

	@Test
	public void testStackingBox1() throws Exception {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotateXYZ(5, 5, 1, 5, 5, 1, 100, null).withStack(new DefaultStack()).build());
		
		BruteForcePackager packager = BruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("D").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("E").withRotateXYZ(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		ContainerProjection p = new ContainerProjection();
		
		List<Container> a = Arrays.asList(fits);

		System.out.println(a);
		
		File file = new File("../viewer/public/assets/containers.json");
		p.project(a , file);
	}
	
	@Test
	public void testStackingBox2() throws Exception {
		List<Container> containers = new ArrayList<>();
		
		containers.add(Container.newBuilder().withName("Container").withEmptyWeight(1).withRotateXYZ(5, 5, 1, 5, 5, 1, 100, null).withStack(new DefaultStack()).build());
		
		FastBruteForcePackager packager = FastBruteForcePackager.newBuilder().withContainers(containers).build();

		List<StackableItem> products = new ArrayList<>();

		products.add(new StackableItem(Box.newBuilder().withName("A").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("B").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("C").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("D").withRotateXYZ(3, 2, 1).withWeight(1).build(), 1));
		products.add(new StackableItem(Box.newBuilder().withName("E").withRotateXYZ(1, 1, 1).withWeight(1).build(), 1));

		Container fits = packager.pack(products);
		assertNotNull(fits);
		System.out.println(fits.getStack().getPlacements());
		
		ContainerProjection p = new ContainerProjection();
		
		List<Container> a = Arrays.asList(fits);

		System.out.println(a);
		
		File file = new File("../viewer/public/assets/containers.json");
		p.project(a , file);
	}
}
