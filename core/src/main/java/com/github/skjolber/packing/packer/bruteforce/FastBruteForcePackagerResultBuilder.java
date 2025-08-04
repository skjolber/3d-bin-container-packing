package com.github.skjolber.packing.packer.bruteforce;

import java.util.Collections;
import java.util.List;

import com.github.skjolber.packing.api.AbstractDefaultPackagerResultBuilder;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplier;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.PackagerInterruptedException;

public class FastBruteForcePackagerResultBuilder extends AbstractDefaultPackagerResultBuilder<FastBruteForcePackagerResultBuilder> {

	private FastBruteForcePackager packager;

	public FastBruteForcePackagerResultBuilder withPackager(FastBruteForcePackager packager) {
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

		PackagerInterruptSupplier interrupt = booleanSupplierBuilder.build();
		try {
			List<Container> packList;
			if(items != null && !items.isEmpty()) {
				packList = packager.pack(items, priority, containers, maxContainerCount, interrupt);
			} else {
				packList = packager.packGroups(itemGroups, priority, containers, maxContainerCount, interrupt);
			}
			long duration = System.currentTimeMillis() - start;
			if(packList == null) {
				return new PackagerResult(Collections.emptyList(), duration, true);
			}
			return new PackagerResult(packList, duration, false);
		} catch (PackagerInterruptedException e) {
			long duration = System.currentTimeMillis() - start;
			return new PackagerResult(Collections.emptyList(), duration, true);
		} finally {
			interrupt.close();
		}
	}
	
}
