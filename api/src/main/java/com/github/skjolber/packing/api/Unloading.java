package com.github.skjolber.packing.api;

/**
 * How the boxes of a container are unloaded. This decides what a box placed under boxes which are already there
 * (for example into a gap under an overhang) may be relied on for: its top touches their bottom, so it carries part
 * of their weight (and its own limits are checked) either way.
 */
public enum Unloading {

	/**
	 * Boxes may be unloaded in any order, so a box placed under boxes which are already there may be removed while
	 * they stay: it does not relieve the boxes which already support them, and does not count as their support.
	 * This also holds if the box does not quite touch them (for example a slightly lower or compressible box).
	 */
	ANY_ORDER,

	/**
	 * Boxes are unloaded in the reverse order of loading, so a box placed under boxes which are already there stays
	 * until they are unloaded: all touching boxes share the load by contact area, and it relieves the boxes which
	 * already support them. This packs denser, but assumes rigid boxes in full contact.
	 */
	REVERSE_LOADING_ORDER
}
