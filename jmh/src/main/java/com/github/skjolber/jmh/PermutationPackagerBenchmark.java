package com.github.skjolber.jmh;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;


@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class PermutationPackagerBenchmark {

    @Benchmark
    public Object parallelPackagerNoDeadline(PermutationPackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
    }

    @Benchmark
    public Object parallelPackagerDeadline(PermutationPackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackager(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object parallelPackagerDeadlineNth(PermutationPackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackagerNth(), System.currentTimeMillis() + 10000);
    }
    
    @Benchmark
    public Object packagerNoDeadline(PermutationPackagerState state) throws Exception {
    	return process(state.getBruteForcePackager(), Long.MAX_VALUE);
    }

    @Benchmark
    public Object packagerDeadline(PermutationPackagerState state) throws Exception {
    	return process(state.getBruteForcePackager(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerDeadlineNth(PermutationPackagerState state) throws Exception {
    	return process(state.getBruteForcePackagerNth(), System.currentTimeMillis() + 10000);
    }
    
    public int process(List<BenchmarkSet> sets, long deadline) {
    	int i = 0;
    	for(BenchmarkSet set : sets) {
    		if(set.getPackager().pack(set.getProducts()) != null) {
    			i++;
    		}
    	}
    	
    	return i;
    }

}
