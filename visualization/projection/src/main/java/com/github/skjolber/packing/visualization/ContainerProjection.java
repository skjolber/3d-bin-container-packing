package com.github.skjolber.packing.visualization;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;

public class ContainerProjection extends AbstractProjection<Container> {

	public PackagingVisualization project(List<Container> inputContainers) {
		int step = 0;
		PackagingVisualization visualization = new PackagingVisualization();
		for(Container inputContainer : inputContainers) {
			ContainerVisualization containerVisualization = new ContainerVisualization();
			containerVisualization.setStep(step++);
			
			containerVisualization.setDx(inputContainer.getWidth());
			containerVisualization.setDy(inputContainer.getDepth());
			containerVisualization.setDz(inputContainer.getHeight());

			containerVisualization.setLoadDx(inputContainer.getWidth());
			containerVisualization.setLoadDy(inputContainer.getDepth());
			containerVisualization.setLoadDz(inputContainer.getHeight());

			containerVisualization.setId(inputContainer.getName());
			containerVisualization.setName(inputContainer.getName());

			StackVisualization stackVisualization = new StackVisualization();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);
			
			for(Level level : inputContainer.getLevels()) {
				for (Placement placement : level) {
					
					Box box = placement.getBox();
					BoxVisualization boxVisualization = new BoxVisualization();
					boxVisualization.setId(box.getName());
					boxVisualization.setName(box.getName());
					boxVisualization.setStep(step);

					boxVisualization.setDx(box.getWidth());
					boxVisualization.setDy(box.getDepth());
					boxVisualization.setDz(box.getHeight());
					
					StackPlacementVisualization stackPlacement = new StackPlacementVisualization();
					stackPlacement.setX(placement.getAbsoluteX());
					stackPlacement.setY(placement.getAbsoluteY());
					stackPlacement.setZ(placement.getAbsoluteZ());
					stackPlacement.setStackable(boxVisualization);
					stackPlacement.setStep(step);

					stackVisualization.add(stackPlacement);
					
					step++;
				}
			}
			
			visualization.add(containerVisualization);
		}
		return visualization;
	}
}
