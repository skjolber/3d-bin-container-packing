package com.github.skjolber.packing.test.ascii;

/**
 * As an abstract base class of several tests, with a helper method which records figures.
 */
abstract class RecorderBaseClass {

	/**
	 * @return the line which {@linkplain FigureRecorder} would record the figure for
	 */
	protected StackTraceElement figure() {
		return FigureRecorder.getCaller(new Throwable().getStackTrace());
	}
}
