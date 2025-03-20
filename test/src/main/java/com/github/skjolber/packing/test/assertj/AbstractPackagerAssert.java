package com.github.skjolber.packing.test.assertj;

import java.util.List;

import org.assertj.core.api.AbstractObjectAssert;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Packager;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;

@SuppressWarnings("rawtypes")
public abstract class AbstractPackagerAssert<SELF extends AbstractPackagerAssert<SELF, ACTUAL>, ACTUAL extends Packager>
		extends AbstractObjectAssert<SELF, ACTUAL> {

	private static final long LEEWAY = 10;

	protected AbstractPackagerAssert(ACTUAL actual, Class<?> selfType) {
		super(actual, selfType);
	}

	@SuppressWarnings("unchecked")
	public SELF respectsDeadline(List<ContainerItem> containerItems, List<BoxItem> items, long maxTime) {
		isNotNull();

		long timestamp = System.currentTimeMillis();
		PackagerResult result = actual.newResultBuilder().withDeadline(timestamp + maxTime).withStackables(items).withContainers(containerItems).build();
		long packDuration = System.currentTimeMillis() - timestamp;

		if(result.getContainers().isEmpty()) {
			failWithMessage("Unable to pack " + items.size() + " items using " + actual.getClass().getName());
		}

		if(packDuration < LEEWAY) {
			failWithMessage("Pack duration too short for " + actual.getClass().getName());
		}

		int divider = 4;
		while (divider < 10) {
			timestamp = System.currentTimeMillis();
			long unrealisticDuration = packDuration / divider;
			result = actual.newResultBuilder().withDeadline(timestamp + unrealisticDuration).withStackables(items).withContainers(containerItems).build();
			if(!result.getContainers().isEmpty()) {
				continue;
			}
			long elapsed = System.currentTimeMillis() - timestamp;

			if(elapsed >= unrealisticDuration + LEEWAY) {
				failWithMessage("Expected packager " + actual.getClass().getName() + " exited before " + unrealisticDuration + "ms, but existed after " + elapsed + "ms");
			}

			return myself;
		}
		failWithMessage("Unexpectedly was able to pack using " + actual.getClass().getName());

		return myself;
	}

}
