package com.mercatis.binpacking.visu.javafx;

import javafx.animation.AnimationTimer;
import javafx.scene.Camera;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.ScrollEvent;

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


	public SimpleCameraControl(final Scene pScene, final Camera pCamera) {

		scene = pScene;
		camera = pCamera;

		addKeyControl();

		// immer in der Mitte halten...
		pScene.widthProperty().addListener((observable, oldValue, newValue) -> {
			pCamera.translateXProperty().set(-newValue.doubleValue() / 2.0);
		});
		pScene.heightProperty().addListener((observable, oldValue, newValue) -> {
			pCamera.translateYProperty().set(-newValue.doubleValue() / 2.0);
		});

		pCamera.translateXProperty().set(-pScene.getWidth() / 2.0);
		pCamera.translateYProperty().set(-pScene.getHeight() / 2.0);

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
				if (currentPressedKeys.contains(KeyCode.LEFT)) {
					camera.translateXProperty().set(camera.getTranslateX() - stepFactor);
				}
				if (currentPressedKeys.contains(KeyCode.RIGHT)) {
					camera.translateXProperty().set(camera.getTranslateX() + stepFactor);
				}
				if (currentPressedKeys.contains(KeyCode.UP)) {
					camera.translateYProperty().set(camera.getTranslateY() - stepFactor);
				}
				if (currentPressedKeys.contains(KeyCode.DOWN)) {
					camera.translateYProperty().set(camera.getTranslateY() + stepFactor);
				}
				if (currentPressedKeys.contains(KeyCode.PLUS)) {
					camera.translateZProperty().set(camera.getTranslateZ() + stepFactor);
				}
				if (currentPressedKeys.contains(KeyCode.MINUS)) {
					camera.translateZProperty().set(camera.getTranslateZ() - stepFactor);
				}
			}
		};
		tAnimationTimer.start();
	}
}
