package com.github.skjolber.packing.jmh.shipping;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.skjolber.packing.api.Box;
import com.github.skjolber.packing.api.BoxItem;
import com.github.skjolber.packing.api.Container;
import com.github.skjolber.packing.api.ContainerItem;
import com.github.skjolber.packing.api.Rotation;

/**
 * Orders from a third-party logistics use case (GitHub issue #1158): items of an order are packed
 * into as few, and as small, shipping containers as possible, with a carrier weight limit per
 * container. Dimensions are in 1/10000 inch.
 * <p>
 * The order data is read from JSON (Jackson serializations of {@code List<ContainerItem>} and
 * {@code List<BoxItem>}) in {@code src/main/resources/shipping/}; add further orders there.
 */
public class ShippingOrders {

	private ShippingOrders() {
	}

	private static final String ISSUE_1158 = "/shipping/issue-1158/";

	private static Container container(String id, int dx, int dy, int dz, int emptyWeight, int maxLoadWeight) {
		return Container.newBuilder()
				.withId(id)
				.withDescription(id)
				.withSize(dx, dy, dz)
				.withEmptyWeight(emptyWeight)
				.withMaxLoadWeight(maxLoadWeight)
				.build();
	}

	/** Available shipping containers (sorted by volume, as smaller is cheaper to ship). */
	public static List<ContainerItem> getContainers() {
		return readContainers(ISSUE_1158 + "containerItems.json");
	}

	/**
	 * An order of 5 items. Fits in CURBY2SL only when the thick item and two flat items share the
	 * lower layer, with two flat items on top.
	 */
	public static List<BoxItem> getOrder() {
		return readBoxes(ISSUE_1158 + "boxItems.json");
	}

	/**
	 * Read container items as serialized by Jackson from {@code List<ContainerItem>} (wrapped in a
	 * {@code containerItems} field).
	 */
	public static List<ContainerItem> readContainers(String resource) {
		JsonNode root = read(resource);
		ContainerItem.Builder builder = ContainerItem.newListBuilder();
		for(JsonNode item : root.get("containerItems")) {
			JsonNode c = item.get("container");
			Container container = Container.newBuilder()
					.withId(c.get("id").asText())
					.withDescription(c.get("description").asText())
					.withSize(c.get("dx").asInt(), c.get("dy").asInt(), c.get("dz").asInt())
					.withEmptyWeight(c.get("emptyWeight").asInt())
					.withMaxLoadWeight(c.get("maxLoadWeight").asInt())
					.build();
			builder.withContainer(container, item.get("count").asInt());
		}
		return builder.build();
	}

	/**
	 * Read box items as serialized by Jackson from {@code List<BoxItem>} (wrapped in a {@code boxItems}
	 * field). The rotations are given by the surfaces of the serialized stack values.
	 */
	public static List<BoxItem> readBoxes(String resource) {
		JsonNode root = read(resource);
		List<BoxItem> items = new ArrayList<>();
		for(JsonNode item : root.get("boxItems")) {
			JsonNode b = item.get("box");
			JsonNode stackValues = b.get("stackValues");
			JsonNode first = stackValues.get(0);

			Rotation.Builder rotation = Rotation.newBuilder();
			Map<String, Integer> surfaces = new HashMap<>();
			for(JsonNode stackValue : stackValues) {
				for(JsonNode surface : stackValue.get("surfaces")) {
					surfaces.merge(surface.get("label").asText(), 1, Integer::sum);
				}
			}
			for(Map.Entry<String, Integer> entry : surfaces.entrySet()) {
				boolean both = entry.getValue() > 1;
				switch (entry.getKey()) {
				case "BOTTOM": rotation = both ? rotation.withBottom() : rotation.withBottomAtZeroDegrees(); break;
				case "TOP": rotation = both ? rotation.withTop() : rotation.withTopAtZeroDegrees(); break;
				case "LEFT": rotation = both ? rotation.withLeft() : rotation.withLeftAtZeroDegrees(); break;
				case "RIGHT": rotation = both ? rotation.withRight() : rotation.withRightAtZeroDegrees(); break;
				case "FRONT": rotation = both ? rotation.withFront() : rotation.withFrontAtZeroDegrees(); break;
				case "REAR": rotation = both ? rotation.withRear() : rotation.withRearAtZeroDegrees(); break;
				default: throw new IllegalArgumentException("Unknown surface " + entry.getKey());
				}
			}
			Box box = Box.newBuilder()
					.withId(b.get("id").asText())
					.withSize(first.get("dx").asInt(), first.get("dy").asInt(), first.get("dz").asInt())
					.withWeight(b.get("weight").asInt())
					.withRotation(rotation.build())
					.build();
			if(box.getStackValues().length != stackValues.size()) {
				throw new IllegalArgumentException("Expected " + stackValues.size() + " rotations for " + box.getId() + ", got " + box.getStackValues().length);
			}
			items.add(new BoxItem(box, item.get("count").asInt()));
		}
		return items;
	}

	private static JsonNode read(String resource) {
		try (InputStream in = ShippingOrders.class.getResourceAsStream(resource)) {
			if(in == null) {
				throw new IllegalArgumentException("Missing resource " + resource);
			}
			return new ObjectMapper().readTree(in);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * The original report: 3 boxes which fit in one row along x (3 × 7125 = 21375), but only when
	 * rotated so that the shorter side is along x.
	 */
	public static List<ContainerItem> getRowContainer() {
		return ContainerItem.newListBuilder()
				.withContainer(container("LYVZ 3 Box", 21375, 9750, 6750, 0, 1_000_000), 3)
				.build();
	}

	public static List<BoxItem> getRowBoxes() {
		List<BoxItem> items = new ArrayList<>();
		items.add(new BoxItem(Box.newBuilder().withId("A").withSize(9500, 7125, 6250).withWeight(1).withRotate3D().build(), 3));
		return items;
	}
}
