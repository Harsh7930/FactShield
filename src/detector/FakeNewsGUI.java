package detector;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import dao.NewsDAO;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class FakeNewsGUI extends JFrame {

    private JTextArea input;
    /** Large verdict strip (Fake / Real / Error / Pending). */
    private JLabel verdictBadge;
    private JLabel scoreValue;
    private JLabel threatLevelLabel;
    private JLabel keywordsLineLabel;
    private JLabel statusLabel;
    private JLabel counterLabel;

    private JProgressBar confidenceBar;
    private JLabel confidenceLegend;
    private JProgressBar busyBar;

    private JButton analyzeButton;

    private boolean darkMode = false;

    private final FakeNewsDetector detector = new FakeNewsDetector();
    private final NewsDAO dao = new NewsDAO();

    private static final int MAX_CHARS = 500;

    private static final Color CONF_HIGH = new Color(46, 204, 113);
    private static final Color CONF_MED = new Color(241, 196, 15);
    private static final Color CONF_LOW = new Color(231, 76, 60);

    private static final Color BADGE_FAKE = new Color(192, 57, 43);
    private static final Color BADGE_REAL = new Color(39, 174, 96);
    private static final Color BADGE_ERROR = new Color(127, 140, 141);
    private static final Color BADGE_PENDING = new Color(149, 165, 166);

    public FakeNewsGUI() {
        setTitle("FactShield");
        setSize(1200, 750);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));

        mainPanel.add(createHeader(), BorderLayout.NORTH);
        mainPanel.add(createCenter(), BorderLayout.CENTER);
        mainPanel.add(createBottomBar(), BorderLayout.SOUTH);

        add(mainPanel);

        setupKeyBindings();

        updateStatus("System Ready", CONF_HIGH);
    }

    private JPanel createHeader() {
        JPanel panel = new JPanel(new BorderLayout());

        JLabel subtitle = new JLabel("Content Credibility Analyzer", SwingConstants.CENTER);
        subtitle.setFont(new Font("Segoe UI", Font.BOLD, 18));

        panel.add(subtitle, BorderLayout.CENTER);

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

        input.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateCounter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateCounter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateCounter();
            }

            private void updateCounter() {
                counterLabel.setText(input.getText().length() + " / " + MAX_CHARS);
            }
        });

        leftPanel.add(new JScrollPane(input), BorderLayout.CENTER);
        leftPanel.add(counterLabel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBorder(new TitledBorder("Analysis Result"));

        verdictBadge = new JLabel("PENDING", SwingConstants.CENTER);
        verdictBadge.setFont(new Font("Segoe UI", Font.BOLD, 17));
        verdictBadge.setOpaque(true);
        verdictBadge.setForeground(Color.WHITE);
        verdictBadge.setBackground(BADGE_PENDING);
        verdictBadge.setBorder(new EmptyBorder(14, 18, 14, 18));

        busyBar = new JProgressBar();
        busyBar.setIndeterminate(true);
        busyBar.setStringPainted(true);
        busyBar.setString("Analyzing...");
        busyBar.setVisible(false);

        confidenceBar = new JProgressBar(0, 100);
        confidenceBar.setStringPainted(true);
        confidenceBar.setFont(new Font("Segoe UI", Font.BOLD, 14));

        scoreValue = new JLabel("--", SwingConstants.CENTER);
        scoreValue.setFont(new Font("Segoe UI", Font.BOLD, 16));

        threatLevelLabel = new JLabel(" ", SwingConstants.CENTER);
        threatLevelLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        keywordsLineLabel = new JLabel(" ", SwingConstants.CENTER);
        keywordsLineLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        keywordsLineLabel.setForeground(new Color(80, 80, 80));

        confidenceLegend = new JLabel(
                "Confidence bar: green (\u226570%), yellow (30-69%), red (<30%)",
                SwingConstants.CENTER);
        confidenceLegend.setFont(confidenceLegend.getFont().deriveFont(10f));
        confidenceLegend.setForeground(Color.GRAY);

        verdictBadge.setAlignmentX(Component.CENTER_ALIGNMENT);
        busyBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        scoreValue.setAlignmentX(Component.CENTER_ALIGNMENT);
        confidenceBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        threatLevelLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        keywordsLineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        confidenceLegend.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel resultStack = new JPanel();
        resultStack.setLayout(new BoxLayout(resultStack, BoxLayout.Y_AXIS));
        resultStack.add(verdictBadge);
        resultStack.add(Box.createVerticalStrut(8));
        resultStack.add(busyBar);
        resultStack.add(Box.createVerticalStrut(8));
        resultStack.add(scoreValue);
        resultStack.add(Box.createVerticalStrut(8));
        resultStack.add(confidenceBar);
        resultStack.add(Box.createVerticalStrut(6));
        resultStack.add(threatLevelLabel);
        resultStack.add(Box.createVerticalStrut(4));
        resultStack.add(keywordsLineLabel);
        resultStack.add(Box.createVerticalStrut(6));
        resultStack.add(confidenceLegend);

        rightPanel.add(resultStack, BorderLayout.CENTER);

        return new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
    }

    private JPanel createBottomBar() {
        JPanel panel = new JPanel(new FlowLayout());

        analyzeButton = createButton(
                "Analyze",
                CONF_HIGH,
                "Analyze the news text (Ctrl+Enter)",
                e -> analyze());
        panel.add(analyzeButton);

        panel.add(createButton(
                "Clear",
                CONF_LOW,
                "Clear the input and result (Esc)",
                e -> clearAll()));

        panel.add(createButton(
                "History",
                new Color(52, 152, 219),
                "View past analyses",
                e -> showHistory()));

        panel.add(createButton(
                "Paste",
                new Color(155, 89, 182),
                "Paste from clipboard (Ctrl+V)",
                e -> pasteText()));

        panel.add(createButton(
                "Dark Mode",
                new Color(52, 73, 94),
                "Toggle dark/light theme",
                e -> toggleTheme()));

        statusLabel = new JLabel("Ready");
        panel.add(statusLabel);

        return panel;
    }

    private JButton createButton(String text, Color color, String tooltip, java.awt.event.ActionListener action) {
        JButton button = new JButton(text);
        button.setBackground(color);
        button.setForeground(Color.WHITE);
        button.setToolTipText(tooltip);
        button.addActionListener(action);
        return button;
    }

    private void analyze() {
        final String text = input.getText().trim();

        if (text.isEmpty()) {
            updateStatus("Enter text first!", Color.RED);
            return;
        }

        if (text.length() > MAX_CHARS) {
            updateStatus("Text too long (max " + MAX_CHARS + " characters)", Color.RED);
            return;
        }

        analyzeButton.setText("Analyzing...");
        analyzeButton.setEnabled(false);
        busyBar.setVisible(true);
        busyBar.setIndeterminate(true);

        SwingWorker<FakeNewsDetector.Result, Void> worker =
                new SwingWorker<FakeNewsDetector.Result, Void>() {
                    @Override
                    protected FakeNewsDetector.Result doInBackground() {
                        return detector.analyze(text);
                    }

                    @Override
                    protected void done() {
                        analyzeButton.setText("Analyze");
                        analyzeButton.setEnabled(true);
                        busyBar.setVisible(false);
                        busyBar.setIndeterminate(false);

                        try {
                            FakeNewsDetector.Result result = get();
                            applyAnalysisResult(text, result);
                        } catch (Exception ex) {
                            applyAnalysisResult(text, new FakeNewsDetector.Result("Error", 0));
                        }
                    }
                };
        worker.execute();
    }

    private void applyAnalysisResult(String newsText, FakeNewsDetector.Result result) {
        if ("Error".equalsIgnoreCase(result.verdict)) {
            verdictBadge.setText("ANALYSIS UNAVAILABLE");
            verdictBadge.setBackground(BADGE_ERROR);
            verdictBadge.setForeground(Color.WHITE);
            styleConfidenceBarError();
            scoreValue.setText("");
            scoreValue.setForeground(Color.GRAY);
            threatLevelLabel.setText("Threat level: —");
            threatLevelLabel.setForeground(Color.GRAY);
            keywordsLineLabel.setText(" ");
            dao.save(newsText, "Error", 0);
            updateStatus("Analysis unavailable", Color.GRAY);
            return;
        }

        if ("Fake".equalsIgnoreCase(result.verdict)) {
            verdictBadge.setText("FAKE NEWS DETECTED  ⚠️");
            verdictBadge.setBackground(BADGE_FAKE);
        } else {
            verdictBadge.setText("CREDIBLE NEWS  ✅");
            verdictBadge.setBackground(BADGE_REAL);
        }
        verdictBadge.setForeground(Color.WHITE);

        applyConfidenceBarForPercent(result.confidencePercent);
        scoreValue.setText("Confidence: " + result.confidencePercent + "%");
        scoreValue.setForeground(confidenceBar.getForeground());

        setThreatLevelLabel(result.confidencePercent);

        if ("Fake".equalsIgnoreCase(result.verdict) && result.keywordMatchCount > 0) {
            keywordsLineLabel.setText("Red-flag keywords found: " + String.join(", ", result.matchedKeywords));
            keywordsLineLabel.setForeground(new Color(120, 40, 40));
        } else {
            keywordsLineLabel.setText(" ");
        }

        dao.save(newsText, result.verdict, result.confidencePercent);
        updateStatus("Analysis Complete", confidenceBar.getForeground());
    }

    private void setThreatLevelLabel(int confidencePercent) {
        String band;
        if (confidencePercent < 40) {
            band = "Low";
        } else if (confidencePercent <= 70) {
            band = "Moderate";
        } else {
            band = "High";
        }
        threatLevelLabel.setText("Threat level: " + band);
        threatLevelLabel.setForeground(Color.DARK_GRAY);
    }

    private void applyConfidenceBarForPercent(int percent) {
        confidenceBar.setBorder(null);
        Color c;
        if (percent >= 70) {
            c = CONF_HIGH;
        } else if (percent >= 30) {
            c = CONF_MED;
        } else {
            c = CONF_LOW;
        }
        confidenceBar.setValue(percent);
        confidenceBar.setForeground(c);
        confidenceBar.setString(percent + "%");
    }

    private void styleConfidenceBarError() {
        confidenceBar.setValue(0);
        confidenceBar.setForeground(Color.GRAY);
        confidenceBar.setString("—");
        try {
            confidenceBar.setBorder(BorderFactory.createDashedBorder(Color.GRAY, 1.0f, 5.0f, 2.0f, false));
        } catch (Throwable ignored) {
            confidenceBar.setBorder(BorderFactory.createLineBorder(Color.GRAY, 2));
        }
    }

    private void clearAll() {
        input.setText("");
        verdictBadge.setText("PENDING");
        verdictBadge.setBackground(BADGE_PENDING);
        verdictBadge.setForeground(Color.WHITE);
        confidenceBar.setBorder(null);
        confidenceBar.setValue(0);
        confidenceBar.setString("");
        confidenceBar.setForeground(CONF_HIGH);
        scoreValue.setText("--");
        scoreValue.setForeground(UIManager.getColor("Label.foreground"));
        threatLevelLabel.setText(" ");
        keywordsLineLabel.setText(" ");
        keywordsLineLabel.setForeground(new Color(80, 80, 80));
        confidenceLegend.setForeground(Color.GRAY);
        counterLabel.setText("0 / " + MAX_CHARS);
    }

    private void showHistory() {
        if (!dao.isDatabaseAvailable()) {
            JOptionPane.showMessageDialog(
                    this,
                    "History unavailable — database is not connected.",
                    "History",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        List<NewsDAO.NewsRecord> history = dao.getHistory();

        if (history.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "No saved analyses yet.",
                    "History",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setRows(18);
        area.setColumns(52);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));

        for (NewsDAO.NewsRecord row : history) {
            area.append(
                    "[" + row.analyzedAt + "] " + row.verdict + " (" + row.score + "%)\n"
                            + row.newsText + "\n\n");
        }

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setPreferredSize(new Dimension(520, 400));

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
                    updateStatus("Text pasted", CONF_HIGH);
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

    private void setupKeyBindings() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.CTRL_DOWN_MASK), "analyze");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK), "paste");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clear");

        am.put("analyze", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                analyze();
            }
        });
        am.put("paste", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                pasteText();
            }
        });
        am.put("clear", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAll();
            }
        });
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
