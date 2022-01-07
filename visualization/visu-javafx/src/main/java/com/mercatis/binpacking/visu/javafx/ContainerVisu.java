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
	private final Random random = new Random();
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

	public void show(Container pContainer) {
		final Group tContainerGroup = new Group();
		for (Level tLevel : pContainer.getLevels()) {
			for (Placement tPlacement : tLevel) {
				final javafx.scene.shape.Box tBox = createBox(tPlacement.getBox());
				final PhongMaterial tBoxMaterial = new PhongMaterial(new Color(random.nextDouble(),
																			   random.nextDouble(),
																			   random.nextDouble(), 1));
				tBox.setMaterial(tBoxMaterial);

				tBox.setTranslateX(tBox.getTranslateX() + tPlacement.getAbsoluteX() - pContainer.getWidth() / 2.0);
				tBox.setTranslateY(tBox.getTranslateY() - tPlacement.getAbsoluteZ() + pContainer.getHeight() / 2.0);
				tBox.setTranslateZ(tBox.getTranslateZ() + tPlacement.getAbsoluteY() - pContainer.getDepth() / 2.0);
				tContainerGroup.getChildren().add(tBox);
			}
		}

		final javafx.scene.shape.Box tContainerBox = createBox(pContainer);
		tContainerBox.setTranslateX(tContainerBox.getTranslateX() - pContainer.getWidth() / 2.0);
		tContainerBox.setTranslateY(tContainerBox.getTranslateY() + pContainer.getHeight() / 2.0);
		tContainerBox.setTranslateZ(tContainerBox.getTranslateZ() - pContainer.getDepth() / 2.0);
		tContainerBox.drawModeProperty().set(DrawMode.LINE);

		tContainerGroup.getChildren().add(tContainerBox);
		new SimpleMouseControl(tContainerGroup);

		containerGroup.getChildren().add(tContainerGroup);

		// add to right
		final Bounds tRootBounds = containerGroup.getBoundsInParent();
		tContainerGroup.setTranslateX(tRootBounds.getMaxX() + tContainerGroup.getBoundsInParent().getWidth());

		// adapt camera to show everything
		final Bounds tAllBounds = containerGroup.getBoundsInParent();
		simpleCameraControl.moveTo(tAllBounds.getCenterX() - getWidth() / 2,
								   tAllBounds.getCenterY() - getHeight() / 2,
								   tAllBounds.getCenterZ() - tAllBounds.getDepth());
	}

	private javafx.scene.shape.Box createBox(final Dimension pBox) {
		final javafx.scene.shape.Box tBox = new javafx.scene.shape.Box(pBox.getWidth(), pBox.getHeight(), pBox.getDepth());
		tBox.setTranslateX(tBox.getWidth() / 2);
		tBox.setTranslateY(-tBox.getHeight() / 2);
		tBox.setTranslateZ(tBox.getDepth() / 2);
		return tBox;
	}

}