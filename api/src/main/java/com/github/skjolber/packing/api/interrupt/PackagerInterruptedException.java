package com.github.skjolber.packing.api.interrupt;

/**
 * Unwinds an interrupted packaging operation, i.e. when the deadline has passed or the interrupt was requested.
 * <p>
 * It is thrown as control flow from deep within the packagers, possibly many times per packaging, so it carries no
 * stack trace (and no suppressed exceptions).
 */

public class PackagerInterruptedException extends Exception {

	private static final long serialVersionUID = 1L;

	public PackagerInterruptedException() {
		super(null, null, false, false);
	}

}
