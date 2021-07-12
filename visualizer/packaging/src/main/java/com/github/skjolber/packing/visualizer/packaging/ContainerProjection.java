package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ContainerVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackPlacementVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackVisualizer;

public class ContainerProjection extends AbstractProjection<Container> {

	public PackagingResultVisualizer project(List<Container> inputContainers) {
		int step = 0;
		PackagingResultVisualizer visualization = new PackagingResultVisualizer();
		for(Container inputContainer : inputContainers) {
			ContainerVisualizer containerVisualization = new ContainerVisualizer();
			containerVisualization.setStep(step++);
			
			containerVisualization.setDx(inputContainer.getWidth());
			containerVisualization.setDy(inputContainer.getDepth());
			containerVisualization.setDz(inputContainer.getHeight());

			containerVisualization.setLoadDx(inputContainer.getWidth());
			containerVisualization.setLoadDy(inputContainer.getDepth());
			containerVisualization.setLoadDz(inputContainer.getHeight());

			containerVisualization.setId(inputContainer.getName());
			containerVisualization.setName(inputContainer.getName());

			StackVisualizer stackVisualization = new StackVisualizer();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);
			
			for(Level level : inputContainer.getLevels()) {
				for (Placement placement : level) {
					
					Box box = placement.getBox();
					BoxVisualizer boxVisualization = new BoxVisualizer();
					boxVisualization.setId(box.getName());
					boxVisualization.setName(box.getName());
					boxVisualization.setStep(step);

					boxVisualization.setDx(box.getWidth());
					boxVisualization.setDy(box.getDepth());
					boxVisualization.setDz(box.getHeight());
					
					StackPlacementVisualizer stackPlacement = new StackPlacementVisualizer();
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
