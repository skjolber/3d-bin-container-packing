package com.github.skjolber.packing.accelerator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A gate per box count which adapts to the races: the gate of a box count starts at the floor, doubles after a race which the accelerator joined and
 * lost, and halves after one it won, between the floor and {@link #CAP_FACTOR} times it. Where the accelerator keeps losing it soon joins only the
 * containers which run far longer than the rest, where it keeps winning it joins at the floor. A race which the packager finishes before the gate
 * teaches nothing.
 */
public final class AdaptiveAcceleratorGate implements AcceleratorGate {

	/** the gate of a box count grows to at most this many times the floor (a millisecond, when the floor is zero) */
	public static final int CAP_FACTOR = 1024;

	/** the floor: the gate of every box count at first, and the least it becomes */
	private final long floorNanos;
	/** the least a gate becomes after a lost race, and the unit of the cap */
	private final long stepNanos;
	/** the gate of each box count: only the box counts of a lost race are present */
	private final ConcurrentHashMap<Integer, AtomicLong> gates = new ConcurrentHashMap<>();

	/** @param floorNanos the gate of every box count at first, and the least it becomes, in nanoseconds */
	public AdaptiveAcceleratorGate(long floorNanos) {
		if(floorNanos < 0L) {
			throw new IllegalArgumentException("Expected a gate of zero or more nanoseconds, got " + floorNanos);
		}
		this.floorNanos = floorNanos;
		this.stepNanos = Math.max(floorNanos, 1_000_000L);
	}

	@Override
	public long gateNanos(FlatProblem problem) {
		return gateNanos(problem.n());
	}

	/** @return the gate for the containers of a box count, in nanoseconds: the floor, or what the races of the box count made of it */
	public long gateNanos(int boxes) {
		AtomicLong gate = gates.get(boxes);
		return gate == null ? floorNanos : gate.get();
	}

	@Override
	public void raced(FlatProblem problem, Outcome outcome, long nanos) {
		int boxes = problem.n();
		if(outcome == Outcome.PACKAGER) {
			// the accelerator joined and the packager finished first: the gate of the box count doubles
			gates.computeIfAbsent(boxes, key -> new AtomicLong(floorNanos)).updateAndGet(gate -> Math.min(stepNanos * CAP_FACTOR, Math.max(gate * 2, stepNanos)));
		} else if(outcome == Outcome.ACCELERATOR) {
			// the accelerator won: the gate of the box count halves, down to the floor
			AtomicLong gate = gates.get(boxes);
			if(gate != null) {
				gate.updateAndGet(value -> {
					long half = value / 2;
					return half < stepNanos ? floorNanos : half;
				});
			}
		}
	}

	@Override
	public String toString() {
		return "AdaptiveAcceleratorGate[floor " + floorNanos / 1_000_000L + " ms, " + gates + "]";
	}
}
