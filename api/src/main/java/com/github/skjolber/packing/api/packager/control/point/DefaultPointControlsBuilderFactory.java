package com.github.skjolber.packing.api.packager.control.point;

public class DefaultPointControlsBuilderFactory implements PointControlsBuilderFactory {

	@Override
	public PointControlsBuilder createPointControlsBuilder() {
		return new DefaultPointControlsBuilder();
	}

	/**
	 * This factory is stateless, so all instances carry the same id: packagers can reuse the result of a container for another with
	 * this factory (see {@link PointControlsBuilderFactory#getId()}).
	 */
	@Override
	public String getId() {
		return "default";
	}

}
