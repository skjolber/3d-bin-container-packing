package com.github.skjolberg.packing;

import java.util.ArrayList;

import static java.lang.Math.max;

public class Container extends Box {

	private int stackHeight = 0;
	private ArrayList<Level> levels = new ArrayList<Level>();
	
	public Container(Dimension dimension) {
		super(dimension.getName(), dimension.getWidth(), dimension.getDepth(), dimension.getHeight());
	}

	public Container(int w, int d, int h) {
		super(w, d, h);
	}

	public Container(String name, int w, int d, int h) {
		super(name, w, d, h);
	}

	public boolean add(Level element) {
		if(!levels.isEmpty()) {
			stackHeight += currentLevelStackHeight();
		}
		
		return levels.add(element);
	}
	
	public int getStackHeight() {
		return stackHeight + currentLevelStackHeight();
	}
	
	public void add(int index, Level element) {
		if(!levels.isEmpty()) {
			stackHeight += currentLevelStackHeight();
		}
		
		levels.add(index, element);
	}
	
	public int currentLevelStackHeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getHeight();
	}
	
	public void add(Placement placement) {
		levels.get(levels.size() - 1).add(placement);
	}
	
	public void addLevel() {
		add(new Level());
	}
	
	public Dimension getFreeSpace() {
		int spaceHeight = height - getStackHeight();
		if(spaceHeight < 0) {
			throw new IllegalArgumentException("Remaining free space is negative at " + spaceHeight);
		}
		return new Dimension(width, depth, spaceHeight);
	}
	
	public ArrayList<Level> getLevels() {
		return levels;
	}
	
	public Placement get(int level, int placement) {
		return levels.get(level).get(placement);
	}

	public void validateCurrentLevel() {
		levels.get(levels.size() - 1).validate();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((levels == null) ? 0 : levels.hashCode());
		result = prime * result + stackHeight;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Container other = (Container) obj;
		if (levels == null) {
			if (other.levels != null)
				return false;
		} else if (!levels.equals(other.levels))
			return false;
		if (stackHeight != other.stackHeight)
			return false;
		return true;
	}
	
	public void clear() {
		levels.clear();
		stackHeight = 0;
	}
	
	public int getBoxCount() {
		int count = 0;
		for(Level level : levels) {
			count += level.size();
		}
		return count;
	}

	public Dimension getUsedSpace() {
		Dimension maxBox = new Dimension();
		int height = 0;
		for (Level level : levels) {
			maxBox = getUsedSpace(level, maxBox, height);
			height += level.getHeight();
		}
		return maxBox;
	}

	private Dimension getUsedSpace(Level level, Dimension maxBox, int height) {
		for (Placement placement : level) {
			maxBox = boundingBox(maxBox, getUsedSpace(placement, height));
		}
		return maxBox;
	}

	private Dimension getUsedSpace(Placement placement, int height) {
		final Box box = placement.getBox();
		final Space space = placement.getSpace();
		return new Dimension(
				space.getX() + box.getWidth(),
				space.getY() + box.getDepth(),
				height + box.getHeight());
	}

	private Dimension boundingBox(final Dimension b1, final Dimension b2) {
		return new Dimension(
				max(b1.getWidth(), b2.getWidth()),
				max(b1.getDepth(), b2.getDepth()),
				max(b1.getHeight(), b2.getHeight()));

	}
}