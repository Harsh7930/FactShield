import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
/**
 * A simple keyword-based fake news detector.
 * This class analyzes a news article and returns "Real" or "Fake"
 * based on the presence of suspicious words/phrases.
 */
public class FakeNewsDetector {

    // Set of keywords commonly found in fake/clickbait news
    private static final Set<String> FAKE_KEYWORDS = new HashSet<>(Arrays.asList(
        "shocking", "you won't believe", "miracle", "conspiracy",
        "hoax", "secret", "exposed", "100% true", "they don't want you to know",
        "cure", "instant", "magic", "revealed", "unbelievable",
        "what happened next", "click here", "viral", "must see",
        "banned", "doctors hate this", "weird trick"
    ));

    // Additional patterns: excessive exclamation marks, ALL CAPS sentences
    private static final int EXCLAMATION_THRESHOLD = 3;
    private static final int ALL_CAPS_WORD_THRESHOLD = 5;

    /**
     * Analyzes the given news text and returns a classification.
     *
     * @param newsText the full text of the news article
     * @return "Fake" if suspicious content is detected, otherwise "Real"
     */
    public String analyze(String newsText) {
        if (newsText == null || newsText.trim().isEmpty()) {
            return "Real";  // empty input → treat as real (or you can return "Invalid")
        }

        String lowerText = newsText.toLowerCase();

        // 1. Check for fake keywords
        for (String keyword : FAKE_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return "Fake";
            }
        }

        // 2. Check for excessive exclamation marks
        long exclamationCount = lowerText.chars().filter(ch -> ch == '!').count();
        if (exclamationCount >= EXCLAMATION_THRESHOLD) {
            return "Fake";
        }

        // 3. Check for many ALL CAPS words (often used for sensationalism)
        String[] words = newsText.split("\\s+");
        long allCapsCount = Arrays.stream(words)
                .filter(word -> word.length() > 2 && word.equals(word.toUpperCase()))
                .count();
        if (allCapsCount >= ALL_CAPS_WORD_THRESHOLD) {
            return "Fake";
        }

        // No suspicious signs found
        return "Real";
    }
}