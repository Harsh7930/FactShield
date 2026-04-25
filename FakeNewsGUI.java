import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

public class FakeNewsGUI extends JFrame {

    private JTextArea input;
    private JLabel resultLabel, scoreValue, statusLabel;
    private JProgressBar confidenceBar;
    private JPanel detailPanel;
    private boolean darkMode = false;

    // =========================
    // MEMBER 2 (Logic Layer)
    // =========================
    private FakeNewsDetector detector = new FakeNewsDetector();

    // =========================
    // MEMBER 3 (Database Layer)
    // =========================
    private NewsDAO dao = new NewsDAO();

    private final String[] fakeSignals = {
            "breaking", "shocking", "miracle", "secret", "scam",
            "hoax", "conspiracy", "hidden", "exposed"
    };

    private final String[] realSignals = {
            "according to", "research shows", "study published",
            "evidence", "peer-reviewed", "official", "data indicates"
    };

    public FakeNewsGUI() {
        setTitle("FACTSHIELD");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBorder(new EmptyBorder(15, 15, 15, 15));

        main.add(createHeader(), BorderLayout.NORTH);
        main.add(createCenter(), BorderLayout.CENTER);
        main.add(createBottomBar(), BorderLayout.SOUTH);

        add(main);

        updateStatus("System Ready", new Color(46, 204, 113));
    }

    // ---------------- HEADER ----------------
    private JPanel createHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JLabel title = new JLabel("FACTSHIELD", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(new Color(52, 152, 219));

        JLabel sub = new JLabel("Content Credibility Analyzer", SwingConstants.CENTER);
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));

        p.add(title, BorderLayout.CENTER);
        p.add(sub, BorderLayout.SOUTH);

        return p;
    }

    // ---------------- CENTER ----------------
    private JSplitPane createCenter() {

        JPanel left = new JPanel(new BorderLayout(10, 10));
        left.setBorder(new TitledBorder("Input News Text"));

        input = new JTextArea();
        input.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        input.setLineWrap(true);
        input.setWrapStyleWord(true);

        left.add(new JScrollPane(input), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout(10, 10));
        right.setBorder(new TitledBorder("Analysis Result"));

        resultLabel = new JLabel("PENDING", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 24));

        confidenceBar = new JProgressBar(0, 100);
        confidenceBar.setStringPainted(true);

        scoreValue = new JLabel("--/100", SwingConstants.CENTER);
        scoreValue.setFont(new Font("Segoe UI", Font.BOLD, 18));

        detailPanel = new JPanel();
        detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));

        right.add(resultLabel, BorderLayout.NORTH);
        right.add(confidenceBar, BorderLayout.CENTER);
        right.add(scoreValue, BorderLayout.SOUTH);
        right.add(new JScrollPane(detailPanel), BorderLayout.EAST);

        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
    }

    // ---------------- BUTTON BAR ----------------
    private JPanel createBottomBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton analyze = button("Analyze", new Color(46, 204, 113), e -> analyze());
        JButton clear = button("Clear", new Color(231, 76, 60), e -> clearAll());
        JButton history = button("History", new Color(52, 152, 219), e -> showHistory());
        JButton theme = button("Dark Mode", new Color(52, 73, 94), e -> toggleTheme());

        statusLabel = new JLabel("Ready");

        p.add(analyze);
        p.add(clear);
        p.add(history);
        p.add(theme);
        p.add(statusLabel);

        return p;
    }

    private JButton button(String text, Color c, java.awt.event.ActionListener a) {
        JButton b = new JButton(text);
        b.setBackground(c);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(8, 15, 8, 15));
        b.addActionListener(a);
        return b;
    }

    // ---------------- ANALYSIS ----------------
    private void analyze() {
        String text = input.getText().trim();

        if (text.isEmpty()) {
            updateStatus("Enter text first!", Color.RED);
            return;
        }

        new javax.swing.Timer(800, e -> {
            performAnalysis(text);
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }

    // =========================
    // MAIN INTEGRATION POINT
    // =========================
    private void performAnalysis(String text) {

        // MEMBER 3 LOGIC
        String verdict = detector.analyze(text);

        String lower = text.toLowerCase();
        int fake = 0, real = 50;

        for (String s : fakeSignals) if (lower.contains(s)) fake += 10;
        for (String s : realSignals) if (lower.contains(s)) real += 15;

        if (lower.contains("!!!")) fake += 10;
        if (lower.matches(".*[A-Z]{5,}.*")) fake += 15;

        int score = Math.max(0, Math.min(100, real - fake));

        Color color;

        if (score > 70) {
            color = new Color(46, 204, 113);
        } else if (score > 40) {
            color = new Color(243, 156, 18);
        } else {
            color = new Color(231, 76, 60);
        }

        resultLabel.setText(verdict);
        resultLabel.setForeground(color);

        confidenceBar.setValue(score);
        confidenceBar.setForeground(color);

        scoreValue.setText(score + "/100");
        scoreValue.setForeground(color);

        updateDetails(score);

        // =========================
        // MEMBER 2 DATABASE SAVE
        // =========================
        dao.save(text, verdict, score);

        updateStatus("Saved to Database", color);
    }

    private void updateDetails(int score) {

        detailPanel.removeAll();

        String[] items = (score > 70)
                ? new String[]{"High credibility sources", "Neutral tone", "Verified structure"}
                : (score > 40)
                ? new String[]{"Mixed signals", "Needs verification", "Partial credibility"}
                : new String[]{"Sensational language", "Low trust signals", "Unverified content"};

        for (String s : items) {
            detailPanel.add(new JLabel("• " + s));
        }

        detailPanel.revalidate();
        detailPanel.repaint();
    }

    private void clearAll() {
        input.setText("");
        resultLabel.setText("PENDING");
        confidenceBar.setValue(0);
        scoreValue.setText("--/100");
        detailPanel.removeAll();
    }

    private void toggleTheme() {
        try {
            darkMode = !darkMode;
            UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showHistory() {
        JOptionPane.showMessageDialog(this, "History feature can be added here.");
    }

    private void updateStatus(String msg, Color c) {
        statusLabel.setText(msg);
        statusLabel.setForeground(c);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception e) {}

        SwingUtilities.invokeLater(() -> new FakeNewsGUI().setVisible(true));
    }
}