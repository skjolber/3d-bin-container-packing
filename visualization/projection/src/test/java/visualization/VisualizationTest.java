package visualization;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.Box;
import com.github.skjolber.packing.BoxItem;
import com.github.skjolber.packing.Container;
import com.github.skjolber.packing.LargestAreaFitFirstPackager;
import com.github.skjolber.packing.visualization.ContainerProjection;

public class VisualizationTest {

	@Test
	public void testPackager() throws Exception {
		
		// issue 159
		List<Container> containers = new ArrayList<>();
		Container container = new Container("X", 4, 4, 4, 1000);
		containers.add(container);

		List<BoxItem> products = new ArrayList<>();
		products.add(new BoxItem(new Box("1", 4, 1, 1, 1)));
		products.add(new BoxItem(new Box("2", 4, 1, 1, 1)));
		products.add(new BoxItem(new Box("3", 4, 1, 1, 1)));
		products.add(new BoxItem(new Box("4", 4, 3, 1, 1)));

		LargestAreaFitFirstPackager packager = new LargestAreaFitFirstPackager(containers, true, true, true, 1);
		Container pack = packager.pack(products, Long.MAX_VALUE);		
		
		ContainerProjection p = new ContainerProjection();
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		List<Container> a = Arrays.asList(pack);

		System.out.println(a);
		
		File file = new File("../viewer/public/assets/containers.json");
		p.project(a , file);

		
	}
}
