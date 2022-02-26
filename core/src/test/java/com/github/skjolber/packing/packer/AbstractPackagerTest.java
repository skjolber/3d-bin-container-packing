package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import static com.github.skjolber.packing.test.assertj.ContainerAssert.*;
import static org.junit.Assert.assertNotNull;

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
