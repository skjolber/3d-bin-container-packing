package com.github.skjolber.packing.test.ascii;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Write {@linkplain Figure}s into the source code of the tests which create them, as comments, so that a figure never gets out of date with the
 * test.
 * <p>
 * Call {@linkplain #record(Figure)} where the figure belongs, for example after the last change to the container the test explains. By default
 * nothing happens. Run the tests with the system property {@code figures.record=true} to record the figures:
 *
 * <pre>
 * ./mvnw -pl points -am -Dtest=MyTest -Dsurefire.failIfNoSpecifiedTests=false -Dfigures.record=true test
 * </pre>
 *
 * The figures are written into the source files when the JVM shuts down: directly above the line of the call, as a block of comments which
 * starts with <code>// &lt;figure&gt;</code> and ends with <code>// &lt;/figure&gt;</code>. A block which is already there is replaced, so that running the tests
 * again updates the figure. The line of the call is the line in the test method: when the figure is recorded from a helper method of the test
 * class, or of a superclass of the test class, that is the line which calls the helper method.
 * <p>
 * The source file is found relative to the working directory, as {@code src/test/java/<package>/<class>.java}, which is where Maven runs the
 * tests of a module. If the same line records more than once (for example in a parameterized test), the first figure is kept.
 */
public final class FigureRecorder {

	/** The system property which must be {@code true} to record figures */
	public static final String PROPERTY = "figures.record";

	static final String BLOCK_START = "// <figure>";
	static final String BLOCK_END = "// </figure>";

	private static final String SOURCE_DIRECTORY = "src/test/java";

	private static final Recordings RECORDINGS = new Recordings();
	private static boolean hookRegistered = false;

	private FigureRecorder() {
		// utility class
	}

	/**
	 * @return true if figures are recorded, i.e. the system property {@code figures.record} is {@code true}. Tests can use this to avoid drawing figures
	 *         which are not recorded.
	 */
	public static boolean isEnabled() {
		return Boolean.getBoolean(PROPERTY);
	}

	/**
	 * Record a figure for the line which calls this method (or the line in the test method which calls the helper method which calls this method).
	 * Does nothing unless the system property {@code figures.record} is {@code true}.
	 *
	 * @param figure the figure, without comment prefix
	 */
	public static void record(Figure figure) {
		if (!isEnabled()) {
			return;
		}
		Objects.requireNonNull(figure);

		StackTraceElement caller = getCaller(new Throwable().getStackTrace());
		if (caller == null || caller.getLineNumber() <= 0) {
			System.err.println("Unable to record figure: no line number for the caller");
			return;
		}
		Path file = getSourceFile(caller.getClassName());
		if (file == null) {
			System.err.println("Unable to record figure: no source file for " + caller.getClassName());
			return;
		}
		RECORDINGS.add(file, caller.getLineNumber(), figure);

		registerHook();
	}

	private static synchronized void registerHook() {
		if (!hookRegistered) {
			hookRegistered = true;
			Runtime.getRuntime().addShutdownHook(new Thread(() -> writeAll(RECORDINGS), "figure-recorder"));
		}
	}

	/**
	 * Find the line which a figure belongs to: the first line outside this class, and also further up the stack as long as the lines are in the
	 * same class, or in a class which extends the class of the line before, so that a helper method of a test can record the figure for the line in
	 * the test which calls the helper. The helper method can be in the test class, or in a superclass of the test class (for example an abstract
	 * base class of several tests): in that case the line is the one in the test class.
	 * <p>
	 * The classes are only compared by name if they cannot be loaded.
	 *
	 * @param stack a stack trace, the most recent call first
	 * @return the stack frame, or null if there is none
	 */
	static StackTraceElement getCaller(StackTraceElement[] stack) {
		String self = FigureRecorder.class.getName();

		int index = 0;
		while (index < stack.length && stack[index].getClassName().equals(self)) {
			index++;
		}
		if (index == stack.length) {
			return null;
		}
		while (index + 1 < stack.length && isCalledBy(stack[index].getClassName(), stack[index + 1].getClassName())) {
			index++;
		}
		return stack[index];
	}

	/**
	 * @param className the class of a method, for example {@code com.example.AbstractTest} or {@code com.example.MyTest$Nested}
	 * @param callerClassName the class of the method which called it
	 * @return true if the classes are the same (or nested in the same class), or the caller is a subclass of the class: the caller is a test which
	 *         uses a helper method of its superclass
	 */
	static boolean isCalledBy(String className, String callerClassName) {
		String outer = getOuterClassName(className);
		String callerOuter = getOuterClassName(callerClassName);
		if (outer.equals(callerOuter)) {
			return true;
		}
		Class<?> type = load(outer);
		Class<?> callerType = load(callerOuter);
		return type != null && callerType != null && type.isAssignableFrom(callerType);
	}

	private static Class<?> load(String className) {
		ClassLoader[] loaders = { Thread.currentThread().getContextClassLoader(), FigureRecorder.class.getClassLoader() };
		for (ClassLoader loader : loaders) {
			if (loader == null) {
				continue;
			}
			try {
				return Class.forName(className, false, loader);
			} catch (ClassNotFoundException | LinkageError e) {
				// try the next
			}
		}
		return null;
	}

	/**
	 * @return the name of the class, or of the class which it is nested in
	 */
	static String getOuterClassName(String className) {
		int index = className.indexOf('$', className.lastIndexOf('.') + 1);
		return index == -1 ? className : className.substring(0, index);
	}

	/**
	 * @param className the class name, for example {@code com.example.MyTest} or {@code com.example.MyTest$Nested}
	 * @return {@code src/test/java/com/example/MyTest.java} (relative to the working directory), or null for no class
	 */
	static Path getSourceFile(String className) {
		String outer = getOuterClassName(className);
		int index = outer.lastIndexOf('.');
		String name = outer.substring(index + 1);
		if (name.isEmpty()) {
			return null;
		}
		Path directory = Path.of(SOURCE_DIRECTORY);
		if (index != -1) {
			directory = directory.resolve(outer.substring(0, index).replace('.', '/'));
		}
		return directory.resolve(name + ".java");
	}

	/**
	 * The figures which have been recorded.
	 */
	static final class Recordings {

		private final Map<Path, SortedMap<Integer, Figure>> figures = new LinkedHashMap<>();

		/**
		 * @param file the source file
		 * @param line the line of the call, from 1
		 * @param figure the figure
		 * @return true if the figure was added, false if there already is a figure for the line (which is kept)
		 */
		synchronized boolean add(Path file, int line, Figure figure) {
			SortedMap<Integer, Figure> lines = figures.computeIfAbsent(file.toAbsolutePath().normalize(), key -> new TreeMap<>());
			return lines.putIfAbsent(line, figure) == null;
		}

		synchronized int size() {
			int count = 0;
			for (SortedMap<Integer, Figure> lines : figures.values()) {
				count += lines.size();
			}
			return count;
		}

		synchronized Map<Path, SortedMap<Integer, Figure>> getFigures() {
			Map<Path, SortedMap<Integer, Figure>> copy = new LinkedHashMap<>();
			for (Map.Entry<Path, SortedMap<Integer, Figure>> entry : figures.entrySet()) {
				copy.put(entry.getKey(), new TreeMap<>(entry.getValue()));
			}
			return copy;
		}
	}

	/**
	 * Number of figures which are waiting to be written at shutdown.
	 */
	static int getRecordedCount() {
		return RECORDINGS.size();
	}

	private static void writeAll(Recordings recordings) {
		for (Map.Entry<Path, SortedMap<Integer, Figure>> entry : recordings.getFigures().entrySet()) {
			try {
				write(entry.getKey(), entry.getValue());
			} catch (IOException | RuntimeException e) {
				System.err.println("Unable to record figures in " + entry.getKey() + ": " + e);
			}
		}
	}

	/**
	 * Write figures into a source file. The file is only changed if the figures are not already there. The line separator is kept.
	 *
	 * @param file the source file
	 * @param figures the figure for each line of a call, from 1
	 * @return true if the file was changed
	 */
	static boolean write(Path file, SortedMap<Integer, Figure> figures) throws IOException {
		String content = Files.readString(file, StandardCharsets.UTF_8);

		String separator = content.contains("\r\n") ? "\r\n" : "\n";
		boolean trailing = content.endsWith("\n");

		List<String> lines = new ArrayList<>();
		if (!content.isEmpty()) {
			for (String line : content.split("\r?\n", -1)) {
				lines.add(line);
			}
			if (trailing) {
				// after the last line separator
				lines.remove(lines.size() - 1);
			}
		}

		List<String> updated = replaceBlocks(lines, figures);

		String result = String.join(separator, updated) + (trailing ? separator : "");
		if (result.equals(content)) {
			return false;
		}
		Files.writeString(file, result, StandardCharsets.UTF_8);
		return true;
	}

	/**
	 * Replace or insert the blocks of several figures, from the bottom up so that the line numbers of the other calls stay valid.
	 *
	 * @param lines the lines of a source file
	 * @param figures the figure for each line of a call, from 1
	 * @return the lines with the figures
	 * @see #replaceBlock(List, int, Figure)
	 */
	static List<String> replaceBlocks(List<String> lines, SortedMap<Integer, Figure> figures) {
		List<String> result = lines;
		for (Map.Entry<Integer, Figure> entry : new TreeMap<>(figures).descendingMap().entrySet()) {
			result = replaceBlock(result, entry.getKey(), entry.getValue());
		}
		return result;
	}

	/**
	 * Put a figure in a block of comments above a line: the block of comment lines directly above the line which starts with
	 * <code>// &lt;figure&gt;</code> and ends with <code>// &lt;/figure&gt;</code> is replaced, or the block is inserted if there is none.
	 * The block is indented as the line, and no line ends with whitespace.
	 *
	 * @param lines the lines of a source file
	 * @param callLine the line of the call, from 1
	 * @param figure the figure
	 * @return a new list of lines
	 */
	static List<String> replaceBlock(List<String> lines, int callLine, Figure figure) {
		if (callLine < 1 || callLine > lines.size()) {
			throw new IllegalArgumentException("Expected line 1-" + lines.size() + ", got " + callLine);
		}
		int call = callLine - 1;
		String indent = getIndent(lines.get(call));

		// the first line of the block which is replaced
		int start = call;
		if (call > 0 && lines.get(call - 1).trim().equals(BLOCK_END)) {
			for (int i = call - 2; i >= 0; i--) {
				String line = lines.get(i).trim();
				if (line.equals(BLOCK_START)) {
					start = i;
					break;
				}
				if (!line.startsWith("//") || line.equals(BLOCK_END)) {
					break;
				}
			}
		}

		List<String> result = new ArrayList<>(lines.size() + figure.getHeight() + 2);
		result.addAll(lines.subList(0, start));
		result.add(indent + BLOCK_START);
		for (String line : figure.getLines()) {
			result.add(trimEnd(indent + "// " + line));
		}
		result.add(indent + BLOCK_END);
		result.addAll(lines.subList(call, lines.size()));
		return result;
	}

	private static String getIndent(String line) {
		int end = 0;
		while (end < line.length() && (line.charAt(end) == ' ' || line.charAt(end) == '\t')) {
			end++;
		}
		return line.substring(0, end);
	}

	private static String trimEnd(String line) {
		int end = line.length();
		while (end > 0 && Character.isWhitespace(line.charAt(end - 1))) {
			end--;
		}
		return line.substring(0, end);
	}
}
