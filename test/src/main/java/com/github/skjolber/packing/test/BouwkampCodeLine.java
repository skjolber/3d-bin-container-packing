package com.github.skjolber.packing.test;

import java.util.List;

public class BouwkampCodeLine {

	private List<Integer> squares;

	public BouwkampCodeLine(List<Integer> squares) {
		super();
		this.squares = squares;
	}

	public List<Integer> getSquares() {
		return squares;
	}

	public void setSquares(List<Integer> squares) {
		this.squares = squares;
	}

	@Override
	public String toString() {
		return "BouwkampCodeLine [squares=" + squares + "]";
	}
	
}
