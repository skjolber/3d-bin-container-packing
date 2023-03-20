package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;

public class BruteForcePackagerResultBuilder extends PackagerResultBuilder<BruteForcePackagerResultBuilder> {

	private AbstractBruteForcePackager packager;

	private int checkpointsPerDeadlineCheck = 1;

	public BruteForcePackagerResultBuilder withPackager(AbstractBruteForcePackager packager) {
		this.packager = packager;
		return this;
	}

	public BruteForcePackagerResultBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}

	public PackagerResult build() {
		if(maxContainerCount <= 0) {
			throw new IllegalStateException();
		}
		if(containers == null) {
			throw new IllegalStateException();
		}
		if(items == null) {
			throw new IllegalStateException();
		}
		long start = System.currentTimeMillis();

		PackagerInterruptSupplierBuilder booleanSupplierBuilder = PackagerInterruptSupplierBuilder.builder();
		if(deadline != -1L) {
			booleanSupplierBuilder.withDeadline(deadline, checkpointsPerDeadlineCheck);
		}
		if(interrupt != null) {
			booleanSupplierBuilder.withInterrupt(interrupt);
		}

		PackagerInterruptSupplier build = booleanSupplierBuilder.build();
		List<Container> packList = packager.pack(items, containers, maxContainerCount, build);
		long duration = System.currentTimeMillis() - start;
		if(packList == null) {
			return new PackagerResult(Collections.emptyList(), duration, true);
		}
		return new PackagerResult(packList, duration, false);
	}

}
