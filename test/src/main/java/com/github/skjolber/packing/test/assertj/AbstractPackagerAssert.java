package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;

@SuppressWarnings("rawtypes")
public abstract class AbstractPackagerAssert<SELF extends AbstractPackagerAssert<SELF, ACTUAL>, ACTUAL extends Packager>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private static final long LEEWAY = 250;

	protected AbstractPackagerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	@SuppressWarnings("unchecked")
	public SELF respectsDeadline(List<ContainerItem> containerItems, List<BoxItem> items, long maxTime) {
		isNotNull();

		long timestamp = System.currentTimeMillis();
		
		PackagerResultBuilder  builder = actual.newResultBuilder();
		PackagerResult result = builder.withDeadline(timestamp + maxTime).withBoxItems(items).withContainerItems(containerItems).build();
		long packDuration = System.currentTimeMillis() - timestamp;

		if(result.getContainers().isEmpty()) {
			failWithMessage("Unable to pack " + items.size() + " items using " + actual.getClass().getName());
		}

		// shorter deadlines, halved until the packager gives up: it must not take much longer than the deadline
		for(long unrealisticDuration = packDuration / 4; ; unrealisticDuration /= 2) {
			timestamp = System.currentTimeMillis();
			result = actual.newResultBuilder().withDeadline(timestamp + unrealisticDuration).withBoxItems(items).withContainerItems(containerItems).build();
			if(result.getContainers().isEmpty()) {
				long elapsed = System.currentTimeMillis() - timestamp;
				if(elapsed >= unrealisticDuration + LEEWAY) {
					failWithMessage("Expected packager " + actual.getClass().getName() + " exited before " + unrealisticDuration + "ms, but existed after " + elapsed + "ms");
				}
				return myself;
			}
			if(unrealisticDuration == 0) {
				// packed although the deadline had passed
				failWithMessage("Unexpectedly was able to pack using " + actual.getClass().getName());
				return myself;
			}
		}
	}

}
