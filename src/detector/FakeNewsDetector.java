package detector;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Backend detector for AI-generated (fake) news content.
 *
 * <p>This class delegates classification to the external Python script
 * {@code ai_detector.py} located in the project root directory. The script is expected
 * to use a Hugging Face model (for example, {@code roberta-base-openai-detector})
 * and print a final line with label and optional confidence, e.g. {@code Fake 0.9873}.</p>
 *
 * <p><strong>Setup (one-time):</strong></p>
 * <ol>
 *     <li>Install Python 3 and ensure the {@code python} command is available in PATH.</li>
 *     <li>Install required Python dependencies:
 *         <pre>pip install transformers torch</pre>
 *     </li>
 *     <li>Place {@code ai_detector.py} in the project root.</li>
 * </ol>
 */
public class FakeNewsDetector {

    /**
     * Verdict plus model confidence as a percentage (0–100).
     */
    public static final class Result {
        /** {@code Real}, {@code Fake}, or {@code Error}. */
        public final String verdict;
        /** Confidence 0–100; {@code 0} when unknown or on error. */
        public final int confidencePercent;

        public Result(String verdict, int confidencePercent) {
            this.verdict = verdict;
            this.confidencePercent = confidencePercent;
        }
    }

    /**
     * Analyzes input news text by invoking the external Python AI detector.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>Returns {@code Real} with {@code 0} confidence when input is {@code null} or blank.</li>
     *     <li>Runs {@code python ai_detector.py &lt;newsText&gt;} via {@link ProcessBuilder}.</li>
     *     <li>Parses the last output line of the form {@code Real} / {@code Fake} with optional score.</li>
     *     <li>Returns {@code Error} if output is empty, unrecognized, or if any exception occurs.</li>
     * </ul>
     *
     * @param newsText full news article text to classify
     * @return verdict and confidence; use {@code new Result("Error", 0)} on failure
     */
    public Result analyze(String newsText) {
        if (newsText == null || newsText.trim().isEmpty()) {
            return new Result("Real", 0);
        }

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/python3", "ai_detector.py", newsText);
            pb.redirectErrorStream(true);
            // Run with project root as cwd so `ai_detector.py` resolves when starting via run.sh / IDE.
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
                    return new Result(verdict, confidencePercent);
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
}
