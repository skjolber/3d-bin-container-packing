package com.github.skjolber.packing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class BruteForcePackagerBuilderTest {

	@Test
	public void testBuilder() {
		List<Container> containers = new ArrayList<>();
		Container container = new Container("X", 100, 36, 5, 1000);
		containers.add(container);

		assertNotNull(BruteForcePackager.newBuilder().withContainers(containers).build());
	}
}
