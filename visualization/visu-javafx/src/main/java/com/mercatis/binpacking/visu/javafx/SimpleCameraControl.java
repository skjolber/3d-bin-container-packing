package com.mercatis.binpacking.visu.javafx;

import javafx.animation.AnimationTimer;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.transform.Rotate;

import java.util.HashSet;
import java.util.Set;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class SimpleCameraControl {

	private final Scene scene;
	private final Camera camera;
	private final Set<KeyCode> currentPressedKeys = new HashSet<>();

	private double stepFactor = 1;

	//Tracks drag starting point for x and y
	private double anchorX, anchorY;
	//Keep track of current angle for x and y
	private double anchorAngleX = 0;
	private double anchorAngleY = 0;
	//We will update these after drag. Using JavaFX property to bind with object
	private final DoubleProperty angleX = new SimpleDoubleProperty(0);
	private final DoubleProperty angleY = new SimpleDoubleProperty(0);


	public SimpleCameraControl(final Scene pScene, final Camera pCamera) {

		scene = pScene;
		camera = pCamera;

		addKeyControl();
		addMouseControl();
	}

	private void addKeyControl() {
		scene.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
			currentPressedKeys.add(event.getCode());
		});

		scene.addEventHandler(KeyEvent.KEY_RELEASED, event -> {
			currentPressedKeys.remove(event.getCode());
		});

		//Attach a scroll listener
		scene.addEventHandler(ScrollEvent.SCROLL, event -> {
			//Get how much scroll was done in Y axis.
			final double delta = event.getDeltaY();
			//Add it to the Z-axis location.
			camera.translateZProperty().set(camera.getTranslateZ() + delta * stepFactor);
		});

		final AnimationTimer tAnimationTimer = new AnimationTimer() {
			@Override
			public void handle(final long now) {
				if ( currentPressedKeys.contains(KeyCode.LEFT)) {
					camera.translateXProperty().set(camera.getTranslateX() - stepFactor);
				}
				if ( currentPressedKeys.contains(KeyCode.RIGHT)) {
					camera.translateXProperty().set(camera.getTranslateX() + stepFactor);
				}
				if ( currentPressedKeys.contains(KeyCode.UP)) {
					camera.translateYProperty().set(camera.getTranslateY() - stepFactor);
				}
				if ( currentPressedKeys.contains(KeyCode.DOWN)) {
					camera.translateYProperty().set(camera.getTranslateY() + stepFactor);
				}
				if ( currentPressedKeys.contains(KeyCode.PLUS)) {
					camera.translateZProperty().set(camera.getTranslateZ() + stepFactor);
				}
				if ( currentPressedKeys.contains(KeyCode.MINUS)) {
					camera.translateZProperty().set(camera.getTranslateZ() - stepFactor);
				}
			}
		};
		tAnimationTimer.start();
	}

	private void addMouseControl() {
		final Rotate tXRotate = new Rotate(0, Rotate.X_AXIS);
		final Rotate tYRotate = new Rotate(0, Rotate.Y_AXIS);
		camera.getTransforms().addAll(tXRotate, tYRotate);
		tXRotate.angleProperty().bind(angleX);
		tYRotate.angleProperty().bind(angleY);

		scene.setOnMousePressed(event -> {
			anchorX = event.getSceneX();
			anchorY = event.getSceneY();
			anchorAngleX = angleX.get();
			anchorAngleY = angleY.get();
			event.consume();
		});


		scene.setOnMouseDragged(event -> {
			angleX.set(anchorAngleX - (anchorY - event.getSceneY()));
			angleY.set(anchorAngleY + anchorX - event.getSceneX());
			event.consume();
		});

	}
	
	public double getStepFactor() {
		return stepFactor;
	}

	public void setStepFactor(final double pStepFactor) {
		stepFactor = pStepFactor;
	}

	public void moveTo(double pX,double pY, double pZ) {
		camera.translateXProperty().set(pX);
		camera.translateYProperty().set(pY);
		camera.translateZProperty().set(pZ);
	}
}
