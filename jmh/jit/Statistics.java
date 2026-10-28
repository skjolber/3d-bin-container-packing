import org.openjdk.jmh.util.ListStatistics;

/** Uses JMH's own sample statistics for a merged set of iteration measurements. */
public final class Statistics {
	public static void main(String[] args) {
		double confidence = Double.parseDouble(args[0]);
		ListStatistics stats = new ListStatistics();
		for(int i = 1; i < args.length; i++) stats.addValue(Double.parseDouble(args[i]));
		if(stats.getN() == 0) throw new IllegalArgumentException("at least one raw JMH sample is required");
		double[] ci = stats.getConfidenceIntervalAt(confidence);
		System.out.printf(java.util.Locale.ROOT, "{\"n\":%d,\"mean\":%.12g,\"stdev\":%.12g,\"min\":%.12g,\"max\":%.12g,\"ci\":[%.12g,%.12g],\"error\":%.12g,\"p50\":%.12g,\"p90\":%.12g,\"p99\":%.12g}%n",
				stats.getN(), stats.getMean(), stats.getStandardDeviation(), stats.getMin(), stats.getMax(), ci[0], ci[1],
				stats.getMeanErrorAt(confidence), stats.getPercentile(50), stats.getPercentile(90), stats.getPercentile(99));
	}
}
