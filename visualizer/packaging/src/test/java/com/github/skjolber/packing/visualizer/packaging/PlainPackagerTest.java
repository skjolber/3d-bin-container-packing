package com.github.skjolber.packing.visualizer.packaging;
import com.github.skjolber.packing.api.*;
import com.github.skjolber.packing.packer.laff.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.packer.plain.PlainPackager;
import com.github.skjolber.packing.visualizer.packaging.DefaultPackagingResultVisualizerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class PlainPackagerTest extends AbstractPackagerTest {

    private static List<BoxItem> products = Arrays.asList(
            new BoxItem(Box.newBuilder().withDescription("1").withSize(395, 260, 185).withWeight(7).build(), 41),
            new BoxItem(Box.newBuilder().withDescription("2").withSize(335, 175, 160).withWeight(6).build(), 2),
            new BoxItem(Box.newBuilder().withDescription("3").withSize(530, 255, 150).withWeight(5).build(), 1));

    private static Container container = Container.newBuilder().withDescription("1")
            .withEmptyWeight(0)
            .withSize(1200, 800, 2000)
            .withMaxLoadWeight(1000)
            .build();

    @Test
    public void testPlainPackager() throws Exception {
        Packager laff_packager = PlainPackager.newBuilder()
                .build();

        List<ContainerItem> containerItems = ContainerItem
                .newListBuilder()
                .withContainer(container)
                .build();

        PackagerResult result = laff_packager.newResultBuilder().withContainers(containerItems).withStackables(products).build();
        if (result.isSuccess()) {
            write(result.get(0));
        } else {
            System.out.println("Impossible to stack");
        }
        System.out.println("Finished");
    }

}