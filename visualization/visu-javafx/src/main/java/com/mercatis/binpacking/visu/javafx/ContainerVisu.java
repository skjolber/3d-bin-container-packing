package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Level;
import com.github.skjolber.packing.Placement;
import javafx.geometry.Bounds;
import javafx.scene.AmbientLight;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

public class ContainerVisu extends Scene {

	private final Group containerGroup = new Group();
	private final PerspectiveCamera perspectiveCamera;
	private final SimpleCameraControl simpleCameraControl;

	public ContainerVisu() {
		super(new Group(), 500, 500, true, SceneAntialiasing.BALANCED);

		setRoot(containerGroup);
		setFill(Color.WHITE);

		perspectiveCamera = new PerspectiveCamera(false);
		perspectiveCamera.setNearClip(0.01);
		setCamera(perspectiveCamera);

		containerGroup.getChildren().add(new AmbientLight(Color.WHITE));

		simpleCameraControl = new SimpleCameraControl(this, perspectiveCamera);
	}

	public void removeAll() {
		List<Node> tChildren = new ArrayList<>(containerGroup.getChildren());
		containerGroup.getChildren().removeAll(tChildren);
	}

	public ContainerNode show(Container pContainer) {
		ContainerNode tContainerNode = new ContainerNode(pContainer);
		for (Level tLevel : pContainer.getLevels()) {
			for (Placement tPlacement : tLevel) {
				tContainerNode.addPlacement(tPlacement);
			}
		}

		containerGroup.getChildren().add(tContainerNode);

		return tContainerNode;
	}

}