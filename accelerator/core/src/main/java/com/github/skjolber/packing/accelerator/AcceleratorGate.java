package com.github.skjolber.packing.accelerator;

/**
 * The gate of {@link AcceleratorMode#AUTO}: how long the packager searches a container alone before the accelerator joins, and what the races teach
 * it. {@link AdaptiveAcceleratorGate} is the default, {@link FixedAcceleratorGate} keeps one gate; an implementation of your own gets the outcome of every
 * race. It is called from more than one thread at a time, so it must be thread-safe.
 */
public interface AcceleratorGate {

	/** how a race ended */
	enum Outcome {
		/** the packager finished before the gate: the accelerator never searched */
		PACKAGER_ALONE,
		/** the accelerator joined, but the packager finished first, or the accelerator gave up or its winner was not confirmed */
		PACKAGER,
		/** the accelerator joined and its winner was taken */
		ACCELERATOR
	}

	/**
	 * @param problem the container's problem, which the accelerator supports
	 * @return the time the packager searches the container alone before the accelerator joins, in nanoseconds; zero for at once
	 */
	long gateNanos(FlatProblem problem);

	/**
	 * A race ended.
	 *
	 * @param problem the container's problem
	 * @param outcome how it ended
	 * @param nanos the time of the race, from the start of the packager's own search to the result
	 */
	void raced(FlatProblem problem, Outcome outcome, long nanos);
}
