package com.github.skjolber.packing.test.ascii;

/**
 * Combine {@linkplain Figure}s into one. The comment prefix of the result is the one of the first figure which has one.
 */
public final class Figures {

	private Figures() {
		// utility class
	}

	/**
	 * Put figures next to each other, aligned at the top.
	 *
	 * @param gap the number of blank columns between the figures
	 * @param figures the figures, from left to right
	 * @return a new figure
	 */
	public static Figure horizontal(int gap, Figure... figures) {
		checkArguments(gap, figures);

		int height = 0;
		for (Figure figure : figures) {
			height = Math.max(height, figure.getHeight());
		}

		String[] lines = new String[height];
		StringBuilder builder = new StringBuilder();
		for (int row = 0; row < height; row++) {
			builder.setLength(0);
			for (int i = 0; i < figures.length; i++) {
				if (i > 0) {
					appendBlanks(builder, gap);
				}
				Figure figure = figures[i];
				int length = 0;
				if (row < figure.getHeight()) {
					String line = figure.getLine(row);
					builder.append(line);
					length = line.length();
				}
				if (i < figures.length - 1) {
					appendBlanks(builder, figure.getWidth() - length);
				}
			}
			lines[row] = Figure.trimEnd(builder.toString());
		}
		return new Figure(lines, getComment(figures));
	}

	/**
	 * Put figures below each other, aligned at the left.
	 *
	 * @param gap the number of blank lines between the figures
	 * @param figures the figures, from top to bottom
	 * @return a new figure
	 */
	public static Figure vertical(int gap, Figure... figures) {
		checkArguments(gap, figures);

		int height = 0;
		for (Figure figure : figures) {
			height += figure.getHeight();
		}
		if (figures.length > 1) {
			height += gap * (figures.length - 1);
		}

		String[] lines = new String[height];
		int row = 0;
		for (int i = 0; i < figures.length; i++) {
			if (i > 0) {
				for (int j = 0; j < gap; j++) {
					lines[row++] = "";
				}
			}
			Figure figure = figures[i];
			for (int j = 0; j < figure.getHeight(); j++) {
				lines[row++] = figure.getLine(j);
			}
		}
		return new Figure(lines, getComment(figures));
	}

	private static void checkArguments(int gap, Figure[] figures) {
		if (gap < 0) {
			throw new IllegalArgumentException("Expected non-negative gap, got " + gap);
		}
		for (Figure figure : figures) {
			if (figure == null) {
				throw new IllegalArgumentException("Expected figures, got null");
			}
		}
	}

	private static String getComment(Figure[] figures) {
		for (Figure figure : figures) {
			if (figure.getComment() != null) {
				return figure.getComment();
			}
		}
		return null;
	}

	private static void appendBlanks(StringBuilder builder, int count) {
		for (int i = 0; i < count; i++) {
			builder.append(' ');
		}
	}
}
