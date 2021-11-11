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
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;

public class VisualizationTest {

	@Test
	public void testPackager() throws Exception {


		List<Container> containers = new ArrayList<>();
		
		DefaultContainer container = Container.newBuilder().withName("1").withEmptyWeight(1).withRotate(3, 2, 1, 3, 2, 1, 100, null).withStack(new DefaultStack()).build();
		containers.add(container);
		
		
		LargestAreaFitFirstPackager packager = LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build();
		
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
}
