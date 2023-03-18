package com.github.skjolber.packing.packer.laff;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;

public class LargestAreaFitFirstPackagerResultBuilder extends PackagerResultBuilder<LargestAreaFitFirstPackagerResultBuilder> {

	private AbstractLargestAreaFitFirstPackager<?> packager;

	private int checkpointsPerDeadlineCheck = 1;

	public LargestAreaFitFirstPackagerResultBuilder withPackager(AbstractLargestAreaFitFirstPackager<?> packager) {
		this.packager = packager;
		return this;
	}

	public LargestAreaFitFirstPackagerResultBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}

	public PackagerResult build() {
		if(containers == null) {
			throw new IllegalStateException();
		}
		if(items == null) {
			throw new IllegalStateException();
		}
		long start = System.currentTimeMillis();

		BooleanSupplierBuilder booleanSupplierBuilder = BooleanSupplierBuilder.builder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(start, checkpointsPerDeadlineCheck);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}

		BooleanSupplier build = booleanSupplierBuilder.build();

		List<Container> packList;
		if(maxContainerCount > 1) {
			packList = packager.packList(items, containers, maxContainerCount, build);
		} else {
			Container result = packager.pack(items, containers, build);

			if(result != null) {
				packList = Arrays.asList(result);
			} else {
				packList = Collections.emptyList();
			}
		}
		return new PackagerResult(packList, System.currentTimeMillis() - start);
	}

}
