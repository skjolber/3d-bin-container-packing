package com.github.skjolber.packing.validator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.github.skjolber.packing.api.Placement;
import com.github.skjolber.packing.api.PlacementLoad;

/**
 * Which placements rest on which, calculated from the placements' positions: a placement rests on another placed
 * before it (earlier in the list, which is the loading order) when its bottom touches the other's top and their
 * footprints overlap; the overlap is the contact area. A box placed later, for example into a gap under an overhang,
 * does not carry the boxes which are already above it, as with the packagers.
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
		private final int index;

		private Node(int index) {
			this.index = index;
		}
	}

	private final Map<Placement, Node> nodes;

	/**
	 * @param placements placements in loading order (as in a stack)
	 */
	public SupportGraph(List<Placement> placements) {
		nodes = new IdentityHashMap<>(placements.size() * 2);
		// placements by the height just above their top
		Map<Integer, List<Placement>> byTop = new HashMap<>();
		for (int i = 0; i < placements.size(); i++) {
			Placement placement = placements.get(i);
			nodes.put(placement, new Node(i));
			byTop.computeIfAbsent(placement.getAbsoluteEndZ() + 1, k -> new ArrayList<>()).add(placement);
		}
		for (int i = 0; i < placements.size(); i++) {
			Placement supportee = placements.get(i);
			List<Placement> candidates = byTop.get(supportee.getAbsoluteZ());
			if(candidates == null) {
				continue;
			}
			Node supporteeNode = nodes.get(supportee);
			for (Placement supporter : candidates) {
				if(nodes.get(supporter).index >= i) {
					// placed after the supportee
					continue;
				}
				long area = getContactArea(supporter, supportee);
				if(area > 0) {
					supporteeNode.supporters.add(new PlacementLoad(supporter, area));
					supporteeNode.supportedArea += area;
					nodes.get(supporter).supportees.add(new PlacementLoad(supportee, area));
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
	 * @return the area of this placement resting on other placements (not counting the container floor)
	 */
	public long getSupportedArea(Placement placement) {
		Node node = nodes.get(placement);
		return node != null ? node.supportedArea : 0L;
	}
}
