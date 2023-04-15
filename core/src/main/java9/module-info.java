module com.github.skjolber.packing.core {
	requires com.github.skjolber.packing.api;
	requires com.github.skjolber.packing.ep;
	
	exports com.github.skjolber.packing.deadline;
	exports com.github.skjolber.packing.iterator;
	
	exports com.github.skjolber.packing.packer;
	exports com.github.skjolber.packing.packer.bruteforce;
	exports com.github.skjolber.packing.packer.laff;
	exports com.github.skjolber.packing.packer.plain;
}