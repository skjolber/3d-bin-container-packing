package com.github.skjolber.packing.packer;

import static com.github.skjolber.packing.test.assertj.ContainerAssert.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.StackableItem;

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

	protected static StackableItem box(int l, int w, int h, int count) {
		return new StackableItem(Box.newBuilder().withRotate3D().withSize(l, w, h).withWeight(0).build(), count);

	}

}
