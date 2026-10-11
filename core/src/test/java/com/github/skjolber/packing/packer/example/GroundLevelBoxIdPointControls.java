package com.github.skjolber.packing.packer.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.packager.control.point.AbstractPointControlsBuilder;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControlsBuilderFactory;
import com.github.skjolber.packing.api.point.DefaultPointSource;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.api.point.PointSource;

/**
 * Worked example of writing point controls: keeps the boxes with the given ids on the floor of the container (no box below them), by
 * giving them only the points at the floor. The rule is about which box it is, not what the box looks like, so it needs custom code:
 * the packagers have no such rule.
 * <p>
 * <a href="../../../../../../../../../../DEVELOPER.md#writing-your-own-placement-controls">Writing your own placement controls</a> in DEVELOPER.md
 * teaches the interfaces; this class is the smallest complete use of them (it is used by {@linkplain PlacementRulesExampleTest}):
 * <ul>
 * <li>The controls extend {@linkplain DefaultPointControls}, which gives every box all the points, and override
 * {@linkplain #getPoints(BoxItem)} to give some boxes fewer.
 * <li>The builder extends {@linkplain AbstractPointControlsBuilder}, which receives the points (and more) from the packager, and
 * creates the controls in {@linkplain AbstractPointControlsBuilder#build()}.
 * <li>The factory is set on the container item (see {@code withPointControlsBuilderFactory(..)} on the container item builder of the
 * packager result builder). It creates one builder per packing attempt. The packagers may use it from several threads, and reuse
 * a result for another container item whose factory has the same {@linkplain PointControlsBuilderFactory#getId() id}, so the factory
 * is immutable, and its id says which boxes it concerns.
 * </ul>
 * The point controls are used by the plain and LAFF packagers; the brute force packagers do not support them.
 */
public class GroundLevelBoxIdPointControls extends DefaultPointControls {

	/**
	 * @param boxIds ids of the boxes which must be placed on the floor
	 * @return a factory for point controls which keep the boxes with the given ids on the floor
	 */
	public static PointControlsBuilderFactory newFactory(String... boxIds) {
		if(boxIds.length == 0) {
			throw new IllegalArgumentException("Expected one or more box ids");
		}
		Set<String> ids = new TreeSet<>();
		for(String boxId : boxIds) {
			if(boxId == null) {
				throw new IllegalArgumentException("Expected box ids, not null");
			}
			ids.add(boxId);
		}
		Set<String> immutableIds = Set.copyOf(ids);
		// the ids are part of the factory's id: factories for the same boxes build the same controls
		return PointControlsBuilderFactory.of("groundLevelBoxId:" + ids, () -> new Builder(immutableIds));
	}

	private static class Builder extends AbstractPointControlsBuilder<Builder> {

		private final Set<String> boxIds;

		private Builder(Set<String> boxIds) {
			this.boxIds = boxIds;
		}

		@Override
		public PointControls build() {
			return new GroundLevelBoxIdPointControls(points, boxIds);
		}
	}

	private final Set<String> boxIds;

	private GroundLevelBoxIdPointControls(PointSource points, Set<String> boxIds) {
		super(points);
		this.boxIds = boxIds;
	}

	@Override
	public PointSource getPoints(BoxItem boxItem) {
		String id = boxItem.getBox().getId();
		if(id == null || !boxIds.contains(id)) {
			return filteredPoints;
		}
		// a source of its own with the points at the floor; the packager keeps maintaining the source it handed to the builder
		List<Point> floor = new ArrayList<>();
		for(int i = 0; i < filteredPoints.size(); i++) {
			Point point = filteredPoints.get(i);
			if(point.getMinZ() == 0) {
				floor.add(point);
			}
		}
		return new DefaultPointSource(floor);
	}
}
