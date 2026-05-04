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
 * and print exactly one label to standard output: {@code Real} or {@code Fake}.</p>
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
     * Analyzes input news text by invoking the external Python AI detector.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>Returns {@code Real} when input is {@code null} or blank.</li>
     *     <li>Runs {@code python ai_detector.py &lt;newsText&gt;} via {@link ProcessBuilder}.</li>
     *     <li>Reads the first output line from the script and maps:
     *         {@code Real -> Real}, {@code Fake -> Fake}.</li>
     *     <li>Returns {@code Error} if output is empty, unrecognized, or if any exception occurs.</li>
     * </ul>
     *
     * @param newsText full news article text to classify
     * @return {@code Real}, {@code Fake}, or {@code Error} based on script result/execution status
     */
    public String analyze(String newsText) {
        if (newsText == null || newsText.trim().isEmpty()) {
            return "Real";
        }

        Process process = null;
        try {
            ProcessBuilder pb = new ProcessBuilder("python3", "ai_detector.py", newsText);
            pb.redirectErrorStream(true);
            // Run with project root as cwd so `ai_detector.py` resolves when starting via run.sh / IDE.
            pb.directory(new File(System.getProperty("user.dir")));
            process = pb.start();

            String scriptOutput;
            try (BufferedReader outputReader =
                         new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                scriptOutput = outputReader.readLine();
            }

            process.waitFor();

            if (scriptOutput == null) {
                return "Error";
            }

            String normalizedOutput = scriptOutput.trim();
            if (normalizedOutput.isEmpty()) {
                return "Error";
            }
            if ("Real".equalsIgnoreCase(normalizedOutput)) {
                return "Real";
            }
            if ("Fake".equalsIgnoreCase(normalizedOutput)) {
                return "Fake";
            }

            return "Error";
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            exception.printStackTrace();
            return "Error";
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