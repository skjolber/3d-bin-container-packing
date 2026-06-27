package com.github.skjolber.packing.packer;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Comparator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.Order;
import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.Stack;
import com.github.skjolber.packing.api.packager.DefaultBoxItemSource;
import com.github.skjolber.packing.api.packager.control.point.DefaultPointControls;
import com.github.skjolber.packing.api.packager.control.point.PointControls;
import com.github.skjolber.packing.comparator.VolumeThenWeightBoxItemComparator;
import com.github.skjolber.packing.comparator.placement.PlacementComparator;
import com.github.skjolber.packing.comparator.placement.VolumeWeightAreaMinZPlacementComparator;
import com.github.skjolber.packing.ep.points3d.DefaultPointCalculator3D;

public class WeightLoadAwarePlacementControlsTest {

	private DefaultBoxItemSource boxItems;
	private DefaultPointCalculator3D pointCalculator;
	private PointControls pointControls;
	private Container container;
	private Stack stack;
	private Order order = Order.NONE;
	private PlacementComparator placementComparator;
	private Comparator<BoxItem> boxItemComparator;
	private boolean fullSupport = false;
	private WeightLoadAwarePlacementControls weightLoadAwarePlacementControls;
	
	@BeforeEach
	public void setup() {
		boxItems = new DefaultBoxItemSource();
		boxItems.setValues(new ArrayList<BoxItem>());
		
		pointCalculator = new DefaultPointCalculator3D(false, 10);
		pointCalculator.clearToSize(10, 10, 10);
		
		pointControls = new DefaultPointControls(pointCalculator);
		
		stack = new Stack();
		
		boxItemComparator = new VolumeThenWeightBoxItemComparator();
		placementComparator = new VolumeWeightAreaMinZPlacementComparator();
		
		container = Container.newBuilder().withSize(10, 10, 10).withMaxLoadWeight(1000).build();
		
		weightLoadAwarePlacementControls = new WeightLoadAwarePlacementControls(
			boxItems, pointControls, pointCalculator, container, stack,
			order, placementComparator, boxItemComparator, fullSupport
		);
		
		weightLoadAwarePlacementControls.initialize(10);
	}
	
	@Test
	public void testAboveWeightLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(5).build();
		
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		
		stack.add(wholeButtom);
		
		Box tooHeavy = Box.newBuilder().withSize(10, 10, 1).withWeight(6).build();
		
		boxItems.add(new BoxItem(tooHeavy));
		
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	@Test
	public void testBelowWeightLimit() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(5).build();
		
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		
		stack.add(wholeButtom);
		
		Box tooHeavy = Box.newBuilder().withSize(10, 10, 1).withWeight(1).build();
		
		boxItems.add(new BoxItem(tooHeavy));
		
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}
	
	@Test
	public void testSupporteeMaxWeightLimitReached() {
		Box bottom = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withMaxLoadWeight(200).withId("First").build();
		
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box overhang = Box.newBuilder().withSize(10, 10, 9).withWeight(200).withId("Second").build();
		Placement overhangPlacement = new Placement(overhang.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), overhangPlacement);
		stack.add(overhangPlacement);
		wholeButtom.addLoad(overhangPlacement, 4, 4);

		Box tooLowMaxLoadLimit = Box.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(1).withWeight(1).build();
		
		boxItems.add(new BoxItem(tooLowMaxLoadLimit));
		
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNull(placement);
	}

	@Test
	public void testSupporteeMaxWeightLimitNotReached() {
		Box bottom = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withMaxLoadWeight(200).withId("First").build();
		
		Placement buttomPlacement = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, buttomPlacement);
		stack.add(buttomPlacement);

		Box overhang = Box.newBuilder().withSize(10, 10, 9).withWeight(200).withId("Second").build();
		Placement overhangPlacement = new Placement(overhang.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), overhangPlacement);
		stack.add(overhangPlacement);
		buttomPlacement.addLoad(overhangPlacement, 4, overhang.getWeight());

		Box tooLowMaxLoadLimit = Box.newBuilder().withSize(1, 1, 1).withMaxLoadWeight(300).withWeight(1).build();
		
		boxItems.add(new BoxItem(tooLowMaxLoadLimit));
		
		// only available point is below a box which is heavy
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

	@Test
	public void testWeightLimitForChained() {
		Box bottom = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(2).withId("First").build();
		
		Placement wholeButtom = new Placement(bottom.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtom);
		stack.add(wholeButtom);

		Box wholeLevel2 = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withId("Second").build();
		Placement wholeLevel2Placement = new Placement(wholeLevel2.getStackValue(0), stack.size(), 0, 0, 1);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 1), wholeLevel2Placement);
		stack.add(wholeLevel2Placement);
		wholeButtom.addLoad(wholeLevel2Placement, 100, wholeLevel2.getWeight());

		Box tooHeavy = Box.newBuilder().withSize(1, 1, 1).withWeight(2).build();
		
		boxItems.add(new BoxItem(tooHeavy));
		
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNull(placement);
		
		Box notTooHeavy = Box.newBuilder().withSize(1, 1, 1).withWeight(1).build();
		
		boxItems.remove(0);
		boxItems.add(new BoxItem(notTooHeavy));

		placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}
	
	@Test
	public void testSupporteeMaxWeightLimitMustCalculateRelif() {
		Box bottomBox = Box.newBuilder().withSize(10, 10, 1).withWeight(1).withMaxLoadWeight(102).withId("First").build();
		
		Placement wholeButtomPlacement = new Placement(bottomBox.getStackValue(0), 0, 0, 0, 0);
		pointCalculator.add(0, wholeButtomPlacement);
		wholeButtomPlacement.setIndex(stack.size());
		stack.add(wholeButtomPlacement);

		Box cornerBox = Box.newBuilder().withSize(2, 2, 1).withWeight(1).withId("First").build();
		Placement cornerPlacement = new Placement(cornerBox.getStackValue(0), 0, 0, 0, 1);
		pointCalculator.add(0, cornerPlacement);
		cornerPlacement.setIndex(stack.size());
		stack.add(cornerPlacement);
		wholeButtomPlacement.addLoad(cornerPlacement, 4, cornerBox.getWeight());
		
		Box overhangBox = Box.newBuilder().withSize(10, 10, 8).withWeight(100).withId("Second").build();
		Placement overhangPlacement = new Placement(overhangBox.getStackValue(0), stack.size(), 0, 0, 2);
		pointCalculator.add(pointCalculator.findPoint(0, 0, 2), overhangPlacement);
		overhangPlacement.setIndex(stack.size());
		stack.add(overhangPlacement);
		cornerPlacement.addLoad(overhangPlacement, 4, overhangBox.getWeight());

		Box tooLowMaxLoadLimit = Box.newBuilder().withSize(1, 1, 1).withWeight(1).withMaxLoadWeight(1000).build();
		
		boxItems.add(new BoxItem(tooLowMaxLoadLimit));
		
		// only available point is below a box which is heavy
		Placement placement = weightLoadAwarePlacementControls.getPlacement(0, boxItems.size());
		assertNotNull(placement);
	}

}
