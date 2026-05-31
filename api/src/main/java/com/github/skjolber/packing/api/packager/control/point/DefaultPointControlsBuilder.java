package com.github.skjolber.packing.api.packager.control.point;

public class DefaultPointControlsBuilder extends AbstractPointControlsBuilder<DefaultPointControlsBuilder> {

	@Override
	public PointControls build() {
		if(isMaxLoad()) {
			// TODO
		}
		return new DefaultPointControls(points);
	}



}
