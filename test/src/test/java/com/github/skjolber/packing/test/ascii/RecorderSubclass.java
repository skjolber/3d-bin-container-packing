package com.github.skjolber.packing.test.ascii;

/**
 * As a test class which calls the helper method of its base class.
 */
class RecorderSubclass extends RecorderBaseClass {

	StackTraceElement callHelper() {
		return figure();
	}
}
