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
 * Which placements rest on which, calculated from the placements' positions: a placement rests on another when its
 * bottom touches the other's top and their footprints overlap; the overlap is the contact area. This includes a box
 * placed later, for example into a gap under an overhang: it carries part of the boxes resting on it (for the order
 * in which boxes can be inserted, see {@link com.github.skjolber.packing.api.ContainerAccess}).
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
	}

	private final Map<Placement, Node> nodes;
	/** The weight resting on each placement calculated so far */
	private final Map<Placement, Double> loadWeights = new IdentityHashMap<>();

	public SupportGraph(List<Placement> placements) {
		nodes = new IdentityHashMap<>(placements.size() * 2);
		// placements by the height just above their top
		Map<Integer, List<Placement>> byTop = new HashMap<>();
		for (Placement placement : placements) {
			nodes.put(placement, new Node());
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
	 * The share of the weight passed down by {@code supportee} (its own and the load on it) which one of its supporters
	 * carries: by contact area.
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
		return node.supportedArea == 0 ? 0.0 : (double)link.getArea() / node.supportedArea;
	}

	/**
	 * @return the area of this placement resting on other placements (not counting the container floor)
	 */
	public long getSupportedArea(Placement placement) {
		Node node = nodes.get(placement);
		return node != null ? node.supportedArea : 0L;
	}

	/**
	 * The total weight resting on a placement, through all levels above it: each box above passes down its own weight
	 * and the weight resting on it, shared between its supporters (see {@link #getShare(Placement, PlacementLoad)}).
	 * <p>
	 * Each placement is calculated once, as one can be reached through several paths (and walking every path grows
	 * exponentially with the stack height).
	 *
	 * @param placement the placement
	 * @return the weight resting on it
	 */
	public double getLoadWeight(Placement placement) {
		Double known = loadWeights.get(placement);
		if(known != null) {
			return known;
		}
		double total = 0.0;
		for(PlacementLoad supporteeLink : getSupportees(placement)) {
			Placement supportee = supporteeLink.getPlacement();
			total += (supportee.getWeight() + getLoadWeight(supportee)) * getShare(supportee, supporteeLink);
		}
		loadWeights.put(placement, total);
		return total;
	}
}
