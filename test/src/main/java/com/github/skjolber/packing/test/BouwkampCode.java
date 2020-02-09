package com.github.skjolber.packing.test;

import java.util.ArrayList;
import java.util.List;

public class BouwkampCode {
	
	private int width;
	private int depth;
	private String name;
	
	private List<Integer> square = new ArrayList<>();

	public int getWidth() {
		return width;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Integer> getSquare() {
		return square;
	}

	public void setSquare(List<Integer> square) {
		this.square = square;
	}

	@Override
	public String toString() {
		return "BkpLine [width=" + width + ", depth=" + depth + ", name=" + name + ", square=" + square + "]";
	}
	
	/*
	public Container toContainer(int height) {
		return new Container(width, depth, height, 0);
	}
	
	public List<Box> toBoxes(int height) {
		List<Box> boxes = new ArrayList();
		
		for (Integer integer : square) {
			boxes.add(new Box(integer, integer, height, 0));
		}
		
		return boxes;
	}
	*/
	
}
