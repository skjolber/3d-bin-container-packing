import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedFrame;
import jdk.jfr.consumer.RecordedMethod;
import jdk.jfr.consumer.RecordedObject;
import jdk.jfr.consumer.RecordingFile;

/** Summarizes HotSpot JIT and execution data from an existing JFR recording. */
public final class JitSummary {
	private static final String PACKAGE = "com.github.skjolber.packing";
	private static final String HARNESS = PACKAGE + ".jmh";

	private JitSummary() {
	}

	public static void main(String[] args) throws Exception {
		if(args.length != 1) {
			System.err.println("Usage: java jmh/jit/JitSummary.java <recording.jfr>");
			System.exit(2);
		}
		Map<String, Long> compilations = new TreeMap<>();
		Map<String, Long> failedInlining = new TreeMap<>();
		Map<String, Long> deoptimizations = new TreeMap<>();
		Map<String, Long> allocations = new TreeMap<>();
		Map<String, Long> inclusive = new TreeMap<>();
		Map<String, Long> leaf = new TreeMap<>();
		Set<Integer> c2CompileIds = new java.util.HashSet<>();
		try(RecordingFile recording = new RecordingFile(Path.of(args[0]))) {
			while(recording.hasMoreEvents()) {
				RecordedEvent event = recording.readEvent();
				if(event.getEventType().getName().equals("jdk.Compilation") && event.getInt("compileLevel") == 4 && isLibrary(event.getValue("method"))) {
					c2CompileIds.add(event.getInt("compileId"));
				}
			}
		}
		long executionSamples = 0;
		long inclusiveSamples = 0;
		try(RecordingFile recording = new RecordingFile(Path.of(args[0]))) {
			while(recording.hasMoreEvents()) {
				RecordedEvent event = recording.readEvent();
				switch(event.getEventType().getName()) {
				case "jdk.Compilation":
					RecordedMethod method = event.getValue("method");
					if(event.getInt("compileLevel") == 4 && isLibrary(method)) {
						String key = name(method) + String.format("  code %d B, inlined %d B, tier 4, %s", event.getLong("codeSize"),
								event.getLong("inlinedBytes"), event.getBoolean("isOsr") ? "OSR" : "normal");
						compilations.merge(key, 1L, Long::sum);
					}
					break;
				case "jdk.CompilerInlining":
					if(!event.getBoolean("succeeded") && c2CompileIds.contains(event.getInt("compileId"))) {
						RecordedMethod caller = event.getValue("caller");
						RecordedObject target = event.getValue("callee");
						String callee = target.getString("type").replace('/', '.') + "." + target.getString("name");
						failedInlining.merge(name(caller) + " -> " + callee + ": " + event.getString("message"), 1L, Long::sum);
					}
					break;
				case "jdk.Deoptimization":
					method = event.getValue("method");
					if(isLibrary(method)) {
						deoptimizations.merge(name(method) + " bci " + event.getInt("bci") + ": " + event.getString("reason") + ", " + event.getString("action"), 1L, Long::sum);
					}
					break;
				case "jdk.ObjectAllocationSample":
					if(event.getStackTrace() != null) {
						String site = firstLibraryFrame(event);
						if(site != null) {
							allocations.merge(event.getClass("objectClass").getName() + " at " + site, event.getLong("weight"), Long::sum);
						}
					}
					break;
				case "jdk.ExecutionSample":
					if(event.getStackTrace() != null && !event.getStackTrace().getFrames().isEmpty()) {
						executionSamples++;
						List<RecordedFrame> frames = event.getStackTrace().getFrames();
						leaf.merge(frameName(frames.get(0)), 1L, Long::sum);
						Set<String> seen = new java.util.HashSet<>();
						for(RecordedFrame frame : frames) {
							if(isLibrary(frame.getMethod())) {
								String methodKey = name(frame.getMethod());
								if(seen.add(methodKey)) inclusive.merge(methodKey, 1L, Long::sum);
							}
						}
						if(!seen.isEmpty()) inclusiveSamples++;
					}
					break;
				default:
					break;
				}
			}
		}
		print("Library C2 compilations (size, tier, OSR; count)", compilations);
		print("Failed inlining (caller -> callee: reason; count)", failedInlining);
		print("Deoptimizations (method, bci, reason, action; count)", deoptimizations);
		print("Sampled allocations (weighted bytes; allocated class and innermost library frame)", allocations);
		System.out.println("\nInclusive execution samples (unique library methods per sample): " + inclusiveSamples);
		print("Inclusive library methods", inclusive);
		System.out.println("\nLeaf execution samples: " + executionSamples);
		print("Leaf frames", leaf);
	}

	private static void print(String title, Map<String, Long> values) {
		System.out.println("\n## " + title);
		List<Map.Entry<String, Long>> entries = new ArrayList<>(values.entrySet());
		entries.sort(Map.Entry.comparingByKey());
		for(Map.Entry<String, Long> entry : entries) {
			System.out.println(entry.getValue() + "  " + entry.getKey());
		}
	}

	private static String firstLibraryFrame(RecordedEvent event) {
		for(RecordedFrame frame : event.getStackTrace().getFrames()) {
			if(isLibrary(frame.getMethod())) {
				return frameName(frame);
			}
		}
		return null;
	}

	private static String frameName(RecordedFrame frame) {
		return name(frame.getMethod()) + ":" + frame.getLineNumber();
	}

	private static boolean isLibrary(RecordedMethod method) {
		if(method == null) return false;
		String type = method.getType().getName();
		return type.startsWith(PACKAGE) && !type.startsWith(HARNESS);
	}

	private static String name(RecordedMethod method) {
		return method.getType().getName() + "." + method.getName() + method.getDescriptor();
	}
}
