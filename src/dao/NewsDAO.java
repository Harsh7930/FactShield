package dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * NewsDAO - Database Access Object for FactShield
 *
 * MEMBER WORK: Database Layer
 *
 * SMART CACHE LOGIC:
 *   1. Before running analysis, check if this (or very similar) text was analyzed before.
 *   2. If found in DB → return saved result instantly (cache hit).
 *   3. If not found   → let FakeNewsDetector run → save result to DB for future use.
 */
public class NewsDAO {

    // ============================================================
    // DATABASE CONFIG — change these to match your MySQL setup
    // ============================================================
    private static final String DB_URL  = "jdbc:mysql://localhost:3306/factshield_db";
    private static final String DB_USER = "root";
    private static final String PASS = "";

    // How similar two texts must be (0.0 to 1.0) to count as a cache hit
    private static final double SIMILARITY_THRESHOLD = 0.85;

    private Connection connection;

    // ============================================================
    // CONSTRUCTOR
    // ============================================================
    public NewsDAO() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, PASS);
            createTableIfNotExists();
            System.out.println("[NewsDAO] Connected to MySQL successfully.");
        } catch (ClassNotFoundException e) {
            System.err.println("[NewsDAO] MySQL Driver not found. Add mysql-connector-j.jar to /lib");
        } catch (SQLException e) {
            System.err.println("[NewsDAO] Connection failed: " + e.getMessage());
        }
    }

    // ============================================================
    // CREATE TABLE
    // ============================================================
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

    // ============================================================
    // CACHE LOOKUP — call this BEFORE running analysis
    // Returns a CachedResult if a similar article exists in DB,
    // or null if no match found (meaning: run fresh analysis).
    // ============================================================
    public CachedResult checkCache(String newsText) {
        if (connection == null) return null;

        String sql = "SELECT news_text, verdict, score FROM news_analysis ORDER BY analyzed_at DESC LIMIT 200";

        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String savedText = rs.getString("news_text");
                double similarity = computeSimilarity(newsText, savedText);

                if (similarity >= SIMILARITY_THRESHOLD) {
                    System.out.println("[NewsDAO] Cache HIT! Similarity: " + String.format("%.0f%%", similarity * 100));
                    return new CachedResult(
                        rs.getString("verdict"),
                        rs.getInt("score"),
                        similarity
                    );
                }
            }

        } catch (SQLException e) {
            System.err.println("[NewsDAO] Cache lookup failed: " + e.getMessage());
        }

        System.out.println("[NewsDAO] Cache MISS — running fresh analysis.");
        return null;
    }

    // ============================================================
    // SAVE — call this AFTER a fresh analysis (cache miss)
    // ============================================================
    public void save(String newsText, String verdict, int score) {
        if (connection == null) {
            System.err.println("[NewsDAO] No DB connection. Skipping save.");
            return;
        }

        String sql = "INSERT INTO news_analysis (news_text, verdict, score, from_cache) VALUES (?, ?, ?, FALSE)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, newsText);
            ps.setString(2, verdict);
            ps.setInt(3, score);
            ps.executeUpdate();
            System.out.println("[NewsDAO] Saved to DB: verdict=" + verdict + ", score=" + score);
        } catch (SQLException e) {
            System.err.println("[NewsDAO] Save failed: " + e.getMessage());
        }
    }

    // ============================================================
    // FETCH HISTORY — for the History button in GUI
    // ============================================================
    public List<NewsRecord> getHistory() {
        List<NewsRecord> records = new ArrayList<>();
        if (connection == null) return records;

        String sql = "SELECT id, news_text, verdict, score, from_cache, analyzed_at " +
                     "FROM news_analysis ORDER BY analyzed_at DESC LIMIT 50";

        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {

            while (rs.next()) {
                records.add(new NewsRecord(
                    rs.getInt("id"),
                    rs.getString("news_text"),
                    rs.getString("verdict"),
                    rs.getInt("score"),
                    rs.getBoolean("from_cache"),
                    rs.getTimestamp("analyzed_at").toString()
                ));
            }

        } catch (SQLException e) {
            System.err.println("[NewsDAO] Fetch history failed: " + e.getMessage());
        }

        return records;
    }

    // ============================================================
    // SIMILARITY — Jaccard similarity on word sets
    // 0.0 = nothing in common, 1.0 = identical
    // ============================================================
    private double computeSimilarity(String text1, String text2) {
        java.util.Set<String> set1 = tokenize(text1);
        java.util.Set<String> set2 = tokenize(text2);

        if (set1.isEmpty() || set2.isEmpty()) return 0.0;

        java.util.Set<String> intersection = new java.util.HashSet<>(set1);
        intersection.retainAll(set2);

        java.util.Set<String> union = new java.util.HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    private java.util.Set<String> tokenize(String text) {
        java.util.Set<String> words = new java.util.HashSet<>();
        for (String word : text.toLowerCase().split("\\W+")) {
            if (word.length() > 2) words.add(word);
        }
        return words;
    }

    // ============================================================
    // CLOSE CONNECTION
    // ============================================================
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("[NewsDAO] Connection closed.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ============================================================
    // INNER CLASS — result returned from cache
    // ============================================================
    public static class CachedResult {
        public final String verdict;
        public final int    score;
        public final double similarity;

        public CachedResult(String verdict, int score, double similarity) {
            this.verdict    = verdict;
            this.score      = score;
            this.similarity = similarity;
        }
    }

    // ============================================================
    // INNER CLASS — one row from DB (for history view)
    // ============================================================
    public static class NewsRecord {
        public final int     id;
        public final String  newsText;
        public final String  verdict;
        public final int     score;
        public final boolean fromCache;
        public final String  analyzedAt;

        public NewsRecord(int id, String newsText, String verdict, int score, boolean fromCache, String analyzedAt) {
            this.id         = id;
            this.newsText   = newsText;
            this.verdict    = verdict;
            this.score      = score;
            this.fromCache  = fromCache;
            this.analyzedAt = analyzedAt;
        }
    }
}
