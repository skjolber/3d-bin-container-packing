module com.github.skjolber.packing.core {
	requires com.github.skjolber.packing.api;
	requires com.github.skjolber.packing.ep;
	
	exports com.github.skjolber.packing.deadline;
	exports com.github.skjolber.packing.iterator;
	
	exports com.github.skjolber.packing.packager;
	exports com.github.skjolber.packing.packager.bruteforce;
	exports com.github.skjolber.packing.packager.laff;
	exports com.github.skjolber.packing.packager.plain;
}