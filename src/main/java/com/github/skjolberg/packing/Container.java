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
	
	public void add(Box box) {
		levels.get(levels.size() - 1).add(box);
	}
	
	public void addLevel() {
		add(new Level());
	}
	
	public Box getRemainigFreeSpace(Dimension box) {
		int spaceHeight = box.getHeight() - getStackHeight();
		if(spaceHeight < 0) {
			throw new IllegalArgumentException();
		}
		return new Box(box.getWidth(), box.getDepth(), spaceHeight);
	}
	
	public String printLevels() {
		StringBuffer buffer = new StringBuffer();
		
		for(int i = 0; i < levels.size(); i++) {
			Level level = levels.get(i);
			buffer.append(i + " " + level.size() + " element / " + level.getHeight() + " height");
			buffer.append(level);
			buffer.append("\n");
		}
		
		return buffer.toString();
	}
	
	public ArrayList<Level> getLevels() {
		return levels;
	}
}
