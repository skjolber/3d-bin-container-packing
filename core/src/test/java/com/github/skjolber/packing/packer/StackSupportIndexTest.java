package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.packager.control.placement.AbstractPlacementControls;

/**
 * The index gives the same supported area as scanning the whole stack, for a growing stack and
 * after the stack is replaced.
 */
public class StackSupportIndexTest {

	@Test
	public void sameAsScanningTheStack() {
		Random random = new Random(1);
		StackSupportIndex index = new StackSupportIndex();
		for(int run = 0; run < 20; run++) {
			// a new stack each run: the index must rebuild
			List<Placement> stack = new ArrayList<>();
			for(int i = 0; i < 60; i++) {
				stack.add(placement(random));
				for(int k = 0; k < 20; k++) {
					BoxStackValue stackValue = stackValue(random);
					int x = random.nextInt(10);
					int y = random.nextInt(10);
					int z = random.nextInt(8);
					assertThat(index.calculateAreaSupport(stack, x, y, z, stackValue))
							.isEqualTo(AbstractPlacementControls.calculateAreaSupport(stack, x, y, z, stackValue));
				}
			}
		}
	}

	private static Placement placement(Random random) {
		return new Placement(stackValue(random), 0, random.nextInt(10), random.nextInt(10), random.nextInt(6), false);
	}

	private static BoxStackValue stackValue(Random random) {
		return Box.newBuilder().withSize(1 + random.nextInt(4), 1 + random.nextInt(4), 1 + random.nextInt(2)).withWeight(1).build().getStackValue(0);
	}
}
