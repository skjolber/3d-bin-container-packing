module com.github.skjolber.packing.ep {
	requires com.github.skjolber.packing.api;
	requires transitive org.eclipse.collections.api;
	requires org.eclipse.collections.impl;

	exports com.github.skjolber.packing.ep;
	exports com.github.skjolber.packing.ep.points1d;
	exports com.github.skjolber.packing.ep.points2d;
	exports com.github.skjolber.packing.ep.points3d;
}
