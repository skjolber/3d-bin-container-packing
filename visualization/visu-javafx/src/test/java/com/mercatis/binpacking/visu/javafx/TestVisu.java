package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.PackCallback;
import com.github.skjolber.packing.Placement;
import com.github.skjolber.packing.Space;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created: 07.01.22   by: Armin Haaf
 *
 * @author Armin Haaf
 */
public class TestVisu extends Application {


	public void start(Stage pStage) {

		final Container tCarton = new Container("1", 200, 250, 100, 0);
		LargestAreaFitFirstPackager tPackager = new LargestAreaFitFirstPackager(Collections.singletonList(tCarton));


		final Random random = new Random();
		final double tMinSizeFactor = 0.1;
		final double tMaxSizeFactor = 0.7;
		List<Box> tItems = new ArrayList<>();
		for (int i = 0; i < 100; i++) {
			tItems.add(new com.github.skjolber.packing.Box("" + i,
														   (int)(tCarton.getWidth() * tMinSizeFactor + tCarton.getWidth() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   (int)(tCarton.getDepth() * tMinSizeFactor + tCarton.getDepth() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   (int)(tCarton.getHeight() * tMinSizeFactor + tCarton.getHeight() * random.nextDouble() * (tMaxSizeFactor - tMinSizeFactor)),
														   0));
		}


		final ContainerVisu tContainerVisu = new ContainerVisu();
		final ContainerNode tContainerNode = tContainerVisu.show(tCarton);

		tPackager.setPackCallback(new PackCallback() {
			@Override
			public void freeSpacesCalculated(final List<Space> pFreeSpaces) {
				Platform.runLater(() -> {
					tContainerNode.removeTransientBoxes();
					for (Space tFreeSpace : pFreeSpaces) {
						if (tFreeSpace != null) {
							tContainerNode.addFreeSpace(tFreeSpace);
						}
					}
				});

				waitForNextStep();
			}

			@Override
			public void placementAdded(final Placement pPlacement) {

				Platform.runLater(() -> {
					tContainerNode.removeTransientBoxes();
					tContainerNode.addPlacement(pPlacement);
				});

				waitForNextStep();
			}
		});


//
//		for (Container tContainer : tPackager.packList(tItems.stream().map(BoxItem::new).collect(Collectors.toList()), Integer.MAX_VALUE, Long.MAX_VALUE)) {
//			tContainerVisu.show(tContainer);
//		}

		tContainerVisu.addEventHandler(KeyEvent.KEY_TYPED, event -> {
			if (" ".equals(event.getCharacter())) {
				synchronized (tWaitObject) {
					tWaitObject.notify();
				}
			}
		});


		pStage.setScene(tContainerVisu);
		pStage.show();

		new Thread(() -> tPackager.pack(tItems, tCarton, Long.MAX_VALUE, 1)).start();

	}

	Object tWaitObject = new Object();

	private void waitForNextStep() {
		try {
			synchronized (tWaitObject) {
				tWaitObject.wait();
			}
		} catch (InterruptedException pE) {
		}
//        try {
//            Thread.sleep(50);
//        } catch (InterruptedException pE) {
//            pE.printStackTrace();
//        }
	}


	public static void main(String[] args) {
		launch();
	}
}
