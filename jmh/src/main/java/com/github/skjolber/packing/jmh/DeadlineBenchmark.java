package com.github.skjolber.packing.jmh;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
import org.openjdk.jmh.runner.options.TimeValue;


/**
 * 
 * Check the impact of using a variable deadline, avoiding doing frequent system calls.
 *
 */

@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class DeadlineBenchmark {

    @Benchmark
    public Object parallelPackagerNoDeadline(PackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
    }

    @Benchmark
    public Object parallelPackagerDeadline(PackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackager(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object parallelPackagerDeadlineNth(PackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackagerNth(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerNoDeadline(PackagerState state) throws Exception {
    	return process(state.getBruteForcePackager(), Long.MAX_VALUE);
    }

    @Benchmark
    public Object packagerDeadline(PackagerState state) throws Exception {
    	return process(state.getBruteForcePackager(), System.currentTimeMillis() + 10000);
    }

    @Benchmark
    public Object packagerDeadlineNth(PackagerState state) throws Exception {
    	return process(state.getBruteForcePackagerNth(), System.currentTimeMillis() + 10000);
    }

    public int process(List<BenchmarkSet> sets, long deadline) {
    	int i = 0;
    	for(BenchmarkSet set : sets) {
    		if(set.getPackager().pack(set.getProducts(), deadline) != null) {
    			i++;
    		}
    	}
    	
    	return i;
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(DeadlineBenchmark.class.getSimpleName())
                .mode(Mode.Throughput)
                /*
                .forks(1)
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(15))
                .timeout(TimeValue.seconds(10))
                */
                .build();

        new Runner(opt).run();
    }    

}
