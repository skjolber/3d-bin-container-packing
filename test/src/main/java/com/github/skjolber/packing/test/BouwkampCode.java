package com.github.skjolber.packing.test;

import java.util.ArrayList;
import java.util.List;

public class BouwkampCode {
	
	protected int width;
	protected int depth;
	protected String name;
	
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
		return "BouwkampCode [name=" + name + ", width=" + width + ", depth=" + depth + ", square=" + square + "]";
	}
	
}
