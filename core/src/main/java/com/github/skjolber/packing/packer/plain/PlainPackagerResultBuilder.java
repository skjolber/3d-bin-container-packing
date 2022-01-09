package com.github.skjolber.packing.packer.plain;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.PackagerResultBuilder;
import com.github.skjolber.packing.deadline.BooleanSupplierBuilder;

public class PlainPackagerResultBuilder extends PackagerResultBuilder<PlainPackagerResultBuilder> {

	private AbstractPlainPackager<?> packager;

	private int checkpointsPerDeadlineCheck = 1;
	
	public PlainPackagerResultBuilder withPackager(AbstractPlainPackager<?> packager) {
		this.packager = packager;
		return this;
	}

	public PlainPackagerResultBuilder withCheckpointsPerDeadlineCheck(int n) {
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
		
		BooleanSupplier build = BooleanSupplierBuilder.builder().withDeadline(deadline, checkpointsPerDeadlineCheck).withInterrupt(interrupt).build();
		
		List<Container> packList;
		if(maxResults > 1) {
			packList = packager.packList(items, maxResults, build);
		} else {
			Container result = packager.pack(items, build);
			
			packList = Arrays.asList(result);
		}
		return new PackagerResult(packList, System.currentTimeMillis() - start);
	}
	
}
