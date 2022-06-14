package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.assertj.ContainerAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.github.skjolber.packing.api.Container;

public class AbstractPackagerTest {

	protected static void assertValid(List<Container> containers) {
		assertNotNull(containers);
		for (Container container : containers) {
			assertValid(container);
		}
	}
	
	protected static void assertValid(Container container) {
		assertNotNull(container);
		assertThat(container).isStackedWithinContraints();
	}
	
}
