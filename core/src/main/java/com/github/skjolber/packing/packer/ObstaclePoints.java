package com.github.skjolber.packing.packer;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.point.Point;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

/**
 * The free space of a container with obstacles (boxes which are already in the container).
 */
public class ObstaclePoints {

	private ObstaclePoints() {
	}

	/**
	 * @param container the container
	 * @param obstacles boxes which are already in the container, in its load coordinates
	 * @return the free points around the obstacles, for use as a container item's initial points
	 */
	public static List<Point> getFreePoints(Container container, List<Placement> obstacles) {
		DefaultPointCalculator3D ep = new DefaultPointCalculator3D(false, obstacles.size() + 1);
		ep.clearToSize(container.getLoadDx(), container.getLoadDy(), container.getLoadDz());
		for(int i = 0; i < obstacles.size(); i++) {
			if(!ep.addObstacle(obstacles.get(i))) {
				throw new IllegalStateException("Unable to add obstacle #" + i + " " + obstacles.get(i));
			}
		}
		return ep.getAll();
	}
}
