package com.github.skjolber.packing.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;
import com.github.skjolber.packing.api.Unloading;

/**
 * Which placements rest on which, calculated from the placements' positions: a placement rests on another when its
 * bottom touches the other's top and their footprints overlap; the overlap is the contact area. This includes a box
 * placed later, for example into a gap under an overhang: it carries part of the boxes resting on it. Whether it
 * relieves their other supporters, and counts as their support, depends on how the boxes are unloaded
 * (see {@link Unloading}); the placements are in loading order.
 * <br>
 * <br>
 * Validators use this instead of the links recorded on the placements ({@link Placement#getSupporters()} and so on),
 * which only packagers with load limits or support record. The placements are not modified.
 */
public class SupportGraph {

	private static final class Node {
		private final List<PlacementLoad> supporters = new ArrayList<>(2);
		private final List<PlacementLoad> supportees = new ArrayList<>(2);
		private long supportedArea;
		private long lateSupportedArea;
		private final int index;

		private Node(int index) {
			this.index = index;
		}
	}

	private final Map<Placement, Node> nodes;

	/**
	 * Support graph for boxes unloaded in any order, see {@link Unloading#ANY_ORDER}.
	 *
	 * @param placements placements in loading order
	 */
	public SupportGraph(List<Placement> placements) {
		this(placements, Unloading.ANY_ORDER);
	}

	/**
	 * @param placements placements in loading order
	 * @param unloading how the boxes are unloaded
	 */
	public SupportGraph(List<Placement> placements, Unloading unloading) {
		nodes = new IdentityHashMap<>(placements.size() * 2);
		// placements by the height just above their top
		Map<Integer, List<Placement>> byTop = new HashMap<>();
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			nodes.put(placement, new Node(i));
			byTop.computeIfAbsent(placement.getAbsoluteEndZ() + 1, k -> new ArrayList<>()).add(placement);
		}
		for (Placement supportee : placements) {
			List<Placement> candidates = byTop.get(supportee.getAbsoluteZ());
			if(candidates == null) {
				continue;
			}
			Node supporteeNode = nodes.get(supportee);
			for (Placement supporter : candidates) {
				long area = getContactArea(supporter, supportee);
				if(area > 0) {
					Node supporterNode = nodes.get(supporter);
					// placed after the supportee, which might be unloaded first
					boolean late = unloading == Unloading.ANY_ORDER && supporterNode.index > supporteeNode.index;
					supporteeNode.supporters.add(new PlacementLoad(supporter, area, 0.0, late));
					if(late) {
						supporteeNode.lateSupportedArea += area;
					} else {
						supporteeNode.supportedArea += area;
					}
					supporterNode.supportees.add(new PlacementLoad(supportee, area, 0.0, late));
				}
			}
		}
	}

	/**
	 * @return the overlap of the two placements' footprints, or 0 if none
	 */
	public static long getContactArea(Placement a, Placement b) {
		long dx = Math.min(a.getAbsoluteEndX(), b.getAbsoluteEndX()) - Math.max(a.getAbsoluteX(), b.getAbsoluteX()) + 1;
		if(dx <= 0) {
			return 0;
		}
		long dy = Math.min(a.getAbsoluteEndY(), b.getAbsoluteEndY()) - Math.max(a.getAbsoluteY(), b.getAbsoluteY()) + 1;
		if(dy <= 0) {
			return 0;
		}
		return dx * dy;
	}

	/**
	 * @return the placements resting on this placement, with their contact areas
	 */
	public List<PlacementLoad> getSupportees(Placement placement) {
		Node node = nodes.get(placement);
		return node != null ? node.supportees : Collections.emptyList();
	}

	/**
	 * @return the placements this placement rests on, with their contact areas
	 */
	public List<PlacementLoad> getSupporters(Placement placement) {
		Node node = nodes.get(placement);
		return node != null ? node.supporters : Collections.emptyList();
	}

	/**
	 * The share of the weight passed down by {@code supportee} (its own and the load on it) which one of its supporters
	 * carries: by contact area among the supporters which count as support, and among all supporters for the others
	 * (see {@link PlacementLoad#isLate()}).
	 *
	 * @param supportee the placement above
	 * @param link the link between the supportee and the supporter (from either side)
	 * @return the share, or 0 if none
	 */
	public double getShare(Placement supportee, PlacementLoad link) {
		Node node = nodes.get(supportee);
		if(node == null) {
			return 0.0;
		}
		if(link.isLate()) {
			return (double)link.getArea() / (node.supportedArea + node.lateSupportedArea);
		}
		return node.supportedArea == 0 ? 0.0 : (double)link.getArea() / node.supportedArea;
	}

	/**
	 * @return the area of this placement resting on placements which count as support (not counting the container floor),
	 *         see {@link PlacementLoad#isLate()}
	 */
	public long getSupportedArea(Placement placement) {
		Node node = nodes.get(placement);
		return node != null ? node.supportedArea : 0L;
	}
}
