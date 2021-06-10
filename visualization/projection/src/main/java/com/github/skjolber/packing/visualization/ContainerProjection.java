package com.github.skjolber.packing.visualization;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;

public class ContainerProjection extends AbstractProjection<Container> {

	public PackagingVisualization project(List<Container> inputContainers) {
		PackagingVisualization visualization = new PackagingVisualization();
		for(Container inputContainer : inputContainers) {
			ContainerVisualization containerVisualization = new ContainerVisualization();
			for(Level level : inputContainer.getLevels()) {
				for (Placement placement : level) {
					BoxVisualization boxVisualization = new BoxVisualization();
					
					boxVisualization.setX(placement.getAbsoluteX());
					boxVisualization.setY(placement.getAbsoluteY());
					boxVisualization.setZ(placement.getAbsoluteZ());

					Box box = placement.getBox();
					boxVisualization.setDx(box.getWidth());
					boxVisualization.setDy(box.getDepth());
					boxVisualization.setDz(box.getHeight());

					boxVisualization.setName(box.getName());
					
					containerVisualization.add(boxVisualization);
				}
			}
			
			visualization.add(containerVisualization);
		}
		return visualization;
	}
}
