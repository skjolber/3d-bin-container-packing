package com.github.skjolberg.packing;

import java.util.ArrayList;

public class Container extends Box {

	private int stackWeight = 0;
	private int stackHeight = 0;
	private ArrayList<Level> levels = new ArrayList<Level>();

	public Container(Container container) {
		super(container.getName(), container.getWidth(), container.getDepth(), container.getHeight(), container.getWeight());
	}

	public Container(Dimension dimension, int weight) {
		super(dimension.getName(), dimension.getWidth(), dimension.getDepth(), dimension.getHeight(), weight);
	}

	public Container(int w, int d, int h, int weight) {
		super(w, d, h, weight);
	}

	public Container(String name, int w, int d, int h, int weight) {
		super(name, w, d, h, weight);
	}

	public boolean add(Level element) {
		if(!levels.isEmpty()) {
			stackHeight += currentLevelStackHeight();
			stackWeight += currentLevelStackWeight();
		}
		
		return levels.add(element);
	}
	
	public int getStackHeight() {
		return stackHeight + currentLevelStackHeight();
	}
	
	public void add(int index, Level element) {
		if(!levels.isEmpty()) {
			stackHeight += currentLevelStackHeight();
			stackWeight += currentLevelStackWeight();
		}
		
		levels.add(index, element);
	}
	
	public int currentLevelStackHeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getHeight();
	}
	
	public int currentLevelStackWeight() {
		if(levels.isEmpty()) {
			return 0;
		}
		return levels.get(levels.size() - 1).getWeight();
	}
	
	public void add(Placement placement) {
		levels.get(levels.size() - 1).add(placement);
	}
	
	public void addLevel() {
		add(new Level());
	}
	
	public Dimension getFreeSpace() {
		int remainder = height - getStackHeight();
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining free space is negative at " + remainder);
		}
		return new Dimension(width, depth, remainder);
	}
	
	public int getFreeWeight() {
		int remainder = weight - stackWeight;
		if(remainder < 0) {
			throw new IllegalArgumentException("Remaining weigth is negative at " + remainder);
		}
		return remainder;
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

	public void clear() {
		levels.clear();
		stackHeight = 0;
		stackWeight = 0;
	}
	
	public int getBoxCount() {
		int count = 0;
		for(Level level : levels) {
			count += level.size();
		}
		return count;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((levels == null) ? 0 : levels.hashCode());
		result = prime * result + stackHeight;
		result = prime * result + stackWeight;
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
		if (stackWeight != other.stackWeight)
			return false;
		return true;
	}
	
	
}
