package com.mercatis.binpacking.visu.javafx;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class SimpleMouseControl {
	//Tracks drag starting point for x and y
	private double anchorX, anchorY;
	//Keep track of current angle for x and y
	private double anchorAngleX = 0;
	private double anchorAngleY = 0;
	//We will update these after drag. Using JavaFX property to bind with object
	private final DoubleProperty angleX = new SimpleDoubleProperty(0);
	private final DoubleProperty angleY = new SimpleDoubleProperty(0);


	public SimpleMouseControl(Group pGroup) {
		initMouseControl(pGroup);
	}

	private void initMouseControl(Group pGroup) {
		final Rotate tXRotate = new Rotate(0, Rotate.X_AXIS);
		final Rotate tYRotate = new Rotate(0, Rotate.Y_AXIS);
		pGroup.getTransforms().addAll(tXRotate, tYRotate);
		tXRotate.angleProperty().bind(angleX);
		tYRotate.angleProperty().bind(angleY);

		pGroup.setOnMousePressed(event -> {
			anchorX = event.getSceneX();
			anchorY = event.getSceneY();
			anchorAngleX = angleX.get();
			anchorAngleY = angleY.get();
			event.consume();
		});


		pGroup.setOnMouseDragged(event -> {
			angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
			angleY.set(anchorAngleY + anchorX - event.getSceneX());
			event.consume();
		});

	}

}
