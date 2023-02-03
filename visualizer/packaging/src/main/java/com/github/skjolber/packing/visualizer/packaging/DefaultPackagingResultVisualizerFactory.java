package com.github.skjolber.packing.visualizer.packaging;

import java.util.List;

import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerStackValue;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.StackPlacement;
import com.github.skjolber.packing.api.StackValue;
import com.github.skjolber.packing.api.Stackable;
import com.github.skjolber.packing.api.ep.Point3D;
import com.github.skjolber.packing.ep.points3d.ExtremePoints3D;
import com.github.skjolber.packing.visualizer.api.packaging.BoxVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.ContainerVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PackagingResultVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.PointVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackPlacementVisualizer;
import com.github.skjolber.packing.visualizer.api.packaging.StackVisualizer;

public class DefaultPackagingResultVisualizerFactory extends AbstractPackagingResultVisualizerFactory<Container> {

	public PackagingResultVisualizer visualize(List<Container> inputContainers) {
		int step = 0;
		PackagingResultVisualizer visualization = new PackagingResultVisualizer();
		for (Container inputContainer : inputContainers) {
			ContainerVisualizer containerVisualization = new ContainerVisualizer();
			containerVisualization.setStep(step++);

			ContainerStackValue[] stackValues = inputContainer.getStackValues();

			ContainerStackValue containerStackValue = stackValues[0];

			containerVisualization.setDx(containerStackValue.getDx());
			containerVisualization.setDy(containerStackValue.getDy());
			containerVisualization.setDz(containerStackValue.getDz());

			containerVisualization.setLoadDx(containerStackValue.getLoadDx());
			containerVisualization.setLoadDy(containerStackValue.getLoadDy());
			containerVisualization.setLoadDz(containerStackValue.getLoadDz());

			containerVisualization.setId(inputContainer.getId());
			containerVisualization.setName(inputContainer.getDescription());

			StackVisualizer stackVisualization = new StackVisualizer();
			stackVisualization.setStep(step++);
			containerVisualization.setStack(stackVisualization);

			Stack stack = inputContainer.getStack();

			ExtremePoints3D<StackPlacement> extremePoints = new ExtremePoints3D<>(containerStackValue.getDx(), containerStackValue.getDy(), containerStackValue.getDz());

			for (StackPlacement placement : stack.getPlacements()) {
				Stackable box = placement.getStackable();
				BoxVisualizer boxVisualization = new BoxVisualizer();
				boxVisualization.setId(box.getId());
				boxVisualization.setName(box.getDescription());
				boxVisualization.setStep(step);

				StackValue stackValue = placement.getStackValue();

				boxVisualization.setDx(stackValue.getDx());
				boxVisualization.setDy(stackValue.getDy());
				boxVisualization.setDz(stackValue.getDz());

				StackPlacementVisualizer stackPlacement = new StackPlacementVisualizer();
				stackPlacement.setX(placement.getAbsoluteX());
				stackPlacement.setY(placement.getAbsoluteY());
				stackPlacement.setZ(placement.getAbsoluteZ());
				stackPlacement.setStackable(boxVisualization);
				stackPlacement.setStep(step);

				int pointIndex = extremePoints.findPoint(placement.getAbsoluteX(), placement.getAbsoluteY(), placement.getAbsoluteZ());

				extremePoints.add(pointIndex, placement);

				for (Point3D<StackPlacement> point : extremePoints.getValues()) {
					PointVisualizer p = new PointVisualizer();

					p.setX(point.getMinX());
					p.setY(point.getMinY());
					p.setZ(point.getMinZ());

					p.setDx(point.getMaxX() - point.getMinX() + 1);
					p.setDy(point.getMaxY() - point.getMinY() + 1);
					p.setDz(point.getMaxZ() - point.getMinZ() + 1);

					stackPlacement.add(p);
				}

				stackVisualization.add(stackPlacement);

				step++;
			}

			visualization.add(containerVisualization);
		}
		return visualization;
	}
}
