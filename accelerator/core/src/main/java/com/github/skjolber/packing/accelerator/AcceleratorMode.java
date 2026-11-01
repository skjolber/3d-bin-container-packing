package com.github.skjolber.packing.accelerator;

/**
 * How a packager with a {@link BruteForceAccelerator} uses it for the containers the accelerator applies to.
 */
public enum AcceleratorMode {
	/** the packager searches every container itself; the accelerator is not used */
	CPU_ONLY,
	/** the accelerator searches every container it applies to, and the packager searches itself only where the accelerator gives up */
	GPU_ONLY,
	/**
	 * the packager starts its own search at once, and after a gate (a few tens of milliseconds, the time the accelerator needs for any search) the
	 * accelerator searches the same container alongside; the first result wins and stops the other. The easy containers never reach the
	 * accelerator, the hard ones take the faster of the two.
	 */
	AUTO
}
