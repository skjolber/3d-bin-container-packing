package com.github.skjolber.packing.api;

/**
 * Carries a supporting {@link Placement} together with the overlap area it shares
 * with the placement above it and the proportional weight share it bears.
 */
public class PlacementLoad {
	
	private final Placement placement;
	private final long area;
	private final long weight;

	public PlacementLoad(Placement placement, long area) {
		this(placement, area, 0);
	}

	public PlacementLoad(Placement placement, long area, long weight) {
		this.placement = placement;
		this.area = area;
		this.weight = weight;
	}

	/** The placement that is directly below and supporting the new box. */
	public Placement getPlacement() {
		return placement;
	}

	/**
	 * The XY overlap area (in the same unit² as dx/dy) between this supporter's
	 * top face and the new box's bottom face. Used to derive the proportional share
	 * of the new box's weight that this supporter must bear.
	 */
	public long getArea() {
		return area;
	}

	/**
	 * The proportional share of the weight that this supporter bears.
	 */
	public long getWeight() {
		return weight;
	}
}
