package com.github.skjolber.packing;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

public class LargestAreaFitFirstPackagerBuilderTest {
	
	@Test
	public void testBuilder() {
		List<Container> containers = new ArrayList<>();
		Container container = new Container("X", 100, 36, 5, 1000);
		containers.add(container);

		assertNotNull(LargestAreaFitFirstPackager.newBuilder().withContainers(containers).build());
	}
}

