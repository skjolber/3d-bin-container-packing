package com.github.skjolberg.packing;

import java.util.ArrayList;

public class Container extends Box {

	private int stackHeight = 0;
	private ArrayList<Level> levels = new ArrayList<Level>();
	
	public Container(Dimension box) {
		super(box.getName(), box.getWidth(), box.getDepth(), box.getHeight());
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
	
	public Dimension getRemainigFreeSpace() {
		int spaceHeight = height - getStackHeight();
		if(spaceHeight < 0) {
			throw new IllegalArgumentException();
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
}
