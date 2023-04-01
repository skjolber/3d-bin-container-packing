package com.github.skjolber.packing.jmh.ep;

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

import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;

@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 300, timeUnit = TimeUnit.SECONDS)

@SuppressWarnings({ "rawtypes", "unchecked" })
public class ExtremePointsBenchmark {

	/*
	@Benchmark
	public int points2D(ExtremePoints2DState state) throws Exception {
		int size = 0;
		List<ExtremePoints2DEntries> entries = state.getEntries();
		for(ExtremePoints2DEntries e : entries) {
			ExtremePoints2D extremePoints2D = e.getExtremePoints2D();
			
			for (ExtremePoint2DEntry extremePointEntry : e.getEntries()) {
				extremePoints2D.add(extremePointEntry.getIndex(), extremePointEntry.getPlacement());
			}
			size += extremePoints2D.getValueCount();
			
			extremePoints2D.redo();
		}
		
		return size;
	}
	*/

	@Benchmark
	public int points3D(ExtremePoints3DState state) throws Exception {
		int size = 0;
		List<ExtremePoints3DEntries> entries = state.getEntries();
		for (ExtremePoints3DEntries e : entries) {
			ExtremePoints3D extremePoints3D = e.getExtremePoints3D();

			for (ExtremePoint3DEntry extremePointEntry : e.getEntries()) {
				extremePoints3D.add(extremePointEntry.getIndex(), extremePointEntry.getPlacement());
			}
			size += extremePoints3D.getValueCount();

			extremePoints3D.redo();
		}

		return size;
	}

	public static void main(String[] args) throws RunnerException {
		Options opt = new OptionsBuilder()
				.include(ExtremePointsBenchmark.class.getSimpleName())
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
