package com.github.skjolber.packing.jmh;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.StackableItem;

@State(Scope.Thread)
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 1, time = 15, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@Measurement(iterations = 1, time = 30, timeUnit = TimeUnit.SECONDS)
public class TychoBenchmark {

	private static List<StackableItem> products22 = Arrays.asList(
			new StackableItem(Box.newBuilder().withRotate3D().withSize(590, 1085, 305).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(20, 1010, 10).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(20, 1010, 10).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 1050, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(80, 650, 2150).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(475, 330, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(91, 500, 720).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(95, 650, 760).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 850, 760).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 620, 10).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 620, 10).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(90, 210, 680).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 650, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 450, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 450, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1120, 420, 70).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(215, 585, 355).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(685, 490, 870).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(600, 678, 118).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(670, 635, 654).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(570, 570, 1840).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(91, 500, 720).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(215, 585, 355).withWeight(0).build(), 1)
	);

	private static List<StackableItem> products33 = Arrays.asList(
			new StackableItem(Box.newBuilder().withRotate3D().withSize(56, 1001, 1505).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(360, 1100, 120).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(210, 210, 250).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(70, 70, 120).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 80, 80).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(20, 20, 500).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 230, 50).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 40, 50).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 60).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 32, 32).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2000, 40, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 40, 60).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(60, 90, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(56, 40, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 280, 380).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2500, 600, 80).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(125, 125, 85).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(80, 180, 360).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(25, 140, 140).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(115, 150, 170).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(76, 76, 222).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(326, 326, 249).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(70, 130, 240).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(330, 120, 490).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(9, 23, 2500).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2000, 20, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 235).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(30, 66, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(90, 610, 210).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(144, 630, 1530).withWeight(0).build(), 1)
	);

	private static List<StackableItem> products93 = Arrays.asList(
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1170, 510, 200).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 250, 70).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 850, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 420, 78).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 420, 78).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(920, 720, 110).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(530, 120, 570).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(470, 550, 190).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(470, 550, 190).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(75, 650, 1600).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(620, 160, 27).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(80, 650, 2150).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2020, 620, 32).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 850, 760).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 420, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 420, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(95, 650, 760).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 660, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 660, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2020, 660, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2020, 660, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(535, 110, 500).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 78).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(280, 800, 480).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 1050, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 150, 22).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 520, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(89, 950, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(89, 950, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(89, 950, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 460, 33).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 460, 33).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 460, 33).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 460, 33).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 650, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 650, 750).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 620, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(55, 500, 745).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(55, 500, 745).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 170, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(720, 170, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(53, 360, 950).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(53, 360, 950).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(136, 30, 140).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(136, 30, 140).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(920, 370, 36).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(920, 370, 36).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(950, 380, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(950, 380, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(950, 380, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(950, 380, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(40, 100, 165).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(16, 2500, 11).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2720, 150, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2720, 150, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2720, 150, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2720, 150, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(2720, 150, 20).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(475, 830, 40).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(680, 585, 145).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(680, 635, 864).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(100, 100, 850).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(60, 25, 560).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(60, 25, 560).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(120, 80, 45).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(60, 120, 200).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(50, 50, 230).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(18, 18, 2400).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(1000, 1000, 1000).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 200, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 200, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 200, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 200, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(300, 200, 30).withWeight(0).build(), 1),
			new StackableItem(Box.newBuilder().withRotate3D().withSize(47, 3010, 660).withWeight(0).build(), 1)
	);

	@Param(value={"22","33","93"})
	private String boxes;

	private List<StackableItem> products;

	@Setup
	public void init() throws Exception {
		switch(boxes) {
		case "22" : {
			products = products22;
			break;
		}
		case "33" : {
			products = products33;
			break;
		}
		case "93" : {
			products = products93;
			break;
		}
		}
	}

	/*
	// never finishes
	@Benchmark 
	public int parallelPackager(TychoPackagerState state) throws Exception {
		return process(state.getParallelBruteForcePackager(), Long.MAX_VALUE);
	}

	// never finishes
	@Benchmark
	public int packager(TychoPackagerState state) throws Exception {
		return process(state.getBruteForcePackager(), Long.MAX_VALUE);
	}
	*/

	@Benchmark
	public int fastPackager(TychoPackagerState state) throws Exception {
		return process(state.getFastBruteForcePackager(), Long.MAX_VALUE);
	}

	public int process(List<BenchmarkSet> sets, long deadline) {
		int i = 0;
		for(BenchmarkSet set : sets) {
			if(set.getPackager().pack(products, deadline) != null) {
				i++;
			}
		}

		return i;
	}
	
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TychoBenchmark.class.getSimpleName())
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
