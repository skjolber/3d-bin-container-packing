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

@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class BouwkampCodePackagerBenchmark {

	
    @Benchmark
    public int parallelPackager(BouwkampCodePackagerState state) throws Exception {
    	return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
    }

    @Benchmark
    public int packager(BouwkampCodePackagerState state) throws Exception {
    	return process(state.getBruteForcePackager(), Long.MAX_VALUE);
    }
    
    @Benchmark
    public int fastPackager(BouwkampCodePackagerState state) throws Exception {
    	return process(state.getFastBruteForcePackager(), Long.MAX_VALUE);
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
                .include(BouwkampCodePackagerBenchmark.class.getSimpleName())
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
