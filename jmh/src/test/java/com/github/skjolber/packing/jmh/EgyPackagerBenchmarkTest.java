package com.github.skjolber.packing.jmh;

import static org.junit.Assert.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

public class EgyPackagerBenchmarkTest {

	private EgyPackagerState state;
	
	@BeforeEach 
	public void init() {
		state = new EgyPackagerState();
		state.init();
	}
	
	@Test
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
    	for(BenchmarkSet set : sets) {
    		assertNotNull(set.getPackager().pack(set.getProducts(), deadline));
    	}
    }
    
    @AfterEach
    public void shutdown() throws InterruptedException {
    	if(state != null) {
    		state.shutdown();
    	}
    }

}
