package com.github.skjolber.packing.packer.laff;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

public class LargestAreaFitFirstPackagerResultBuilder extends PackagerResultBuilder<LargestAreaFitFirstPackagerResultBuilder> {

	private AbstractLargestAreaFitFirstPackager packager;

	public LargestAreaFitFirstPackagerResultBuilder withPackager(AbstractLargestAreaFitFirstPackager packager) {
		this.packager = packager;
		return this;
	}

	public PackagerResult build() {
		if(maxContainerCount <= 0) {
			throw new IllegalStateException();
		}
		if(containers == null) {
			throw new IllegalStateException();
		}
		if(itemGroups == null) {
			throw new IllegalStateException();
		}
		long start = System.currentTimeMillis();

		PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(deadline);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}

		booleanSupplierBuilder.withScheduledThreadPoolExecutor(packager.getScheduledThreadPoolExecutor());

		PackagerInterruptSupplier build = booleanSupplierBuilder.build();
		try {
			List<Container> packList = packager.pack(itemGroups, containers, maxContainerCount, build);
			long duration = System.currentTimeMillis() - start;
			if(packList == null) {
				return new PackagerResult(Collections.emptyList(), duration, true);
			}
			return new PackagerResult(packList, duration, false);
		} finally {
			build.close();
		}
	}

}
