package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * JDBC access for storing and retrieving news analysis rows (MySQL).
 */
public class NewsDAO {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/fake_news_db";
    private static final String DB_USER = "root";
    private static final String PASS = "";

    private static final double SIMILARITY_THRESHOLD = 0.85;

    private Connection connection;

    public NewsDAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, PASS);
            createTableIfNotExists();
        } catch (ClassNotFoundException e) {
            System.err.println("[NewsDAO] MySQL driver not found. Add mysql-connector-j.jar to lib/");
        } catch (SQLException e) {
            System.err.println("[NewsDAO] Connection failed: " + e.getMessage());
        }
    }

    /**
     * @return {@code true} if JDBC reports an open, valid connection; {@code false} if MySQL is
     *         unreachable or the driver failed to load.
     */
    public boolean isDatabaseAvailable() {
        if (connection == null) {
            return false;
        }
        try {
            if (connection.isClosed()) {
                return false;
            }
            return connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private void createTableIfNotExists() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS news_analysis (" +
                "    id          INT AUTO_INCREMENT PRIMARY KEY," +
                "    news_text   TEXT        NOT NULL," +
                "    verdict     VARCHAR(10) NOT NULL," +
                "    score       INT         NOT NULL," +
                "    from_cache  BOOLEAN     DEFAULT FALSE," +
                "    analyzed_at TIMESTAMP   DEFAULT CURRENT_TIMESTAMP" +
                ");";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Optional cache lookup before running the detector (similarity over stored texts).
     *
     * @return a cached row if similarity is above threshold, otherwise {@code null}
     */
    public CachedResult checkCache(String newsText) {
        if (connection == null) {
            return null;
        }

        String sql = "SELECT news_text, verdict, score FROM news_analysis ORDER BY analyzed_at DESC LIMIT 200";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String savedText = rs.getString("news_text");
                double similarity = computeSimilarity(newsText, savedText);

                if (similarity >= SIMILARITY_THRESHOLD) {
                    return new CachedResult(
                            rs.getString("verdict"),
                            rs.getInt("score"),
                            similarity);
                }
            }

        } catch (SQLException e) {
            System.err.println("[NewsDAO] Cache lookup failed: " + e.getMessage());
        }

        return null;
    }

    public void save(String newsText, String verdict, int score) {
        if (connection == null) {
            return;
        }

        String sql = "INSERT INTO news_analysis (news_text, verdict, score, from_cache) VALUES (?, ?, ?, FALSE)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newsText);
            ps.setString(2, verdict);
            ps.setInt(3, score);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[NewsDAO] Save failed: " + e.getMessage());
        }
    }

    /**
     * Recent analyses for the History dialog. Returns an empty list when the DB is unavailable
     * or on query failure (never {@code null}).
     */
    public List<NewsRecord> getHistory() {
        List<NewsRecord> records = new ArrayList<>();
        if (connection == null) {
            return records;
        }

        String sql = "SELECT id, news_text, verdict, score, from_cache, analyzed_at "
                + "FROM news_analysis ORDER BY analyzed_at DESC LIMIT 50";

        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(new NewsRecord(
                        rs.getInt("id"),
                        rs.getString("news_text"),
                        rs.getString("verdict"),
                        rs.getInt("score"),
                        rs.getBoolean("from_cache"),
                        rs.getTimestamp("analyzed_at").toString()));
            }

        } catch (SQLException e) {
            System.err.println("[NewsDAO] Fetch history failed: " + e.getMessage());
        }

        return records;
    }

    private double computeSimilarity(String text1, String text2) {
        Set<String> set1 = tokenize(text1);
        Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private Set<String> tokenize(String text) {
        Set<String> words = new HashSet<>();
        for (String word : text.toLowerCase().split("\\W+")) {
            if (word.length() > 2) {
                words.add(word);
            }
        }
        return words;
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("[NewsDAO] Close failed: " + e.getMessage());
        }
    }

    public static class CachedResult {
        public final String verdict;
        public final int score;
        public final double similarity;

        public CachedResult(String verdict, int score, double similarity) {
            this.verdict = verdict;
            this.score = score;
            this.similarity = similarity;
        }
    }

    public static class NewsRecord {
        public final int id;
        public final String newsText;
        public final String verdict;
        public final int score;
        public final boolean fromCache;
        public final String analyzedAt;

        public NewsRecord(int id, String newsText, String verdict, int score, boolean fromCache,
                String analyzedAt) {
            this.id = id;
            this.newsText = newsText;
            this.verdict = verdict;
            this.score = score;
            this.fromCache = fromCache;
            this.analyzedAt = analyzedAt;
        }
    }
}
