package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;
import java.util.logging.Logger;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxStackValue;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.ep.Point;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ContainerVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PointVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackPlacementVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackVisualizer;

public class DefaultPackagingResultVisualizerFactory extends AbstractPackagingResultVisualizerFactory<Container> {

	private static final Logger LOGGER = Logger.getLogger(DefaultPackagingResultVisualizerFactory.class.getName());

	protected final boolean calculatePoints;
	
	public DefaultPackagingResultVisualizerFactory(boolean calculatePoints) {
		this.calculatePoints = calculatePoints;
	}
	
	public PackagingResultVisualizer visualize(List<Container> inputContainers) {
		
		boolean calculatePoints = this.calculatePoints;
		
		int step = 0;
		PackagingResultVisualizer visualization = new PackagingResultVisualizer();
		for (Container inputContainer : inputContainers) {
			ContainerVisualizer containerVisualization = new ContainerVisualizer();
			containerVisualization.setStep(step++);

			containerVisualization.setDx(inputContainer.getDx());
			containerVisualization.setDy(inputContainer.getDy());
			containerVisualization.setDz(inputContainer.getDz());

			containerVisualization.setLoadDx(inputContainer.getLoadDx());
			containerVisualization.setLoadDy(inputContainer.getLoadDy());
			containerVisualization.setLoadDz(inputContainer.getLoadDz());

			containerVisualization.setId(inputContainer.getId());
			containerVisualization.setName(inputContainer.getDescription());

			StackVisualizer stackVisualization = new StackVisualizer();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);

			Stack stack = inputContainer.getStack();

			ExtremePoints3D extremePoints = new ExtremePoints3D(inputContainer.getDx(), inputContainer.getDy(), inputContainer.getDz(), true);

			for (StackPlacement placement : stack.getPlacements()) {
				Box box = placement.getStackable();
				BoxVisualizer boxVisualization = new BoxVisualizer();
				boxVisualization.setId(box.getId());
				boxVisualization.setName(box.getDescription());
				boxVisualization.setStep(step);

				BoxStackValue stackValue = placement.getStackValue();

				boxVisualization.setDx(stackValue.getDx());
				boxVisualization.setDy(stackValue.getDy());
				boxVisualization.setDz(stackValue.getDz());

				StackPlacementVisualizer stackPlacement = new StackPlacementVisualizer();
				stackPlacement.setX(placement.getAbsoluteX());
				stackPlacement.setY(placement.getAbsoluteY());
				stackPlacement.setZ(placement.getAbsoluteZ());
				stackPlacement.setStackable(boxVisualization);
				stackPlacement.setStep(step);

				if(calculatePoints) {
					int pointIndex = extremePoints.findPoint(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ());
	
					if(pointIndex == -1) {
						LOGGER.info("Unable to find next point, disabling further calculation of points");
						
						calculatePoints = false;
					} else {
						extremePoints.add(pointIndex, placement);
		
						for (Point point : extremePoints.getValues()) {
							PointVisualizer p = new PointVisualizer();
		
							p.setX(point.getMinX());
							p.setY(point.getMinY());
							p.setZ(point.getMinZ());
		
							p.setDx(point.getMaxX() - point.getMinX() + 1);
							p.setDy(point.getMaxY() - point.getMinY() + 1);
							p.setDz(point.getMaxZ() - point.getMinZ() + 1);
		
							stackPlacement.add(p);
						}
					}
				}
				
				stackVisualization.add(stackPlacement);

				step++;
			}

			visualization.add(containerVisualization);
		}
		return visualization;
	}
}
