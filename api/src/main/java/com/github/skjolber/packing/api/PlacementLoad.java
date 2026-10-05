package com.github.skjolber.packing.api;

/**
 * Carries a supporting {@link Placement} together with the overlap area it shares
 * with the placement above it and the proportional weight share it bears.
 */
public class PlacementLoad {
	
	private final Placement placement;
	private final long area;
	private final double weight;
	private final boolean late;

	public PlacementLoad(Placement placement, long area) {
		this(placement, area, 0);
	}

	public PlacementLoad(Placement placement, long area, double weight) {
		this(placement, area, weight, false);
	}

	/**
	 * @param late whether the supporter was placed after the supportee and does not relieve its other supporters,
	 *        see {@link Unloading#ANY_ORDER}
	 */
	public PlacementLoad(Placement placement, long area, double weight, boolean late) {
		this.placement = placement;
		this.area = area;
		this.weight = weight;
		this.late = late;
	}

	/**
	 * Whether the supporter was placed after the supportee, and carries a share of it without relieving its other
	 * supporters (see {@link Unloading#ANY_ORDER}). Such a supporter does not count towards the supported area.
	 */
	public boolean isLate() {
		return late;
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
	 * The proportional, potentially fractional share of the weight that this supporter bears.
	 */
	public double getWeight() {
		return weight;
	}

}
