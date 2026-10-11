package com.github.skjolber.packing.api;

import java.util.List;

public class Container {

	public static Builder newBuilder() {
		return new Builder();
	}

	public static class Builder {

		protected int emptyWeight = -1;
		protected Stack stack;

		protected String id;
		protected String description;

		protected int dx = -1; // width
		protected int dy = -1; // depth
		protected int dz = -1; // height

		protected int maxLoadWeight = -1;

		protected int loadDx = -1; // x
		protected int loadDy = -1; // y
		protected int loadDz = -1; // z

		protected ContainerAccess access = ContainerAccess.ANY;

		protected Motion motion;

		/**
		 * Set how boxes get into the container. Default {@link ContainerAccess#ANY}.
		 *
		 * @param access container access
		 * @return this builder
		 */
		public Builder withAccess(ContainerAccess access) {
			this.access = java.util.Objects.requireNonNull(access);
			return this;
		}

		public Builder withSize(int dx, int dy, int dz) {
			this.dx = dx;
			this.dy = dy;
			this.dz = dz;
			return this;
		}

		public Builder withMaxLoadWeight(int weight) {
			this.maxLoadWeight = weight;
			return this;
		}

		public Builder withLoadSize(int dx, int dy, int dz) {
			this.loadDx = dx;
			this.loadDy = dy;
			this.loadDz = dz;
			return this;
		}

		public Builder withDescription(String description) {
			this.description = description;
			return this;
		}

		public Builder withId(String id) {
			this.id = id;
			return this;
		}
		
		public Builder withStack(Stack stack) {
			this.stack = stack;
			return this;
		}

		public Builder withEmptyWeight(int emptyWeight) {
			this.emptyWeight = emptyWeight;

			return this;
		}

		/**
		 * Set the motion (acceleration) which the container is exposed to. Default none.
		 *
		 * @param motion the motion, or null for none
		 * @return this builder
		 */
		public Builder withMotion(Motion motion) {
			this.motion = motion;
			return this;
		}

		public Container build() {
			if (dx == -1) {
				throw new IllegalStateException("Expected size");
			}
			if (dy == -1) {
				throw new IllegalStateException("Expected size");
			}
			if (dz == -1) {
				throw new IllegalStateException("Expected size");
			}
			if (maxLoadWeight == -1) {
				throw new IllegalStateException("Expected max weight");
			}
			if (loadDx == -1) {
				loadDx = dx;
			}
			if (loadDy == -1) {
				loadDy = dy;
			}
			if (loadDz == -1) {
				loadDz = dz;
			}

			if (emptyWeight == -1) {
				emptyWeight = 0;
			}
			if (stack == null) {
				stack = new Stack();
			}

			return new Container(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight,
					stack, motion, access);
		}

	}

	protected final long volume;
	protected final long loadVolume;

	protected final int emptyWeight;
	/** i.e. best of the stack values */
	protected final long maxLoadVolume;
	/** i.e. best of the stack values */
	protected final int maxLoadWeight;

	protected final long maximumArea;

	protected final int dx; // x
	protected final int dy; // y
	protected final int dz; // z

	protected final int loadDx; // x
	protected final int loadDy; // y
	protected final int loadDz; // z

	protected final String id;
	protected final String description;

	protected final Stack stack;
	
	protected final Motion motion;

	protected final ContainerAccess access;

	/** Boxes which are already in the container (obstacles), in its load coordinates */
	protected final List<Placement> obstacles;

	public Container(String id, String description, int dx, int dy, int dz, int emptyWeight, int loadDx, int loadDy,
			int loadDz, int maxLoadWeight, Stack stack) {
		this(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight, stack, null);
	}

	public Container(String id, String description, int dx, int dy, int dz, int emptyWeight, int loadDx, int loadDy,
			int loadDz, int maxLoadWeight, Stack stack, Motion motion) {
		this(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight, stack, motion, ContainerAccess.ANY);
	}

	public Container(String id, String description, int dx, int dy, int dz, int emptyWeight, int loadDx, int loadDy,
			int loadDz, int maxLoadWeight, Stack stack, Motion motion, ContainerAccess access) {
		this(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight, stack, motion, access, List.of());
	}

	public Container(String id, String description, int dx, int dy, int dz, int emptyWeight, int loadDx, int loadDy,
			int loadDz, int maxLoadWeight, Stack stack, Motion motion, ContainerAccess access, List<Placement> obstacles) {
		this.access = access;
		this.obstacles = obstacles;
		this.id = id;
		this.description = description;

		this.loadDx = loadDx;
		this.loadDy = loadDy;
		this.loadDz = loadDz;

		this.loadVolume = (long) loadDx * (long) loadDy * (long) loadDz;

		this.emptyWeight = emptyWeight;

		this.maximumArea = ((long) loadDx) * loadDy;

		this.maxLoadVolume = maximumArea * (long) loadDz;
		this.maxLoadWeight = maxLoadWeight;

		this.dx = dx;
		this.dy = dy;
		this.dz = dz;
		this.volume = (long) dx * (long) dy * (long) dz;
		
		this.stack = stack;
		this.motion = motion;
	}

	public String getDescription() {
		return description;
	}

	public String getId() {
		return id;
	}

	public long getWeight() {
		return emptyWeight + stack.getWeight();
	}

	public long getMaxLoadVolume() {
		return maxLoadVolume;
	}

	public int getMaxLoadWeight() {
		return maxLoadWeight;
	}

	public int getEmptyWeight() {
		return emptyWeight;
	}

	public long getMaxWeight() {
		return (long)emptyWeight + maxLoadWeight;
	}

	public long getLoadWeight() {
		return stack.getWeight();
	}

	public long getVolume() {
		return volume;
	}

	public long getMaximumArea() {
		return maximumArea;
	}

	public long getLoadVolume() {
		return stack.getVolume();
	}

	/**
	 * Whether the box fits: its weight and volume are within the load limits, and at least one of its stack values fits the load size.
	 *
	 * @param box the box
	 * @return true if the box can be loaded, alone
	 */
	public boolean canLoad(Box box) {
		if (box.getVolume() > maxLoadVolume) {
			return false;
		}
		if (box.getWeight() > maxLoadWeight) {
			return false;
		}
		// at least one stack value must fit within the container load
		for (BoxStackValue stackValue : box.getStackValues()) {
			if (canLoad(stackValue)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether every box of the box item fits: the weight and volume of all its boxes together are within the load limits, and
	 * at least one stack value of its box fits the load size.
	 *
	 * @param boxItem the box item
	 * @return true if all the boxes of the box item can be loaded
	 */
	public boolean canLoad(BoxItem boxItem) {
		if (boxItem.getVolume() > maxLoadVolume) {
			return false;
		}
		if (boxItem.getWeight() > maxLoadWeight) {
			return false;
		}
		Box box = boxItem.getBox();
		for (BoxStackValue stackValue : box.getStackValues()) {
			if (canLoad(stackValue)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether the stack value fits the load size, ignoring weight and volume.
	 *
	 * @param stackValue the stack value
	 * @return true if the stack value fits the load size
	 */
	public boolean canLoad(BoxStackValue stackValue) {
		return stackValue.getDx() <= loadDx && stackValue.getDy() <= loadDy && stackValue.getDz() <= loadDz;
	}

	/**
	 * Whether every box of the group fits: the weight and volume of the group are within the load limits, and each box
	 * has at least one stack value which fits the load size. See also {@link #canLoadAtLeastOneBox(BoxItemGroup)}.
	 *
	 * @param group the box item group
	 * @return true if every box of the group can be loaded
	 */
	public boolean canLoad(BoxItemGroup group) {
		if (group.getVolume() > maxLoadVolume) {
			return false;
		}
		if (group.getWeight() > maxLoadWeight) {
			return false;
		}

		// all boxes must fit within the container load
		for (BoxItem boxItem : group.getItems()) {
			Box box = boxItem.getBox();
			if (!canLoad(box)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Whether at least one of the box items fits (see {@link #canLoad(Box)}).
	 *
	 * @param boxes the box items
	 * @return true if at least one box item can be loaded
	 */
	public boolean canLoadAtLeastOneBox(List<BoxItem> boxes) {

		for (BoxItem boxItem : boxes) {
			Box box = boxItem.getBox();
			if (canLoad(box)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Whether at least one of the groups fits (see {@link #canLoad(BoxItemGroup)}: every box of the group must fit).
	 *
	 * @param boxes the box item groups
	 * @return true if at least one group can be loaded
	 */
	public boolean canLoadAtLeastOneGroup(List<BoxItemGroup> boxes) {

		for (BoxItemGroup group : boxes) {
			if (canLoad(group)) {
				return true;
			}
		}
		return false;
	}

	public Container copy() {
		return copy(0);
	}

	public Container copy(int stackCapacity) {
		return new Container(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight,
				new Stack(stackCapacity), motion, access, obstacles);
	}

	public int getLoadDx() {
		return loadDx;
	}

	public int getLoadDy() {
		return loadDy;
	}

	public int getLoadDz() {
		return loadDz;
	}

	public Stack getStack() {
		return stack;
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
	 * Whether at least one box of the group fits: the weight and volume of the group are within the load limits, and at least
	 * one stack value of at least one of its boxes fits the load size. See also {@link #canLoad(BoxItemGroup)}, where every box must fit.
	 *
	 * @param boxItemGroup the box item group
	 * @return true if at least one box of the group can be loaded
	 */
	public boolean canLoadAtLeastOneBox(BoxItemGroup boxItemGroup) {
		if (boxItemGroup.getVolume() <= getMaxLoadVolume() && boxItemGroup.getWeight() <= getMaxLoadWeight()) {
			for (int i = 0; i < boxItemGroup.size(); i++) {

				Box box = boxItemGroup.get(i).getBox();
				for (BoxStackValue boxStackValue : box.getStackValues()) {
					if (boxStackValue.fitsInside3D(loadDx, loadDy, loadDz)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Whether every placement of the stack is within the load size.
	 *
	 * @param stack the stack
	 * @return true if all the placements of the stack are within the load size
	 */
	public boolean fitsInside(Stack stack) {
		List<Placement> placements = stack.getPlacements();
		for(int i = placements.size() - 1; i >= 0; i--) {
			Placement placement = placements.get(i);
			
			if(placement.getAbsoluteEndX() >= loadDx) {
				return false;
			}
			if(placement.getAbsoluteEndY() >= loadDy) {
				return false;
			}
			if(placement.getAbsoluteEndZ() >= loadDz) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		if (dx != loadDx || dy != loadDy || dz != loadDz) {
			return "Container[" + (id != null ? id : "") + "[" + dx + "x" + dy + "x" + dz + " (" + loadDx + "x" + loadDy
					+ "x" + loadDz + ")]";
		}
		return "Container[" + (id != null ? id : "") + "[" + dx + "x" + dy + "x" + dz + "]";
	}

	public Motion getMotion() {
		return motion;
	}

	/**
	 * @param obstacles boxes which are already in the container, in its load coordinates
	 * @return a copy of this container (with the same stack) with the given obstacles
	 */
	public Container withObstacles(List<Placement> obstacles) {
		return new Container(id, description, dx, dy, dz, emptyWeight, loadDx, loadDy, loadDz, maxLoadWeight,
				stack, motion, access, obstacles);
	}

	/**
	 * Boxes which are already in the container: they are inserted before the packed boxes (see {@link ContainerAccess}).
	 *
	 * @return the obstacles, or an empty list
	 */
	public List<Placement> getObstacles() {
		return obstacles;
	}

	/**
	 * @return how boxes get into the container
	 */
	public ContainerAccess getAccess() {
		return access;
	}
}
