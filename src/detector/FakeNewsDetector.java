package detector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Combines the Python AI classifier with lightweight keyword heuristics for fake news.
 */
public class FakeNewsDetector {

    private static final String[] FAKE_KEYWORDS = {
            "shocking",
            "you won't believe",
            "miracle",
            "conspiracy",
            "secret",
            "exposed",
            "100% true",
            "doctors hate",
            "cure",
            "instant",
            "viral",
            "click here",
            "banned"
    };

    /**
     * Verdict, adjusted confidence, and red-flag keyword hits from the hybrid step.
     */
    public static final class Result {
        public final String verdict;
        public final int confidencePercent;
        /** How many red-flag keyword phrases were found (each phrase counts at most once). */
        public final int keywordMatchCount;
        /** Keywords matched, in the order defined above. */
        public final List<String> matchedKeywords;

        public Result(String verdict, int confidencePercent, int keywordMatchCount,
                List<String> matchedKeywords) {
            this.verdict = verdict;
            this.confidencePercent = confidencePercent;
            this.keywordMatchCount = keywordMatchCount;
            this.matchedKeywords = Collections.unmodifiableList(new ArrayList<>(matchedKeywords));
        }

        /** Shortcut for errors or callers that do not need keyword metadata. */
        public Result(String verdict, int confidencePercent) {
            this(verdict, confidencePercent, 0, List.of());
        }
    }

    public Result analyze(String newsText) {
        if (newsText == null || newsText.trim().isEmpty()) {
            return new Result("Real", 0);
        }

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", "ai_detector.py", newsText);
            pb.redirectErrorStream(true);
            pb.directory(new File(System.getProperty("user.dir")));
            process = pb.start();

            String fullOutput;
            try (BufferedReader outputReader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = outputReader.readLine()) != null) {
                    if (sb.length() > 0) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                fullOutput = sb.toString();
            }

            process.waitFor();

            System.out.println("[Detector] Python output: " + fullOutput);

            String[] lines = fullOutput.split("\\R");
            for (int i = lines.length - 1; i >= 0; i--) {
                String line = lines[i].trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split("\\s+");
                String token = parts[0];
                if (token.equalsIgnoreCase("Real") || token.equalsIgnoreCase("Fake")) {
                    String verdict =
                            token.substring(0, 1).toUpperCase() + token.substring(1).toLowerCase();
                    int confidencePercent = 0;
                    if (parts.length >= 2) {
                        try {
                            confidencePercent = (int) Math.round(Double.parseDouble(parts[1]) * 100);
                        } catch (NumberFormatException ignored) {
                            confidencePercent = 0;
                        }
                    }
                    return applyHybrid(newsText, verdict, confidencePercent);
                }
            }
            return new Result("Error", 0);
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            exception.printStackTrace();
            return new Result("Error", 0);
        } finally {
            if (process != null) {
                try {
                    process.getOutputStream().close();
                } catch (IOException ignored) {
                    // Best-effort cleanup
                }
                try {
                    process.getInputStream().close();
                } catch (IOException ignored) {
                    // Best-effort cleanup
                }
                try {
                    process.getErrorStream().close();
                } catch (IOException ignored) {
                    // Best-effort cleanup
                }
                process.destroy();
            }
        }
    }

    /**
     * Counts distinct keywords present in the lower-cased input, then adjusts AI verdict/confidence.
     */
    private Result applyHybrid(String newsText, String aiVerdict, int aiConfidence) {
        if ("Error".equalsIgnoreCase(aiVerdict)) {
            return new Result("Error", 0);
        }

        String lower = newsText.toLowerCase();
        List<String> matched = new ArrayList<>();
        for (String kw : FAKE_KEYWORDS) {
            if (lower.contains(kw)) {
                matched.add(kw);
            }
        }
        int keywordCount = matched.size();

        String verdict = aiVerdict;
        int confidence = aiConfidence;

        if ("Real".equalsIgnoreCase(verdict) && keywordCount >= 5) {
            verdict = "Fake";
            confidence = 85;
            return new Result(verdict, confidence, keywordCount, matched);
        }

        if ("Fake".equalsIgnoreCase(verdict) && keywordCount >= 3) {
            confidence = Math.min(100, (int) Math.round(confidence * 1.5));
        }

        return new Result(verdict, confidence, keywordCount, matched);
    }
}
