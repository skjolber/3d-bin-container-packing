package com.github.skjolber.packing.accelerator;

/** One gate for every container, whatever the races. */
public final class FixedAcceleratorGate implements AcceleratorGate {

	private final long nanos;

	/** @param nanos the gate, in nanoseconds */
	public FixedAcceleratorGate(long nanos) {
		if(nanos < 0L) {
			throw new IllegalArgumentException("Expected a gate of zero or more nanoseconds, got " + nanos);
		}
		this.nanos = nanos;
	}

	@Override
	public long gateNanos(FlatProblem problem) {
		return nanos;
	}

	@Override
	public void raced(FlatProblem problem, Outcome outcome, long nanos) {
	}

	@Override
	public String toString() {
		return "FixedAcceleratorGate[" + nanos / 1_000_000L + " ms]";
	}
}
