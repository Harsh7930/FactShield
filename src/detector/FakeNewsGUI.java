package detector;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import dao.NewsDAO;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.util.List;

public class FakeNewsGUI extends JFrame {

    private JTextArea input;
    private JLabel resultLabel;
    private JLabel scoreValue;
    private JLabel statusLabel;
    private JLabel counterLabel;

    private JProgressBar confidenceBar;
    private boolean darkMode = false;

    private final FakeNewsDetector detector = new FakeNewsDetector();
    private final NewsDAO dao = new NewsDAO();

    private static final int MAX_CHARS = 500;

    public FakeNewsGUI() {
        setTitle("FACTSHIELD");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createCenter(), BorderLayout.CENTER);
        mainPanel.add(createBottomBar(), BorderLayout.SOUTH);

        add(mainPanel);

        updateStatus("System Ready", new Color(46, 204, 113));
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel title = new JLabel("FACTSHIELD", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));

        JLabel subtitle = new JLabel("Content Credibility Analyzer", SwingConstants.CENTER);

        panel.add(title, BorderLayout.CENTER);
        panel.add(subtitle, BorderLayout.SOUTH);

        return panel;
    }

    private JSplitPane createCenter() {

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setBorder(new TitledBorder("Input News Text"));

        input = new JTextArea();
        input.setLineWrap(true);
        input.setWrapStyleWord(true);

        ((AbstractDocument) input.getDocument())
                .setDocumentFilter(new TextLimitFilter(MAX_CHARS));

        counterLabel = new JLabel("0 / " + MAX_CHARS, SwingConstants.RIGHT);

        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateCounter(); }

            private void updateCounter() {
                counterLabel.setText(input.getText().length() + " / " + MAX_CHARS);
            }
        });

        leftPanel.add(new JScrollPane(input), BorderLayout.CENTER);
        leftPanel.add(counterLabel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Analysis Result"));

        resultLabel = new JLabel("PENDING", SwingConstants.CENTER);
        resultLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));

        confidenceBar = new JProgressBar(0, 100);
        confidenceBar.setStringPainted(true);
        confidenceBar.setFont(new Font("Segoe UI", Font.BOLD, 14));

        scoreValue = new JLabel("--", SwingConstants.CENTER);
        scoreValue.setFont(new Font("Segoe UI", Font.BOLD, 18));

        rightPanel.add(resultLabel, BorderLayout.NORTH);
        rightPanel.add(confidenceBar, BorderLayout.CENTER);
        rightPanel.add(scoreValue, BorderLayout.SOUTH);

        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
    }

    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new FlowLayout());

        panel.add(createButton("Analyze", new Color(46, 204, 113), e -> analyze()));
        panel.add(createButton("Clear", new Color(231, 76, 60), e -> clearAll()));
        panel.add(createButton("History", new Color(52, 152, 219), e -> showHistory()));
        panel.add(createButton("Paste", new Color(155, 89, 182), e -> pasteText()));
        panel.add(createButton("Dark Mode", new Color(52, 73, 94), e -> toggleTheme()));

        statusLabel = new JLabel("Ready");
        panel.add(statusLabel);

        return panel;
    }

    private JButton createButton(String text, Color color, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.addActionListener(action);
        return button;
    }

    private void analyze() {
        String text = input.getText().trim();

        if (text.isEmpty()) {
            updateStatus("Enter text first!", Color.RED);
            return;
        }

        if (text.length() > MAX_CHARS) {
            updateStatus("Text too long (max " + MAX_CHARS + " characters)", Color.RED);
            return;
        }

        String verdict = detector.analyze(text);
        int score;
        if ("Error".equalsIgnoreCase(verdict)) {
            score = 0;
        } else if ("Real".equalsIgnoreCase(verdict)) {
            score = 80;
        } else {
            score = 30;
        }

        String label;
        Color color;

        if (score >= 75) {
            label = "Highly Reliable";
            color = new Color(46, 204, 113);
        } else if (score >= 50) {
            label = "Moderate Trust";
            color = new Color(241, 196, 15);
        } else if (score >= 25) {
            label = "Suspicious";
            color = new Color(230, 126, 34);
        } else {
            label = "Likely Fake";
            color = new Color(231, 76, 60);
        }

        resultLabel.setText(label);
        resultLabel.setForeground(color);

        confidenceBar.setValue(score);
        confidenceBar.setForeground(color);
        confidenceBar.setString(score + "% Confidence");

        scoreValue.setText(score + "% (" + label + ")");
        scoreValue.setForeground(color);

        dao.save(text, verdict, score);

        updateStatus("Analysis Complete", color);
    }

    private void clearAll() {
        input.setText("");
        resultLabel.setText("PENDING");
        confidenceBar.setValue(0);
        confidenceBar.setString("");
        scoreValue.setText("--");
        counterLabel.setText("0 / " + MAX_CHARS);
    }

    private void showHistory() {
        List<NewsDAO.NewsRecord> history = dao.getHistory();

        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No history found.");
            return;
        }

        JTextArea area = new JTextArea();
        area.setEditable(false);

        for (NewsDAO.NewsRecord row : history) {
            area.append(
                    "[" + row.analyzedAt + "] " + row.verdict + " (" + row.score + "%)\n"
                            + row.newsText + "\n\n");
        }

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(500, 400));

        JOptionPane.showMessageDialog(this, scrollPane, "History", JOptionPane.INFORMATION_MESSAGE);
    }

    private void pasteText() {
        try {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            String data = (String) clipboard.getData(DataFlavor.stringFlavor);

            if (data != null) {
                if (data.length() > MAX_CHARS) {
                    data = data.substring(0, MAX_CHARS);
                    updateStatus("Pasted text trimmed to limit", Color.ORANGE);
                } else {
                    updateStatus("Text pasted", new Color(46, 204, 113));
                }

                input.setText(data);
                counterLabel.setText(input.getText().length() + " / " + MAX_CHARS);
            }

        } catch (Exception e) {
            updateStatus("Paste failed", Color.RED);
        }
    }

    private void toggleTheme() {
        try {
            darkMode = !darkMode;
            UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ignored) {}
    }

    private void updateStatus(String message, Color color) {
        statusLabel.setText(message);
        statusLabel.setForeground(color);
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new FakeNewsGUI().setVisible(true));
    }

    class TextLimitFilter extends DocumentFilter {
        private final int limit;

        public TextLimitFilter(int limit) {
            this.limit = limit;
        }

        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            if (fb.getDocument().getLength() + text.length() <= limit) {
                super.insertString(fb, offset, text, attr);
            }
        }

        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            if (fb.getDocument().getLength() - length + text.length() <= limit) {
                super.replace(fb, offset, length, text, attrs);
            }
        }
    }
}