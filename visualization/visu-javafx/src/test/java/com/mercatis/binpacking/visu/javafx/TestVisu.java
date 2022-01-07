package com.mercatis.binpacking.visu.javafx;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.impl.LAFFResult;
import javafx.application.Application;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
		for (Container tContainer : tPackager.packList(tItems.stream().map(BoxItem::new).collect(Collectors.toList()), Integer.MAX_VALUE, Long.MAX_VALUE)) {
			tContainerVisu.show(tContainer);
		}

		pStage.setScene(tContainerVisu);
		pStage.show();

	}

	public static void main(String[] args) {
		launch();
	}
}
