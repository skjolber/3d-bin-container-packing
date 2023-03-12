package com.github.skjolber.packing.packer.bruteforce;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;

public class FastBruteForcePackagerResultBuilder extends PackagerResultBuilder<FastBruteForcePackagerResultBuilder> {

	private FastBruteForcePackager packager;

	private int checkpointsPerDeadlineCheck = 1;

	public FastBruteForcePackagerResultBuilder withPackager(FastBruteForcePackager packager) {
		this.packager = packager;
		return this;
	}

	public FastBruteForcePackagerResultBuilder withCheckpointsPerDeadlineCheck(int n) {
		this.checkpointsPerDeadlineCheck = n;
		return this;
	}

	public PackagerResult build() {
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
		if(maxResults > 1) {
			packList = packager.packList(items, containerInventory, maxResults, build);
		} else {
			Container result = packager.pack(items, containerInventory, build);

			if(result != null) {
				packList = Arrays.asList(result);
			} else {
				packList = Collections.emptyList();
			}
		}
		return new PackagerResult(packList, System.currentTimeMillis() - start);
	}

}
