package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Dimension;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;
import javafx.geometry.Bounds;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.DrawMode;

import java.util.Random;

public class ContainerVisu extends Scene {

	private final Group containerGroup = new Group();
	private final Camera perspectiveCamera;
	private final SimpleCameraControl simpleCameraControl;

	public ContainerVisu() {
		super(new Group(), 500, 500, true);

		setRoot(containerGroup);
		setFill(new Color(1, 1, 0, 1));

		perspectiveCamera = new PerspectiveCamera(false);
		perspectiveCamera.setTranslateZ(-200);
		perspectiveCamera.setTranslateY(-150);
		setCamera(perspectiveCamera);

		simpleCameraControl = new SimpleCameraControl(this, perspectiveCamera);
	}

	public ContainerNode show(Container pContainer) {
		ContainerNode tContainerNode = new ContainerNode(pContainer);
		for (Level tLevel : pContainer.getLevels()) {
			for (Placement tPlacement : tLevel) {
				tContainerNode.addPlacement(tPlacement);
			}
		}

		containerGroup.getChildren().add(tContainerNode);
		
		// add to right
		final Bounds tRootBounds = containerGroup.getBoundsInParent();
		tContainerNode.setTranslateX(tRootBounds.getMaxX() + tContainerNode.getBoundsInParent().getWidth());

		// adapt camera to show everything
		final Bounds tAllBounds = containerGroup.getBoundsInParent();
		simpleCameraControl.moveTo(tAllBounds.getCenterX() - getWidth() / 2,
								   tAllBounds.getCenterY() - getHeight() / 2,
								   tAllBounds.getCenterZ() - tAllBounds.getDepth());


		return tContainerNode;
	}

}