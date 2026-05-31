package com.github.skjolber.packing.api;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BoxStackValue {
	
	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static ListBuilder newListBuilder() {
		return new ListBuilder();
	}
	
	public static class ListBuilder {

		protected List<BoxStackValue> stackValues = new ArrayList<>();
		
		public ListBuilder withRotate(List<BoxStackValue> stackValues) {
			this.stackValues = stackValues;
			return this;
		}

		public ListBuilder withRotate(BoxStackValue stackValue) {
			this.stackValues.add(stackValue);
			return this;
		}
		
		public ListBuilder withRotate(Consumer<BoxStackValue.AbstractBuilder> stackValue) {
			Builder builder = new Builder();
			stackValue.accept(builder);
			this.stackValues.add(builder.build());
			return this;
		}

		public ListBuilder withRotate2D(Consumer<BoxStackValue.AbstractBuilder> stackValue) {
			Builder builder = new Builder();
			stackValue.accept(builder);
			this.stackValues.add(builder.build());
			builder.withDimensions(builder.dy, builder.dx, builder.dz);
			this.stackValues.add(builder.build());
			return this;
		}
		
		public List<BoxStackValue> build() {
			return stackValues;
		}
	}

	public abstract static class AbstractBuilder<T extends AbstractBuilder<T>> {

		protected int dx = -1;
		protected int dy = -1;
		protected int dz = -1;

		protected BoxItem boxItem;

		protected List<Surface> surfaces;
		protected int index = 0;

		protected long maxLoadWeight   = -1;
		protected long maxLoadPressure = -1;
		protected int maxLoadBoxCount = -1;
		protected boolean maxLoadIdenticalOnly = false;

		public T withBoxItem(BoxItem boxItem) {
			this.boxItem = boxItem;
			return (T) this;
		}
		
		public T withDimensions(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			return (T) this;
		}

		public T withSurfaces(List<Surface> surfaces) {
			this.surfaces = surfaces;
			return (T) this;
		}

		public T withIndex(int index) {
			this.index = index;
			return (T) this;
		}

		/**
		 * Sets the max weight allowed on top. If {@link #withMaxLoadPressure} is also set,
		 * pressure takes precedence.
		 */
		public T withMaxLoadWeight(long weight) {
			this.maxLoadWeight = weight;
			return (T) this;
		}

		/**
		 * Sets the max load as a pressure value (weight × 1000 / area), matching the
		 * convention used by {@link Box#getMinimumPressure()} / {@link Box#getMaximumPressure()}.
		 * Takes precedence over {@link #withMaxLoadWeight} when both are set.
		 */
		public T withMaxLoadPressure(long pressure) {
			this.maxLoadPressure = pressure;
			return (T) this;
		}

		/** Sets max total boxes allowed on top. -1 means no limit. */
		public T withMaxLoadBoxCount(int count) {
			this.maxLoadBoxCount = count;
			this.maxLoadIdenticalOnly = false;
			return (T) this;
		}

		/** 
		 * Sets max boxes allowed on top, and restricts them to be of the same type. 
		 * -1 means no limit on count, but still restricted to identical boxes.
		 */
		public T withMaxLoadIdenticalBoxCount(int count) {
			this.maxLoadBoxCount = count;
			this.maxLoadIdenticalOnly = true;
			return (T) this;
		}

		/**
		 * Sets whether only identical boxes can be stacked on top.
		 * 
		 * @param identicalOnly true if only identical boxes
		 * @return this
		 */
		public T withMaxLoadIdenticalOnly(boolean identicalOnly) {
			this.maxLoadIdenticalOnly = identicalOnly;
			return (T) this;
		}

	}

	public static class Builder extends AbstractBuilder<Builder> {

		public BoxStackValue build() {
			return new BoxStackValue(dx, dy, dz, surfaces, index,
					maxLoadWeight, maxLoadPressure, maxLoadBoxCount, maxLoadIdenticalOnly);
		}
	}

	// -------------------------------------------------------------------------

	protected final int dx; // width
	protected final int dy; // depth
	protected final int dz; // height

	protected final long area;

	protected final List<Surface> surfaces;
	protected long volume;

	protected final int index;
	protected Box box;

	protected final long maxLoadWeight;
	protected final long maxLoadPressure;
	protected final int maxLoadBoxCount;
	protected final boolean maxLoadBoxCountIdenticalOnly;

	public BoxStackValue(int dx, int dy, int dz, List<Surface> surfaces, int index) {
		this(dx, dy, dz, surfaces, index, -1, -1, -1, false);
	}

	public BoxStackValue(int dx, int dy, int dz, List<Surface> surfaces, int index,
			long maxLoadWeight, long maxLoadPressure, int maxLoadBoxCount, boolean maxLoadIdenticalOnly) {
		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.surfaces = surfaces;

		this.area = (long) dx * (long) dy;
		this.volume = area * (long) dz;

		this.index = index;
		this.maxLoadWeight = maxLoadWeight;
		this.maxLoadPressure = maxLoadPressure;
		this.maxLoadBoxCount = maxLoadBoxCount;
		this.maxLoadBoxCountIdenticalOnly = maxLoadIdenticalOnly;
	}

	protected BoxStackValue(BoxStackValue other) {
		this.dx = other.dx;
		this.dy = other.dy;
		this.dz = other.dz;
		this.surfaces = other.surfaces;

		this.area = other.area;
		this.volume = other.volume;
		this.index = other.index;

		this.box = other.box;
		this.maxLoadWeight = other.maxLoadWeight;
		this.maxLoadPressure = other.maxLoadPressure;
		this.maxLoadBoxCount = other.maxLoadBoxCount;
		this.maxLoadBoxCountIdenticalOnly = other.maxLoadBoxCountIdenticalOnly;
	}

	public int getDx() {
		return dx;
	}

	public int getDy() {
		return dy;
	}

	public int getDz() {
		return dz;
	}

	/**
	 * Check whether this object fits within a dimension (without rotation).
	 *
	 * @param dimension the dimensions to fit within
	 * @return true if this can fit within the argument space
	 */

	public boolean fitsInside3D(Container dimension) {
		return fitsInside3D(dimension.getLoadDx(), dimension.getLoadDy(), dimension.getLoadDz());
	}

	public boolean fitsInside3D(int dx, int dy, int dz) {
		return dx >= this.dx && dy >= this.dy && dz >= this.dz;
	}

	public boolean fitsInside2D(int dx, int dy) {
		return dx >= this.dx && dy >= this.dy;
	}

	public long getArea() {
		return area;
	}

	public long getVolume() {
		return volume;
	}

	/**
	 * Maximum weight that may rest on top of this stack value (i.e. this specific orientation).
	 * -1 means no limit.
	 *
	 * @return max load weight in the same unit as {@link Box#getWeight()}, or -1 if unconstrained
	 */
	public long getMaxLoadWeight() {
		return maxLoadWeight;
	}

	/**
	 * Maximum load pressure on top of this stack value (weight × 1000 / area),
	 * matching the convention used by {@link Box#getMinimumPressure()}.
	 * -1 means no limit.
	 *
	 * @return max load pressure, or -1 if unconstrained
	 */
	public long getMaxLoadPressure() {
		return maxLoadPressure;
	}

	/**
	 * Maximum number of vertical levels of boxes that may rest on top of this stack value.
	 * 
	 * @return max box count, or -1 if unconstrained
	 */
	public int getMaxLoadBoxCount() {
		return maxLoadBoxCount;
	}

	/**
	 * Whether only boxes of the same type as this one may be stacked on top.
	 * If true, any attempt to stack a different box type will violate the constraint.
	 * If false, any box type can be stacked, up to the {@link #getMaxLoadBoxCount()} limit.
	 * 
	 * @return true if identical only
	 */
	public boolean isMaxLoadBoxCountIdenticalOnly() {
		return maxLoadBoxCountIdenticalOnly;
	}

	public List<Surface> getSurfaces() {
		return surfaces;
	}

	@Override
	public String toString() {
		return "BoxStackValue[" + surfaces + " " + dx + "x" + dy + "x" + dz + "]";
	}

	@Override
	public BoxStackValue clone() {
		return new BoxStackValue(this);
	}

	public void setBox(Box box) {
		this.box = box;
	}

	public Box getBox() {
		return box;
	}

	public int getIndex() {
		return index;
	}

	public boolean isMaxLoadWeight() {
		return maxLoadWeight != -1L;
	}
	
	public boolean isMaxLoadPressure() {
		return maxLoadPressure != -1L;
	}
	
	public boolean isMaxLoadBoxCount() {
		return maxLoadBoxCount != -1;
	}
	
	public boolean isMaxLoadIdenticalBoxCount() {
		return maxLoadBoxCountIdenticalOnly;
	}

	public long getPressure() {
		// TODO optimize
		return box.getWeight() / area;
	}
	
}