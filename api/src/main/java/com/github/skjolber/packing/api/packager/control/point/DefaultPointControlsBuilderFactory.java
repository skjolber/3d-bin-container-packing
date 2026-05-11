package com.github.skjolber.packing.api.packager.control.point;

public class DefaultPointControlsBuilderFactory implements PointControlsBuilderFactory {

	@Override
	public PointControlsBuilder createPointControlsBuilder() {
		return new DefaultPointControlsBuilder();
	}

}
