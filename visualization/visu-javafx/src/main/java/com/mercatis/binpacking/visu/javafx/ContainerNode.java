package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.Dimension;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.Space;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.DrawMode;

import javax.swing.text.Position;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class ContainerNode extends Group {

	private final Random random = new Random();
	private final Container container;

	final List<Box> transientBoxes = new ArrayList<>();

	public ContainerNode(Container pContainer) {
		this.container = pContainer;

		final javafx.scene.shape.Box tContainerBox = createBox(pContainer);
		tContainerBox.setTranslateX(tContainerBox.getTranslateX() - pContainer.getWidth() / 2.0);
		tContainerBox.setTranslateY(tContainerBox.getTranslateY() + pContainer.getHeight() / 2.0);
		tContainerBox.setTranslateZ(tContainerBox.getTranslateZ() - pContainer.getDepth() / 2.0);
		tContainerBox.drawModeProperty().set(DrawMode.LINE);

		getChildren().add(tContainerBox);
		new SimpleMouseControl(this);
	}

	public void addPlacement(Placement tPlacement) {
		final javafx.scene.shape.Box tBox = createBox(tPlacement.getBox());
		final PhongMaterial tBoxMaterial = new PhongMaterial(new Color(random.nextDouble(),
																	   random.nextDouble(),
																	   random.nextDouble(), 1));
		tBox.setMaterial(tBoxMaterial);

		tBox.setTranslateX(tBox.getTranslateX() + tPlacement.getAbsoluteX() - container.getWidth() / 2.0);
		tBox.setTranslateY(tBox.getTranslateY() - tPlacement.getAbsoluteZ() + container.getHeight() / 2.0);
		tBox.setTranslateZ(tBox.getTranslateZ() + tPlacement.getAbsoluteY() - container.getDepth() / 2.0);
		getChildren().add(tBox);
	}

	public void addFreeSpace(Space pFreeSpace) {
		javafx.scene.shape.Box tFreeSpaceBox = createBox(pFreeSpace);
		PhongMaterial tInsideMaterial = new PhongMaterial(new Color(random.nextDouble(),
																	random.nextDouble(),
																	random.nextDouble(), 0.5));
		tFreeSpaceBox.setMaterial(tInsideMaterial);

		tFreeSpaceBox.setTranslateX(tFreeSpaceBox.getTranslateX() + pFreeSpace.getX() - container.getWidth() / 2.0);
		tFreeSpaceBox.setTranslateY(tFreeSpaceBox.getTranslateY() - pFreeSpace.getZ() + container.getHeight() / 2.0);
		tFreeSpaceBox.setTranslateZ(tFreeSpaceBox.getTranslateZ() + pFreeSpace.getY() - container.getDepth() / 2.0);
		getChildren().add(tFreeSpaceBox);
		transientBoxes.add(tFreeSpaceBox);

	}

	public void removeTransientBoxes() {
		getChildren().removeAll(transientBoxes);
		transientBoxes.clear();

	}

	private javafx.scene.shape.Box createBox(final Dimension pBox) {
		final javafx.scene.shape.Box tBox = new javafx.scene.shape.Box(pBox.getWidth(), pBox.getHeight(), pBox.getDepth());
		tBox.setTranslateX(tBox.getWidth() / 2);
		tBox.setTranslateY(-tBox.getHeight() / 2);
		tBox.setTranslateZ(tBox.getDepth() / 2);
		return tBox;
	}
}
