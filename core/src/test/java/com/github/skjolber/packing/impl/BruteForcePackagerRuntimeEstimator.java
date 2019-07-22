package com.github.skjolber.packing.impl;

import com.github.skjolber.packing.*;
import com.github.skjolber.packing.impl.DefaultPermutationRotationIterator;
import com.github.skjolber.packing.impl.PermutationRotation;

import java.util.ArrayList;
import java.util.List;

public class BruteForcePackagerRuntimeEstimator {

	private static class Measurement {
		private long rotations;
		private long permutations;
		private long duration;
		Measurement(long rotations, long permutations, long duration) {
			super();
			this.rotations = rotations;
			this.permutations = permutations;
			this.duration = duration;
		}
		@Override
		public String toString() {
			return "Measurement [rotations=" + rotations + ", permutations=" + permutations + ", duration=" + duration
					+ "]";
		}
	}

	private static class BruteForcePackagerEstimator extends BruteForcePackager {

		BruteForcePackagerEstimator(List<Container> containers, boolean rotate3d, boolean binarySearch) {
			super(containers, rotate3d, binarySearch);
		}

		@Override
		protected boolean accept() {
			return false;
		}
	}

	public static void main(String[] args) {

		long runDuration = 60 * 10;

		// n! permutations
		// 6 rotations per box
		// so something like n! * 6^n combinations, each needing to be stacked
		//
		// anyways my laptop cannot do more than perhaps 10 within 5 seconds
		// on a single thread and this is quite a simple scenario

		System.out.println("Run for " + runDuration + " seconds");

		long deadline = System.currentTimeMillis() + runDuration * 1000;

		// warmup
		run(deadline, 4, 8);

		List<Measurement> costs = run(deadline, 4, 8);
		System.out.println("Found " + costs.size() + " cost measurements");

		long maxTime = 5000;

		int n = 1;
		while(deadline > System.currentTimeMillis() && n < 20) {
			List<Container> containers = new ArrayList<>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new BruteForcePackagerEstimator(containers, true, true);

			for(int k = 1; k <= n; k++) {
				List<BoxItem> products1 = new ArrayList<>();

				int spent = 0;
				while(spent < n) {
					Box box = new Box(5, 10, 10, 0);
					int count = Math.min(k, n - spent);
					products1.add(new BoxItem(box, count));
					spent += count;
				}

				PermutationRotation[] rotationMatrix = DefaultPermutationRotationIterator.toRotationMatrix(products1, true);

				DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(containers.get(0), rotationMatrix);
				if(iterator.length() != n) {
					throw new RuntimeException(iterator.length() +" != " + n + " for " + products1.size());
				}
				long countPermutations = iterator.countPermutations();
				long countRotations = iterator.countRotations();

				if(countPermutations == -1L) {
					System.out.println(n + "@" + k + " for infinite permutation complexity");

					continue;
				}
				if(countRotations == -1L) {
					System.out.println(n + "@" + k + " for infinite rotation complexity");

					continue;
				}

				if(Long.MAX_VALUE / countPermutations <= countRotations) {
					System.out.println(n + "@" + k + " for infinite combined complexity");

					continue;
				}


				long complexity = iterator.countPermutations() * iterator.countRotations();

				long eta = estimate(costs, iterator.countPermutations(), iterator.countRotations());

				if(eta == -1L || eta > maxTime) {
					System.out.println("Skip " + n + "@" + k + " for too long processing time (" + eta + ")");

					continue;
				}
				long time = System.nanoTime();
				Container container = bruteForcePackager.pack(products1, deadline);
				if(container != null) {
					throw new IllegalArgumentException();
				}
				long duration = (System.nanoTime() - time);
				if(duration > 1) {
					System.out.println(n + "@" + k + " in " + (duration) / 1000000 + " (vs " + eta + ") for complexity " + complexity + " = " + (duration / complexity) + "ns per complexity");
				} else {
					System.out.println(n + "@" + k + " in " + (duration) / 1000000 + " for complexity " + complexity);
				}

			}


			n++;
		}


	}

	private static long estimate(List<Measurement> estimates, long countPermutations, long countRotations) {
		long totalComplexity = 0;
		long totalDuration = 0;

		for (Measurement second : estimates) {
			totalComplexity += second.permutations * second.rotations;
			totalDuration += second.duration;
		}

		long durationPerComplexity = totalDuration / totalComplexity;

		if(Long.MAX_VALUE / (countPermutations * countRotations) < durationPerComplexity) {
			return -1L;
		}

		return (durationPerComplexity * (countPermutations * countRotations)) / (1000 * 1000);
	}

	private static List<Measurement> run(long deadline, int min, int max) {
		List<Measurement> estimates = new ArrayList<>();

		long totalComplexity = 0;
		long totalDuration = 0;

		int n = min;
		while(deadline > System.currentTimeMillis() && n < max) {
			List<Container> containers = new ArrayList<>();
			containers.add(new Container(5 * n, 10, 10, 0));
			Packager bruteForcePackager = new BruteForcePackagerEstimator(containers, true, true);

			for(int k = 1; k <= n; k++) {
				List<BoxItem> products1 = new ArrayList<>();

				int spent = 0;
				while(spent < n) {
					Box box = new Box(5, 10, 10, 0);
					int count = Math.min(k, n - spent);
					products1.add(new BoxItem(box, count));
					spent += count;
				}

				PermutationRotation[] rotationMatrix = DefaultPermutationRotationIterator.toRotationMatrix(products1, true);

				DefaultPermutationRotationIterator iterator = new DefaultPermutationRotationIterator(containers.get(0), rotationMatrix);
				if(iterator.length() != n) {
					throw new RuntimeException(iterator.length() +" != " + n + " for " + products1.size());
				}
				long countPermutations = iterator.countPermutations();
				long countRotations = iterator.countRotations();

				if(countPermutations == -1L) {
					System.out.println(n + "@" + k + " for infinite permutation complexity");

					continue;
				}
				if(countRotations == -1L) {
					System.out.println(n + "@" + k + " for infinite rotation complexity");

					continue;
				}

				if(Long.MAX_VALUE / countPermutations <= countRotations) {
					System.out.println(n + "@" + k + " for infinite combined complexity");

					continue;
				}


				long complexity = iterator.countPermutations() * iterator.countRotations();

				long time = System.nanoTime();
				Container container = bruteForcePackager.pack(products1, deadline);
				if(container != null) {
					throw new IllegalArgumentException();
				}
				long duration = (System.nanoTime() - time);
				if(duration > 1) {
					totalComplexity += complexity;
					totalDuration += duration;

					estimates.add(new Measurement(countRotations, countPermutations, duration));

					System.out.println(n + "@" + k + " in " + (System.nanoTime() - time) / 1000000 + " for complexity " + complexity + " = " + (duration / complexity) + " average " + (totalDuration / totalComplexity) + "ns per complexity");
				} else {
					System.out.println(n + "@" + k + " in " + (System.nanoTime() - time) / 1000000 + " for complexity " + complexity);
				}

			}


			n++;
		}

		return  estimates;
	}

}
