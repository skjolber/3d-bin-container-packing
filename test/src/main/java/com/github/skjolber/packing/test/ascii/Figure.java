package com.github.skjolber.packing.test.ascii;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A drawing in text: lines of characters, without trailing blanks. Combine figures with {@linkplain Figures}.
 * <p>
 * {@linkplain #toString()} ends every line with a line feed, so that a figure compares equal to a Java text block.
 */
public final class Figure {

	/**
	 * @param lines the lines (trailing blanks are removed)
	 * @return a figure
	 */
	public static Figure of(String... lines) {
		String[] copy = new String[lines.length];
		for (int i = 0; i < lines.length; i++) {
			copy[i] = trimEnd(lines[i]);
		}
		return new Figure(copy, null);
	}

	private final String[] lines;
	private final int width;
	private final String comment;

	Figure(String[] lines, String comment) {
		this.lines = lines;
		this.comment = comment;

		int width = 0;
		for (String line : lines) {
			width = Math.max(width, line.length());
		}
		this.width = width;
	}

	/**
	 * @return the number of characters of the longest line, not counting the comment prefix
	 */
	public int getWidth() {
		return width;
	}

	/**
	 * @return the number of lines
	 */
	public int getHeight() {
		return lines.length;
	}

	/**
	 * @return the lines, without comment prefix and without trailing blanks
	 */
	public List<String> getLines() {
		List<String> result = new ArrayList<>(lines.length);
		for (String line : lines) {
			result.add(line);
		}
		return Collections.unmodifiableList(result);
	}

	/**
	 * @return the comment prefix, or null
	 */
	public String getComment() {
		return comment;
	}

	/**
	 * @param prefix put in front of every line when the figure is printed, for example "// " to paste the figure into source code, or null for none
	 * @return a copy of this figure with the comment prefix
	 */
	public Figure withComment(String prefix) {
		return new Figure(lines, prefix);
	}

	String getLine(int index) {
		return lines[index];
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String line : lines) {
			if (comment != null) {
				if (line.isEmpty()) {
					builder.append(trimEnd(comment));
				} else {
					builder.append(comment);
					builder.append(line);
				}
			} else {
				builder.append(line);
			}
			builder.append('\n');
		}
		return builder.toString();
	}

	static String trimEnd(String line) {
		int end = line.length();
		while (end > 0 && line.charAt(end - 1) == ' ') {
			end--;
		}
		return end == line.length() ? line : line.substring(0, end);
	}
}
