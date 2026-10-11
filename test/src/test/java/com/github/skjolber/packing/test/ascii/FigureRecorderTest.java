package com.github.skjolber.packing.test.ascii;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class FigureRecorderTest {

	private static final String RECORDER = FigureRecorder.class.getName();

	private static StackTraceElement frame(String className, String method, int line) {
		return new StackTraceElement(className, method, className.substring(className.lastIndexOf('.') + 1) + ".java", line);
	}

	@Test
	public void testInsertBlockAboveTheLine() {
		List<String> lines = List.of(
				"public class MyTest {",
				"\t@Test",
				"\tpublic void test() {",
				"\t\tint a = 1;",
				"\t\tfigure(a);",
				"\t}",
				"}");

		List<String> result = FigureRecorder.replaceBlock(lines, 5, Figure.of("ab", "", "  c"));

		assertThat(result).containsExactly(
				"public class MyTest {",
				"\t@Test",
				"\tpublic void test() {",
				"\t\tint a = 1;",
				"\t\t// <figure>",
				"\t\t// ab",
				"\t\t//",
				"\t\t//   c",
				"\t\t// </figure>",
				"\t\tfigure(a);",
				"\t}",
				"}");
		// the original lines are not changed
		assertThat(lines).hasSize(7);
	}

	@Test
	public void testInsertBlockWithTheIndentOfTheLine() {
		List<String> lines = List.of("  a();", "    figure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 2, Figure.of("x"));

		assertThat(result).containsExactly(
				"  a();",
				"    // <figure>",
				"    // x",
				"    // </figure>",
				"    figure(a);");
	}

	@Test
	public void testInsertBlockAtTheFirstLine() {
		List<String> result = FigureRecorder.replaceBlock(List.of("figure();"), 1, Figure.of("x"));

		assertThat(result).containsExactly(
				"// <figure>",
				"// x",
				"// </figure>",
				"figure();");
	}

	@Test
	public void testReplaceBlock() {
		List<String> lines = List.of(
				"\tpublic void test() {",
				"\t\t// text",
				"\t\t// <figure>",
				"\t\t// old",
				"\t\t// figure",
				"\t\t// </figure>",
				"\t\tfigure(a);",
				"\t}");

		List<String> result = FigureRecorder.replaceBlock(lines, 7, Figure.of("new"));

		assertThat(result).containsExactly(
				"\tpublic void test() {",
				"\t\t// text",
				"\t\t// <figure>",
				"\t\t// new",
				"\t\t// </figure>",
				"\t\tfigure(a);",
				"\t}");
	}

	@Test
	public void testReplaceBlockWithALongerFigure() {
		List<String> lines = List.of(
				"\t// <figure>",
				"\t// old",
				"\t// </figure>",
				"\tfigure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 4, Figure.of("1", "2", "3"));

		assertThat(result).containsExactly(
				"\t// <figure>",
				"\t// 1",
				"\t// 2",
				"\t// 3",
				"\t// </figure>",
				"\tfigure(a);");
	}

	@Test
	public void testReplaceBlockWithTheSameFigureDoesNotChangeTheLines() {
		List<String> lines = List.of("\ta();", "\tfigure(a);");

		List<String> once = FigureRecorder.replaceBlock(lines, 2, Figure.of("┌─┐", "│A│", "└─┘"));
		List<String> twice = FigureRecorder.replaceBlock(once, 2 + 5, Figure.of("┌─┐", "│A│", "└─┘"));

		assertThat(once).hasSize(2 + 5);
		assertThat(twice).isEqualTo(once);
	}

	@Test
	public void testBlockWhichIsNotDirectlyAboveTheLineIsKept() {
		List<String> lines = List.of(
				"\t// <figure>",
				"\t// old",
				"\t// </figure>",
				"",
				"\tfigure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 5, Figure.of("new"));

		assertThat(result).containsExactly(
				"\t// <figure>",
				"\t// old",
				"\t// </figure>",
				"",
				"\t// <figure>",
				"\t// new",
				"\t// </figure>",
				"\tfigure(a);");
	}

	@Test
	public void testCommentsDirectlyAboveTheLineAreKept() {
		List<String> lines = List.of(
				"\t// explanation",
				"\tfigure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 2, Figure.of("x"));

		assertThat(result).containsExactly(
				"\t// explanation",
				"\t// <figure>",
				"\t// x",
				"\t// </figure>",
				"\tfigure(a);");
	}

	@Test
	public void testBlockWithoutStartIsKept() {
		List<String> lines = List.of(
				"\t// text",
				"\t// </figure>",
				"\tfigure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 3, Figure.of("x"));

		assertThat(result).containsExactly(
				"\t// text",
				"\t// </figure>",
				"\t// <figure>",
				"\t// x",
				"\t// </figure>",
				"\tfigure(a);");
	}

	@Test
	public void testBlockIsNotExtendedIntoAnotherBlock() {
		List<String> lines = List.of(
				"\t// <figure>",
				"\t// first",
				"\t// </figure>",
				"\t// </figure>",
				"\tfigure(a);");

		List<String> result = FigureRecorder.replaceBlock(lines, 5, Figure.of("x"));

		assertThat(result).containsExactly(
				"\t// <figure>",
				"\t// first",
				"\t// </figure>",
				"\t// </figure>",
				"\t// <figure>",
				"\t// x",
				"\t// </figure>",
				"\tfigure(a);");
	}

	@Test
	public void testLinesDoNotEndWithWhitespace() {
		List<String> lines = List.of("\t\tfigure(a);", "  figure(b);");

		for (int line = 1; line <= 2; line++) {
			List<String> result = FigureRecorder.replaceBlock(lines, line, Figure.of("a", "", "  ", "b"));

			for (String text : result) {
				assertThat(text).isEqualTo(text.stripTrailing());
			}
		}
	}

	@Test
	public void testReplaceBlocksFromTheBottomUp() {
		List<String> lines = List.of(
				"\tvoid a() {",
				"\t\tfigure(1);",
				"\t}",
				"\tvoid b() {",
				"\t\t// <figure>",
				"\t\t// old",
				"\t\t// figure",
				"\t\t// </figure>",
				"\t\tfigure(2);",
				"\t}");
		SortedMap<Integer, Figure> figures = new TreeMap<>();
		figures.put(2, Figure.of("one"));
		figures.put(9, Figure.of("two", "lines"));

		List<String> result = FigureRecorder.replaceBlocks(lines, figures);

		assertThat(result).containsExactly(
				"\tvoid a() {",
				"\t\t// <figure>",
				"\t\t// one",
				"\t\t// </figure>",
				"\t\tfigure(1);",
				"\t}",
				"\tvoid b() {",
				"\t\t// <figure>",
				"\t\t// two",
				"\t\t// lines",
				"\t\t// </figure>",
				"\t\tfigure(2);",
				"\t}");
	}

	@Test
	public void testReplaceBlockOutsideTheFile() {
		List<String> lines = List.of("a", "b");

		assertThatThrownBy(() -> FigureRecorder.replaceBlock(lines, 0, Figure.of("x"))).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> FigureRecorder.replaceBlock(lines, 3, Figure.of("x"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testCallerIsTheLineWhichCallsTheRecorder() {
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame("com.example.MyTest", "test", 20),
				frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke", 103),
				frame("org.junit.platform.commons.util.ReflectionUtils", "invokeMethod", 580)
		};

		StackTraceElement caller = FigureRecorder.getCaller(stack);

		assertThat(caller.getClassName()).isEqualTo("com.example.MyTest");
		assertThat(caller.getLineNumber()).isEqualTo(20);
	}

	/**
	 * A helper method of the test, or a lambda, records the figure for the line in the test.
	 */
	@Test
	public void testCallerIsTheLineInTheTestWhenAHelperRecords() {
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame("com.example.MyTest", "figure", 50),
				frame("com.example.MyTest", "lambda$figure$0", 51),
				frame("com.example.MyTest$Helper", "draw", 60),
				frame("com.example.MyTest", "test", 20),
				frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke", 103),
				frame("com.example.MyTest", "other", 7)
		};

		StackTraceElement caller = FigureRecorder.getCaller(stack);

		assertThat(caller.getMethodName()).isEqualTo("test");
		assertThat(caller.getLineNumber()).isEqualTo(20);
	}

	/**
	 * The helper method which records is in a superclass of the test class (for example an abstract base class of several tests): the figure is for
	 * the line in the test class which calls the helper.
	 */
	@Test
	public void testCallerIsTheLineInTheTestWhenTheHelperIsInASuperclass() {
		String base = RecorderBaseClass.class.getName();
		String test = RecorderSubclass.class.getName();
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame(base, "figure", 50),
				frame(base, "lambda$figure$0", 51),
				frame(test, "test", 20),
				frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke", 103),
				frame(test, "other", 7)
		};

		StackTraceElement caller = FigureRecorder.getCaller(stack);

		assertThat(caller.getClassName()).isEqualTo(test);
		assertThat(caller.getMethodName()).isEqualTo("test");
		assertThat(caller.getLineNumber()).isEqualTo(20);
	}

	/**
	 * A helper method of the test class which calls the helper method of the superclass: the line in the test is still the one which counts.
	 */
	@Test
	public void testCallerIsTheLineInTheTestWhenTheHelperOfTheSubclassCallsTheHelperOfTheSuperclass() {
		String base = RecorderBaseClass.class.getName();
		String test = RecorderSubclass.class.getName();
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame(base, "figure", 50),
				frame(test, "draw", 30),
				frame(test, "test", 20),
				frame("jdk.internal.reflect.DirectMethodHandleAccessor", "invoke", 103)
		};

		assertThat(FigureRecorder.getCaller(stack).getMethodName()).isEqualTo("test");
	}

	/**
	 * The caller is not a subclass of the class with the helper method: the line in the helper class is the caller.
	 */
	@Test
	public void testCallerIsTheHelperWhenTheCallerIsNotASubclass() {
		String base = RecorderBaseClass.class.getName();
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame(base, "figure", 50),
				frame(FigureRecorderTest.class.getName(), "test", 20)
		};

		assertThat(FigureRecorder.getCaller(stack).getClassName()).isEqualTo(base);
	}

	/**
	 * With the real stack.
	 */
	@Test
	public void testCallerOfAHelperInASuperclassWithTheRealStack() {
		StackTraceElement caller = new RecorderSubclass().callHelper();

		assertThat(caller.getClassName()).isEqualTo(RecorderSubclass.class.getName());
		assertThat(caller.getMethodName()).isEqualTo("callHelper");
		assertThat(FigureRecorder.getSourceFile(caller.getClassName())).isEqualTo(Path.of("src/test/java/com/github/skjolber/packing/test/ascii/RecorderSubclass.java"));
	}

	@Test
	public void testIsCalledBy() {
		String base = RecorderBaseClass.class.getName();
		String sub = RecorderSubclass.class.getName();

		// the same class, or nested in the same class
		assertThat(FigureRecorder.isCalledBy("com.example.MyTest", "com.example.MyTest")).isTrue();
		assertThat(FigureRecorder.isCalledBy("com.example.MyTest", "com.example.MyTest$Nested")).isTrue();
		assertThat(FigureRecorder.isCalledBy("com.example.MyTest$Nested", "com.example.MyTest")).isTrue();
		// a subclass calls a method of its superclass, but not the other way around
		assertThat(FigureRecorder.isCalledBy(base, sub)).isTrue();
		assertThat(FigureRecorder.isCalledBy(base + "$Inner", sub)).isTrue();
		assertThat(FigureRecorder.isCalledBy(sub, base)).isFalse();
		// unrelated classes, also those which cannot be loaded
		assertThat(FigureRecorder.isCalledBy(base, FigureRecorderTest.class.getName())).isFalse();
		assertThat(FigureRecorder.isCalledBy("com.example.MyTest", sub)).isFalse();
		assertThat(FigureRecorder.isCalledBy(base, "com.example.MyTest")).isFalse();
	}

	@Test
	public void testCallerSkipsThePackagerResultHelper() {
		StackTraceElement[] stack = {
				frame(RECORDER, "record", 80),
				frame(PackagerResultFigures.class.getName(), "figure", 60),
				frame("com.example.MyTest", "test", 12),
				frame("org.junit.Runner", "run", 1)
		};
		StackTraceElement caller = FigureRecorder.getCaller(stack);
		assertThat(caller.getClassName()).isEqualTo("com.example.MyTest");
		assertThat(caller.getLineNumber()).isEqualTo(12);
	}

	@Test
	public void testCallerIsNullWithoutACaller() {
		assertThat(FigureRecorder.getCaller(new StackTraceElement[0])).isNull();
		assertThat(FigureRecorder.getCaller(new StackTraceElement[] { frame(RECORDER, "record", 80) })).isNull();
	}

	@Test
	public void testOuterClassName() {
		assertThat(FigureRecorder.getOuterClassName("com.example.MyTest")).isEqualTo("com.example.MyTest");
		assertThat(FigureRecorder.getOuterClassName("com.example.MyTest$Inner$Deeper")).isEqualTo("com.example.MyTest");
		assertThat(FigureRecorder.getOuterClassName("MyTest$Inner")).isEqualTo("MyTest");
		assertThat(FigureRecorder.getOuterClassName("MyTest")).isEqualTo("MyTest");
	}

	@Test
	public void testSourceFileIsInTheTestSourcesOfTheModule() {
		assertThat(FigureRecorder.getSourceFile("com.example.points.MyTest")).isEqualTo(Path.of("src/test/java/com/example/points/MyTest.java"));
		assertThat(FigureRecorder.getSourceFile("com.example.points.MyTest$Inner")).isEqualTo(Path.of("src/test/java/com/example/points/MyTest.java"));
		assertThat(FigureRecorder.getSourceFile("MyTest")).isEqualTo(Path.of("src/test/java/MyTest.java"));
	}

	@Test
	public void testRecordingsKeepTheFirstFigureOfALine() {
		FigureRecorder.Recordings recordings = new FigureRecorder.Recordings();
		Path file = Path.of("src/test/java/MyTest.java");

		assertThat(recordings.add(file, 10, Figure.of("first"))).isTrue();
		assertThat(recordings.add(file, 10, Figure.of("second"))).isFalse();
		assertThat(recordings.add(file, 5, Figure.of("other line"))).isTrue();
		assertThat(recordings.add(Path.of("src/test/java/Other.java"), 10, Figure.of("other file"))).isTrue();

		assertThat(recordings.size()).isEqualTo(3);

		Map<Path, SortedMap<Integer, Figure>> figures = recordings.getFigures();
		SortedMap<Integer, Figure> lines = figures.get(file.toAbsolutePath().normalize());
		assertThat(lines.keySet()).containsExactly(5, 10);
		assertThat(lines.get(10).getLines()).containsExactly("first");
	}

	@Test
	public void testRecordDoesNothingWithoutTheProperty() {
		Assumptions.assumeFalse(Boolean.getBoolean(FigureRecorder.PROPERTY));

		FigureRecorder.record(Figure.of("x"));

		assertThat(FigureRecorder.getRecordedCount()).isZero();
	}

	@Test
	public void testWriteFigureIntoFile(@TempDir Path directory) throws IOException {
		Path file = directory.resolve("MyTest.java");
		Files.writeString(file, "class MyTest {\n\tvoid test() {\n\t\tfigure();\n\t}\n}\n", StandardCharsets.UTF_8);
		SortedMap<Integer, Figure> figures = new TreeMap<>();
		figures.put(3, Figure.of("┌─┐", "│A│", "└─┘"));

		assertThat(FigureRecorder.write(file, figures)).isTrue();

		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
				class MyTest {
					void test() {
						// <figure>
						// ┌─┐
						// │A│
						// └─┘
						// </figure>
						figure();
					}
				}
				""");

		// the same figure again, now the call is at line 8
		figures.clear();
		figures.put(8, Figure.of("┌─┐", "│A│", "└─┘"));
		assertThat(FigureRecorder.write(file, figures)).isFalse();

		figures.put(8, Figure.of("B"));
		assertThat(FigureRecorder.write(file, figures)).isTrue();
		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("""
				class MyTest {
					void test() {
						// <figure>
						// B
						// </figure>
						figure();
					}
				}
				""");
	}

	@Test
	public void testWriteKeepsTheLineSeparatorAndNoFinalLineSeparator(@TempDir Path directory) throws IOException {
		Path file = directory.resolve("MyTest.java");
		Files.writeString(file, "a\r\nfigure();\r\nb", StandardCharsets.UTF_8);
		SortedMap<Integer, Figure> figures = new TreeMap<>();
		figures.put(2, Figure.of("x"));

		assertThat(FigureRecorder.write(file, figures)).isTrue();

		assertThat(Files.readString(file, StandardCharsets.UTF_8)).isEqualTo("a\r\n// <figure>\r\n// x\r\n// </figure>\r\nfigure();\r\nb");
	}
}
