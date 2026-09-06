package com.github.skjolber.packing.packer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPoint3D;

class ControlledContainerItemTest {

	@Test
	void preservesControlsAndInitialPointsWhenCopied() {
		ControlledContainerItem source = new ControlledContainerItem(Container.newBuilder()
				.withSize(10, 10, 10)
				.withMaxLoadWeight(100)
				.build(), 2);
		ManifestControlsBuilderFactory manifestControls = () -> null;
		PointControlsBuilderFactory pointControls = () -> null;
		List<Point> initialPoints = List.of(new DefaultPoint3D(0, 0, 0, 9, 9, 9));
		source.setBoxItemControlsBuilderFactory(manifestControls);
		source.setPointControlsBuilderFactory(pointControls);
		source.setInitialPoints(initialPoints);

		ControlledContainerItem result = new ControlledContainerItem(source);

		assertThat(result.getBoxItemControlsBuilderFactory()).isSameAs(manifestControls);
		assertThat(result.getPointControlsBuilderFactory()).isSameAs(pointControls);
		assertThat(result.getInitialPoints()).isSameAs(initialPoints);
		assertThat(result.hasControls()).isTrue();
	}

	@Test
	void pointControlsAloneCountsAsControls() {
		ControlledContainerItem item = new ControlledContainerItem(Container.newBuilder()
				.withSize(1, 1, 1)
				.withMaxLoadWeight(1)
				.build(), 1);
		item.setPointControlsBuilderFactory(() -> null);

		assertThat(item.hasControls()).isTrue();
	}
}
