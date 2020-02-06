package com.github.skjolber.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Warmup;


@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 2, time = 10, timeUnit = TimeUnit.SECONDS)
public class PackagerBenchmark {

    @Benchmark
    public Object parallelPackagerNoDeadline(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackager().pack(state.getProducts());
    }

    @Benchmark
    public Object parallelPackagerDeadline(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackager().pack(state.getProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object parallelPackagerDeadlineNth(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackagerNth().pack(state.getProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerNoDeadline(PackagerState state) throws Exception {
    	return state.getBruteForcePackager().pack(state.getProducts());
    }

    @Benchmark
    public Object packagerDeadline(PackagerState state) throws Exception {
    	return state.getBruteForcePackager().pack(state.getProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerDeadlineNth(PackagerState state) throws Exception {
    	return state.getBruteForcePackagerNth().pack(state.getProducts(), System.currentTimeMillis() + 10000);
    }

}
