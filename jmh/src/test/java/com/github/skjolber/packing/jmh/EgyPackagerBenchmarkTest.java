package com.github.skjolber.packing.jmh;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.PackagerResult;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.deadline.PackagerInterruptSupplierBuilder;
import com.github.skjolber.packing.packer.AbstractPackager;

public class EgyPackagerBenchmarkTest {

	private EgyPackagerState state;

	@BeforeEach
	public void init() {
		state = new EgyPackagerState();
		state.init();
	}

	@Test
	@Disabled
	public void parallelPackager() throws Exception {
		assertValid(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
	}

	@Test
	public void packager() throws Exception {
		assertValid(state.getBruteForcePackager(), Long.MAX_VALUE);
	}

	@Test
	public void fastPackager() throws Exception {
		assertValid(state.getFastBruteForcePackager(), Long.MAX_VALUE);
	}

	public void assertValid(List<BenchmarkSet> sets, long deadline) {
		for (BenchmarkSet set : sets) {
			AbstractPackager packager = set.getPackager();
			List<ContainerItem> containers = set.getContainers();
			List<BoxItem> products = set.getProducts();

			PackagerResult build = packager.newResultBuilder().withContainers(containers).withMaxContainerCount(1).withStackables(products).withDeadline(deadline).build();
			assertTrue(build.isSuccess());
		}
	}

	@AfterEach
	public void shutdown() throws InterruptedException {
		if(state != null) {
			state.shutdown();
		}
	}

}
