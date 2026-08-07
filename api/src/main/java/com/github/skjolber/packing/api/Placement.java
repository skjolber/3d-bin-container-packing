package com.github.skjolber.packing.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import com.github.skjolber.packing.api.point.Point;

public class Placement implements Serializable {

	private static final long serialVersionUID = 1L;

	protected BoxStackValue stackValue;
	protected int x;
	protected int y;
	protected int z;
	
	protected int pointIndex;
	protected int index;

	protected long supportedArea;

	// -----------------------------------------------------------------------
	// Box-load tracking
	// -----------------------------------------------------------------------

	protected List<PlacementLoad> supporters = new ArrayList<>(4);
	protected List<PlacementLoad> supportees = new ArrayList<>(4);
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 */
	protected long loadWeight;
	
	protected Object properties;

	public Placement(BoxStackValue stackValue, int index, int x, int y, int z) {
		super();
		this.stackValue = stackValue;
		this.pointIndex = index;
		
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Placement(BoxStackValue stackValue, Point point) {
		this(stackValue, point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
	}

	public Placement() {
	}

	public BoxStackValue getStackValue() {
		return stackValue;
	}

	public void setStackValue(BoxStackValue stackValue) {
		this.stackValue = stackValue;
	}

	public boolean intersects(Placement placement) {
		return intersectsX(placement) && intersectsY(placement) && intersectsZ(placement);
	}

	public boolean intersectsY(Placement placement) {
		int endY = y + stackValue.getDy() - 1;

		if (y <= placement.getAbsoluteY() && placement.getAbsoluteY() <= endY) {
			return true;
		}

		return y <= placement.getAbsoluteY() + placement.getStackValue().getDy() - 1
				&& placement.getAbsoluteY() + placement.getStackValue().getDy() - 1 <= endY;
	}

	public boolean intersectsX(Placement placement) {
		int endX = x + stackValue.getDx() - 1;

		if (x <= placement.getAbsoluteX() && placement.getAbsoluteX() <= endX) {
			return true;
		}

		return x <= placement.getAbsoluteX() + placement.getStackValue().getDx() - 1
				&& placement.getAbsoluteX() + placement.getStackValue().getDx() - 1 <= endX;
	}

	public boolean intersectsZ(Placement placement) {
		int endZ = z + stackValue.getDz() - 1;

		if (z <= placement.getAbsoluteZ() && placement.getAbsoluteZ() <= endZ) {
			return true;
		}

		return z <= placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1
				&& placement.getAbsoluteZ() + placement.getStackValue().getDz() - 1 <= endZ;
	}

	public int getAbsoluteX() {
		return x;
	}

	public int getAbsoluteY() {
		return y;
	}

	public int getAbsoluteZ() {
		return z;
	}

	public int getAbsoluteEndX() {
		return x + stackValue.getDx() - 1;
	}

	public int getAbsoluteEndY() {
		return y + stackValue.getDy() - 1;
	}

	public int getAbsoluteEndZ() {
		return z + stackValue.getDz() - 1;
	}

	public long getVolume() {
		return stackValue.getBox().getVolume();
	}
	
	public boolean intersects2D(int placementX, int placementEndX, int placementY, int placementEndY) {
		return !(
				placementEndX < x || placementX > getAbsoluteEndX() || 
				placementEndY < y || placementY > getAbsoluteEndY()
				);
	}

	public boolean intersects2D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x || placement.getAbsoluteX() > getAbsoluteEndX() || 
				placement.getAbsoluteEndY() < y || placement.getAbsoluteY() > getAbsoluteEndY()
				);
	}

	public boolean intersects3D(Placement placement) {
		return !(
				placement.getAbsoluteEndX() < x ||
				placement.getAbsoluteX() > getAbsoluteEndX() ||
				placement.getAbsoluteEndY() < y ||
				placement.getAbsoluteY() > getAbsoluteEndY() ||
				placement.getAbsoluteEndZ() < z ||
				placement.getAbsoluteZ() > getAbsoluteEndZ()
				);
	}

	@Override
	public String toString() {		
		Box box = stackValue.getBox();
		return (box != null ? box.getId() : "") + "[" +x + "x" + y + "x" + z + " " + getAbsoluteEndX() + "x"
				+ getAbsoluteEndY() + "x" + getAbsoluteEndZ() + "]";
	}
	
	public void setPoint(int index, int x, int y, int z) {
		this.pointIndex = index;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public void setPoint(Point point) {
		setPoint(point.getIndex(), point.getMinX(), point.getMinY(), point.getMinZ());
	}
	
	public int getWeight() {
		return stackValue.getBox().getWeight();
	}

	public BoxItem getBoxItem() {
		return stackValue.getBox().getBoxItem();
	}
	
	public Box getBox() {
		return stackValue.getBox();
	}

	public int getPointIndex() {
		return pointIndex;
	}
	
	/**
	 * Total weight of all boxes resting on top of this placement.
	 * Includes all boxes in the vertical stack above, adjusted for area-proportional distribution.
	 *
	 * @return accumulated load weight, in the same units as {@link Box#getWeight()}
	 */
	public long getLoadWeight() {
		return loadWeight;
	}

	/**
	 * Returns the load pressure on the top surface of this placement,
	 * expressed as {@code loadWeight × 1000 / topArea}, matching the
	 * convention used by {@link Box#getMinimumPressure()}.
	 *
	 * @return load pressure, or 0 if the area is zero
	 */
	public long getLoadPressure() {
		long area = stackValue.getArea();
		if(area == 0) {
			return 0;
		}
		return (loadWeight * 1000L) / area;
	}

	/**
	 * Returns the list of placements that are directly supported by this placement.
	 *
	 * @return list of supportees
	 */
	public List<PlacementLoad> getSupportees() {
		return supportees;
	}

	/**
	 * Returns the list of placements that are directly supporting this placement.
	 *
	 * @return list of supporters
	 */
	public List<PlacementLoad> getSupporters() {
		return supporters;
	}

	
	/**
	 * Records that {@code supportee} is resting on top of this placement.
	 * Sets up a two-way relationship and propagates weight and stack levels 
	 * down through the support graph.
	 *
	 * @param supportee the placement resting on top
	 * @param area the area shared between the two
	 * @param weight the initial weight share of the supportee box itself
	 */
	public void addLoad(Placement supportee, long area, long weight) {
		addSupportee(new PlacementLoad(supportee, area, weight));
		supportee.addSupporter(new PlacementLoad(this, area, weight));

		propagateLoad(weight);
	}
	
	protected void addSupportee(PlacementLoad supporter) {
		this.supportees.add(supporter);
	}

	protected void addSupporter(PlacementLoad supporter) {
		this.supporters.add(supporter);
		
		supportedArea += supporter.getArea();
	}

	protected void propagateLoad(long weightIncrement) {
		this.loadWeight += weightIncrement;

		if(!supporters.isEmpty()) {
			for (int i = 0; i < supporters.size(); i++) {
				PlacementLoad supporterLink = supporters.get(i);
				long share = (weightIncrement * supporterLink.getArea()) / supportedArea;
				supporterLink.getPlacement().propagateLoad(share);
			}
		}
	}
	
	public void removeLoad(Placement supportee) {
		PlacementLoad toRemove = null;
		for(PlacementLoad supporteeLink : supportees) {
			if(supporteeLink.getPlacement() == supportee) {
				toRemove = supporteeLink;
				break;
			}
		}
		
		if(toRemove != null) {
			supportees.remove(toRemove);
			supportee.removeSupporter(this);
			
			propagateLoad(-toRemove.getWeight());
		}
	}
	
	public void clearLoad() {
		supportees.clear();
		supporters.clear();
		
		loadWeight = 0;
		supportedArea = 0;
	}

	public void removeSupporter(Placement placement) {
		for(int i = 0; i < supporters.size(); i++) {
			PlacementLoad supporterLink = supporters.get(i);
			if(supporterLink.getPlacement() == placement) {
				supporters.remove(i);
				supportedArea -= supporterLink.getArea();
				
				propagateLoad(-supporterLink.getWeight());
				break;
			}
		}
	}

	public long getSupportedArea() {
		return supportedArea;
	}

	public void setSupportedArea(long supportedArea) {
		this.supportedArea = supportedArea;
	}
	
	public void setIndex(int index) {
		this.index = index;
	}
	
	public int getIndex() {
		return index;
	}

	public boolean isWithinMaxLoadBoxCount(int levels) {
		if(stackValue.isMaxLoadBoxCount()) {
			if(stackValue.getMaxLoadBoxCount() < levels) {
				return false;
			}
		}
		
		levels++;
		for (PlacementLoad placementLoad : supporters) {
			if(!placementLoad.getPlacement().isWithinMaxLoadBoxCount(levels)) {
				return false;
			}
		}
		
		return true;
	}

	public <T> T getProperties() {
		return (T) properties;
	}

	public <T> void setProperties(T properties) {
		this.properties = properties;
	}

	/**
	 * Determines whether this placement is stable considering both its supporters
	 * <em>and</em> the weight of every box stacked above it (its supportees,
	 * recursively).
	 *
	 * <p>The effective centre of mass (CoM) is computed as the weighted average of
	 * the geometric centres of this box and all boxes in its supportee sub-tree.
	 * When a box above is shared between multiple supporters (split load), its
	 * contribution to this sub-tree is scaled by
	 * {@code overlapArea / supportee.supportedArea}, matching the same proportion
	 * used by {@link #propagateLoad(long)}.
	 *
	 * <p>The effective CoM is tested against the support region:
	 * <ul>
	 *   <li>For boxes resting on the container floor ({@code z == 0}, no supporters):
	 *       the support region is the box's own footprint.  A heavy off-centre
	 *       supportee can still cause tipping.</li>
	 *   <li>For elevated boxes: the support region is the axis-aligned bounding box
	 *       of the union of all support contact patches, identical to
	 *       {@link #isStableSupport()}.</li>
	 * </ul>
	 *
	 * @return {@code true} if the effective centre of mass of this box and its
	 *         entire stack lies within the support region, {@code false} otherwise
	 * @see #isStableSupport()
	 */

	public boolean isStable() {
		// Fast path: if the box is fully supported, the support region is the full
		// footprint — we still need to verify the combined CoM is within it, but we
		// can skip the supporter-loop and use the footprint bounds directly.
		
		if(supporters.isEmpty()) {
			return z == 0;
		}
		
		if(supportedArea == stackValue.getArea()) {
			return true;
		}
		
		int minSupportX = Integer.MAX_VALUE;
		int maxSupportX = Integer.MIN_VALUE;
		int minSupportY = Integer.MAX_VALUE;
		int maxSupportY = Integer.MIN_VALUE;

		for(PlacementLoad supporterLink : supporters) {
			Placement supporter = supporterLink.getPlacement();

			int overlapMinX = Math.max(x, supporter.x);
			int overlapMaxX = Math.min(getAbsoluteEndX(), supporter.getAbsoluteEndX());
			int overlapMinY = Math.max(y, supporter.y);
			int overlapMaxY = Math.min(getAbsoluteEndY(), supporter.getAbsoluteEndY());

			if(overlapMinX < minSupportX) minSupportX = overlapMinX;
			if(overlapMaxX > maxSupportX) maxSupportX = overlapMaxX;
			if(overlapMinY < minSupportY) minSupportY = overlapMinY;
			if(overlapMaxY > maxSupportY) maxSupportY = overlapMaxY;
		}

		// Accumulate weighted CoM of this box + the proportional share of every
		// box above it. Values are kept ×2 in integer arithmetic (same trick as
		// isStable) and scaled by 1000 to survive proportional division.
		long[] stack = accumulateStackCenterOfMass(1000L);
		long totalWeight = stack[0];
		if(totalWeight == 0) {
			// Zero-weight stack: fall back to geometric centre
			int com2x = 2 * x + stackValue.getDx();
			int com2y = 2 * y + stackValue.getDy();
			return com2x >= 2 * minSupportX && com2x <= 2 * maxSupportX && com2y >= 2 * minSupportY && com2y <= 2 * maxSupportY;
		}

		// Effective CoM ×2 (still scaled by 1000, but ratio cancels out)
		long com2x = stack[1] / totalWeight;
		long com2y = stack[2] / totalWeight;

		return com2x >= 2 * minSupportX && com2x <= 2 * maxSupportX && com2y >= 2 * minSupportY && com2y <= 2 * maxSupportY;
	}

	/**
	 * Recursively accumulates the weighted centre-of-mass contribution of this
	 * placement and all boxes above it.
	 *
	 * <p>The {@code share} parameter is a fixed-point multiplier (e.g. 1000 at the
	 * root) that represents the fraction of this sub-tree's weight attributed to
	 * the root caller.  For split-load supportees it is reduced proportionally.
	 *
	 * @param share  fixed-point weight multiplier for this sub-tree
	 * @return {@code long[3]} where {@code [0]} = total effective weight,
	 *         {@code [1]} = weighted CoM-X sum (×2), {@code [2]} = weighted CoM-Y sum (×2)
	 */
	protected long[] accumulateStackCenterOfMass(long share) {
		long w = (long) getWeight() * share;
		// CoM of this box ×2 (centre of footprint)
		long com2x = 2L * x + stackValue.getDx();
		long com2y = 2L * y + stackValue.getDy();

		long totalWeight  = w;
		long weightedComX = w * com2x;
		long weightedComY = w * com2y;

		for(PlacementLoad supporteeLink : supportees) {
			Placement supportee = supporteeLink.getPlacement();
			long supporteeArea = supportee.supportedArea;
			if(supporteeArea == 0) {
				continue;
			}
			// Proportion of this supportee's sub-tree attributed to us
			long overlapMinX = Math.max(x, supportee.x);
			long overlapMaxX = Math.min(getAbsoluteEndX(), supportee.getAbsoluteEndX());
			long overlapMinY = Math.max(y, supportee.y);
			long overlapMaxY = Math.min(getAbsoluteEndY(), supportee.getAbsoluteEndY());

			if(overlapMinX > overlapMaxX || overlapMinY > overlapMaxY) {
				continue;
			}

			long overlapArea = (overlapMaxX - overlapMinX + 1) * (overlapMaxY - overlapMinY + 1);
			long supporteeShare = (share * overlapArea) / supporteeArea;
			if(supporteeShare == 0) {
				continue;
			}

			long[] sub = supportee.accumulateStackCenterOfMass(supporteeShare);
			totalWeight  += sub[0];
			weightedComX += sub[1];
			weightedComY += sub[2];
		}

		return new long[] { totalWeight, weightedComX, weightedComY };
	}

	/**
	 * Determines whether this placement is stable given its current supporters,
	 * assuming uniform weight distribution within the box.
	 *
	 * <p>A box is considered stable when its centre of mass (CoM) — located at the
	 * geometric centre of its XY footprint — lies within the axis-aligned bounding
	 * box of the union of all support contact patches between this placement and its
	 * supporters.  This is an exact test for the common case where every contact
	 * patch is an axis-aligned rectangle.
	 *
	 * <p>A placement with no supporters is considered unstable unless it rests on
	 * the container floor (z == 0).
	 *
	 * @return {@code true} if the centre of mass is within the support region,
	 *         {@code false} if the box would topple
	 */
	
	public boolean isStableSupport() {
		if(supporters.isEmpty()) {
			return z == 0;
		}

		// Fast path: full footprint coverage guarantees the CoM is within the
		// support region without any geometric calculation.
		if(supportedArea >= stackValue.getArea()) {
			return true;
		}

		// Centre of mass (×2 to stay in integer arithmetic – compare against 2× bounds)
		int com2x = 2 * x + stackValue.getDx(); // = 2 * (x + dx/2)
		int com2y = 2 * y + stackValue.getDy();

		// Bounding box of the union of all supporter overlap patches
		int minSupportX = Integer.MAX_VALUE;
		int maxSupportX = Integer.MIN_VALUE;
		int minSupportY = Integer.MAX_VALUE;
		int maxSupportY = Integer.MIN_VALUE;

		for(PlacementLoad supporterLink : supporters) {
			Placement supporter = supporterLink.getPlacement();

			// Overlap rectangle between this placement's footprint and the supporter's footprint
			int overlapMinX = Math.max(x, supporter.x);
			int overlapMaxX = Math.min(getAbsoluteEndX(), supporter.getAbsoluteEndX());
			int overlapMinY = Math.max(y, supporter.y);
			int overlapMaxY = Math.min(getAbsoluteEndY(), supporter.getAbsoluteEndY());

			if(overlapMinX < minSupportX) minSupportX = overlapMinX;
			if(overlapMaxX > maxSupportX) maxSupportX = overlapMaxX;
			if(overlapMinY < minSupportY) minSupportY = overlapMinY;
			if(overlapMaxY > maxSupportY) maxSupportY = overlapMaxY;
		}

		// CoM (×2) must lie within the support bounding box (×2)
		return com2x >= 2 * minSupportX && com2x <= 2 * maxSupportX && com2y >= 2 * minSupportY && com2y <= 2 * maxSupportY;
	}

}