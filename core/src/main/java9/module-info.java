module com.github.skjolber.packing.core {
	requires com.github.skjolber.packing.api;
	requires com.github.skjolber.packing.ep;
	requires org.eclipse.collections.api;
	requires org.eclipse.collections.impl;

	exports com.github.skjolber.packing.comparator;
	exports com.github.skjolber.packing.comparator.placement;
	exports com.github.skjolber.packing.cost;
	exports com.github.skjolber.packing.iterator;
	exports com.github.skjolber.packing.virtualbox;

	exports com.github.skjolber.packing.packer;
	exports com.github.skjolber.packing.packer.bruteforce;
	exports com.github.skjolber.packing.packer.composite;
	exports com.github.skjolber.packing.packer.laff;
	exports com.github.skjolber.packing.packer.plain;
	exports com.github.skjolber.packing.packer.strategy;
	exports com.github.skjolber.packing.packer.strategy.allocation;
	exports com.github.skjolber.packing.packer.strategy.bruteforce;
	exports com.github.skjolber.packing.packer.strategy.cost;
	exports com.github.skjolber.packing.packer.strategy.ordered;
	exports com.github.skjolber.packing.packer.util;
}
