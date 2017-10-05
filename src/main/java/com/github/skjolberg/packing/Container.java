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
}
