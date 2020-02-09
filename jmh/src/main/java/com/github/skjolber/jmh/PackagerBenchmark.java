package com.github.skjolber.jmh;

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
@Measurement(iterations = 1, time = 10, timeUnit = TimeUnit.SECONDS)
public class PackagerBenchmark {

    @Benchmark
    public Object parallelRotationPackagerNoDeadline(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackager().pack(state.getIdentialProducts());
    }

    @Benchmark
    public Object parallelRotationPackagerDeadline(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackager().pack(state.getIdentialProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object parallelRotationPackagerDeadlineNth(PackagerState state) throws Exception {
    	return state.getParallelBruteForcePackagerNth().pack(state.getIdentialProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerRotationNoDeadline(PackagerState state) throws Exception {
    	return state.getBruteForcePackager().pack(state.getIdentialProducts());
    }

    @Benchmark
    public Object packagerRotationDeadline(PackagerState state) throws Exception {
    	return state.getBruteForcePackager().pack(state.getIdentialProducts(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerRotationDeadlineNth(PackagerState state) throws Exception {
    	return state.getBruteForcePackagerNth().pack(state.getIdentialProducts(), System.currentTimeMillis() + 10000);
    }

}
