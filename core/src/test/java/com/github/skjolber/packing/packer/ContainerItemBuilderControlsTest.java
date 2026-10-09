package com.github.skjolber.packing.packer;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.packager.control.manifest.ManifestControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControlsBuilderFactory;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.packer.AbstractPackagerResultBuilder.DefaultContainerItemBuilder;

/**
 * A container item which already carries controls, initial points or a cost calculator keeps them
 * when passed through the result builder's container item builder without overrides.
 */
public class ContainerItemBuilderControlsTest {

	private Container container = Container.newBuilder()
			.withSize(10, 10, 10)
			.withMaxLoadWeight(100)
			.build();

	@Test
	public void preconfiguredContainerItemKeepsItsControls() {
		ManifestControlsBuilderFactory manifestControlsBuilderFactory = () -> null;
		PointControlsBuilderFactory pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();
		List<Point> initialPoints = new ArrayList<>();

		ContainerItem containerItem = new ContainerItem(container, 1);
		containerItem.setManifestControlsBuilderFactory(manifestControlsBuilderFactory);
		containerItem.setPointControlsBuilderFactory(pointControlsBuilderFactory);
		containerItem.setInitialPoints(initialPoints);

		DefaultContainerItemBuilder builder = new DefaultContainerItemBuilder();
		builder.withContainerItem(containerItem);
		ContainerItem build = builder.build();

		assertSame(manifestControlsBuilderFactory, build.getManifestControlsBuilderFactory());
		assertSame(pointControlsBuilderFactory, build.getPointControlsBuilderFactory());
		assertSame(initialPoints, build.getInitialPoints());
	}

	@Test
	public void builderControlsOverrideTheContainerItems() {
		ContainerItem containerItem = new ContainerItem(container, 1);
		containerItem.setManifestControlsBuilderFactory(() -> null);
		containerItem.setPointControlsBuilderFactory(new DefaultPointControlsBuilderFactory());

		ManifestControlsBuilderFactory manifestControlsBuilderFactory = () -> null;
		PointControlsBuilderFactory pointControlsBuilderFactory = new DefaultPointControlsBuilderFactory();

		DefaultContainerItemBuilder builder = new DefaultContainerItemBuilder();
		builder.withContainerItem(containerItem)
				.withManifestControlsBuilderFactory(manifestControlsBuilderFactory)
				.withPointControlsBuilderFactory(pointControlsBuilderFactory);
		ContainerItem build = builder.build();

		assertSame(manifestControlsBuilderFactory, build.getManifestControlsBuilderFactory());
		assertSame(pointControlsBuilderFactory, build.getPointControlsBuilderFactory());
	}

	@Test
	public void plainContainerItemStaysWithoutControls() {
		DefaultContainerItemBuilder builder = new DefaultContainerItemBuilder();
		builder.withContainerItem(container, 1);
		ContainerItem build = builder.build();

		assertNull(build.getManifestControlsBuilderFactory());
		assertNull(build.getPointControlsBuilderFactory());
		assertNull(build.getInitialPoints());
	}
}
