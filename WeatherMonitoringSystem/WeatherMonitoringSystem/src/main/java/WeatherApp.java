import station.WeatherStation;
import displays.CurrentConditionsDisplay;
import displays.StatisticsDisplay;
import displays.ForecastDisplay;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.json.JSONArray;
import org.json.JSONObject;

public class WeatherApp {
    private static final String API_KEY = "fe984e14fe26f479193efd94361c378d";
    private static final long POLL_INTERVAL_SECONDS = 10L;
    private static final Color PRIMARY_BLUE     = new Color(59, 130, 246);
    private static final Color SECONDARY_BLUE   = new Color(37, 99, 235);
    private static final Color ACCENT_GREEN     = new Color(34, 197, 94);
    private static final Color ACCENT_RED       = new Color(239, 68, 68);
    private static final Color BACKGROUND_LIGHT = new Color(248, 250, 252);
    private static final Color CARD_BACKGROUND  = new Color(255, 255, 255);
    private static final Color TEXT_PRIMARY     = new Color(51, 65, 85);
    private static final Color TEXT_SECONDARY   = new Color(100, 116, 139);
    private static final String WEATHER_ICON_URL   = "https://cdn-icons-png.flaticon.com/32/1163/1163661.png";
    private static final String SEARCH_ICON_URL    = "https://cdn-icons-png.flaticon.com/16/54/54481.png";
    private static final String REFRESH_ICON_URL   = "https://cdn-icons-png.flaticon.com/16/2805/2805355.png";
    private static final String QUIT_ICON_URL      = "https://cdn-icons-png.flaticon.com/16/1828/1828774.png";
    private static final String TEMP_ICON_URL      = "https://cdn-icons-png.flaticon.com/24/1684/1684371.png";
    private static final String STATS_ICON_URL     = "https://cdn-icons-png.flaticon.com/24/2920/2920277.png";
    private static final String FORECAST_ICON_URL  = "https://cdn-icons-png.flaticon.com/24/1163/1163627.png";
    private final WeatherStation weatherStation = new WeatherStation();
    private ScheduledExecutorService scheduler;
    private volatile String currentCity = "";
    private final JComboBox<String> cityComboBox         = new JComboBox<>();
    private final DefaultComboBoxModel<String> cityModel  = new DefaultComboBoxModel<>();
    private javax.swing.Timer geoDebounceTimer;
    private JLabel statusLabel;
    private JLabel lastUpdatedLabel;
    private JFrame mainFrame;
    private boolean isUpdatingModel = false;
    private CurrentConditionsDisplay currentPanel;
    private StatisticsDisplay statsPanel;
    private ForecastDisplay   forecastPanel;

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            customizeNimbusTheme();
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> {
            try {
                new WeatherApp().createAndShowGUI();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }
    private static void customizeNimbusTheme() {
        UIManager.put("Label.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("Button.font", new Font("Segoe UI", Font.BOLD, 12));
        UIManager.put("ComboBox.font", new Font("Segoe UI", Font.PLAIN, 13));
        UIManager.put("TitledBorder.font", new Font("Segoe UI", Font.BOLD, 14));
        UIManager.put("TextField.font", new Font("Segoe UI", Font.PLAIN, 13));
    }
    private void createAndShowGUI() {
        mainFrame = new JFrame("Weather Monitoring System");
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(600, 700);
        mainFrame.setMinimumSize(new Dimension(550, 650));
        mainFrame.setLayout(new BorderLayout());
        mainFrame.getContentPane().setBackground(BACKGROUND_LIGHT);
        setFrameIcon();
        JPanel headerPanel = createHeaderPanel();
        JPanel controlsPanel = createControlsPanel();
        JPanel northWrapper = new JPanel();
        northWrapper.setLayout(new BoxLayout(northWrapper, BoxLayout.Y_AXIS));
        northWrapper.add(headerPanel);
        northWrapper.add(controlsPanel);
        mainFrame.add(northWrapper, BorderLayout.NORTH);
        currentPanel  = new CurrentConditionsDisplay(weatherStation);
        statsPanel    = new StatisticsDisplay(weatherStation);
        forecastPanel = new ForecastDisplay(weatherStation);
        styleDisplayPanels(currentPanel, statsPanel, forecastPanel);
        JPanel centerWrapper = createCenterPanel(currentPanel, statsPanel, forecastPanel);
        mainFrame.add(centerWrapper, BorderLayout.CENTER);
        JPanel footerPanel = createFooterPanel();
        mainFrame.add(footerPanel, BorderLayout.SOUTH);
        mainFrame.setLocationRelativeTo(null);
        mainFrame.setVisible(true);
        startPolling();
    }
    private void setFrameIcon() {
        ImageIcon icon = loadIconFromURL(WEATHER_ICON_URL);
        if (icon != null) {
            mainFrame.setIconImage(icon.getImage());
        }
    }
    private ImageIcon loadIconFromURL(String urlString) {
        try {
            URL url = new URL(urlString);
            BufferedImage image = ImageIO.read(url);
            return new ImageIcon(image);
        } catch (Exception ignored) {
            return null;
        }
    }
    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gradient = new GradientPaint(0, 0, PRIMARY_BLUE, 0, getHeight(), SECONDARY_BLUE);
                g2d.setPaint(gradient);
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        headerPanel.setBorder(new EmptyBorder(20, 25, 20, 25));
        headerPanel.setPreferredSize(new Dimension(0, 85));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets   = new Insets(0, 0, 0, 10);
        gbc.anchor   = GridBagConstraints.WEST;
        gbc.fill     = GridBagConstraints.VERTICAL;
        ImageIcon weatherIcon = loadIconFromURL(WEATHER_ICON_URL);
        JLabel iconLabel = new JLabel(weatherIcon != null ? weatherIcon : new JLabel("🌤️").getIcon());
        iconLabel.setBorder(new EmptyBorder(0, 0, 0, 10));
        gbc.gridx     = 0;
        headerPanel.add(iconLabel, gbc);
        JLabel titleLabel = new JLabel("Weather Monitoring System");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        gbc.gridx     = 1;
        gbc.weightx   = 1;
        headerPanel.add(titleLabel, gbc);
        JPanel searchPanel = createSearchPanel();
        GridBagConstraints gbcSearch = new GridBagConstraints();
        gbcSearch.gridx    = 2;
        gbcSearch.anchor   = GridBagConstraints.EAST;
        headerPanel.add(searchPanel, gbcSearch);
        return headerPanel;
    }
    private JPanel createControlsPanel() {
        JPanel controlsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        controlsPanel.setBackground(BACKGROUND_LIGHT);
        JCheckBox cbCurrent = new JCheckBox("Show Current");
        cbCurrent.setSelected(true);
        cbCurrent.addActionListener(e -> {
            if (cbCurrent.isSelected()) {
                weatherStation.registerObserver(currentPanel);
                currentPanel.setVisible(true);
            } else {
                weatherStation.removeObserver(currentPanel);
                currentPanel.setVisible(false);
            }
            mainFrame.revalidate();
        });
        JCheckBox cbStats = new JCheckBox("Show Statistics");
        cbStats.setSelected(true);
        cbStats.addActionListener(e -> {
            if (cbStats.isSelected()) {
                weatherStation.registerObserver(statsPanel);
                statsPanel.setVisible(true);
            } else {
                weatherStation.removeObserver(statsPanel);
                statsPanel.setVisible(false);
            }
            mainFrame.revalidate();
        });
        JCheckBox cbForecast = new JCheckBox("Show Forecast");
        cbForecast.setSelected(true);
        cbForecast.addActionListener(e -> {
            if (cbForecast.isSelected()) {
                weatherStation.registerObserver(forecastPanel);
                forecastPanel.setVisible(true);
            } else {
                weatherStation.removeObserver(forecastPanel);
                forecastPanel.setVisible(false);
            }
            mainFrame.revalidate();
        });
        controlsPanel.add(cbCurrent);
        controlsPanel.add(cbStats);
        controlsPanel.add(cbForecast);
        return controlsPanel;
    }
    private JPanel createSearchPanel() {
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        searchPanel.setOpaque(false);
        ImageIcon searchIcon = loadIconFromURL(SEARCH_ICON_URL);
        JLabel searchIconLabel = new JLabel(searchIcon);
        searchIconLabel.setBorder(new EmptyBorder(8, 0, 0, 5));
        cityComboBox.setModel(cityModel);
        cityComboBox.setEditable(true);
        cityComboBox.setPrototypeDisplayValue("________________________");
        cityComboBox.setPreferredSize(new Dimension(240, 32));
        cityComboBox.setBackground(Color.WHITE);
        cityComboBox.setBorder(new LineBorder(new Color(229, 231, 235), 1, true));
        cityComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cityComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = cityComboBox.getPreferredSize().width;
                return d;
            }
        });
        setupCityComboBoxListeners();
        searchPanel.add(searchIconLabel);
        searchPanel.add(cityComboBox);
        return searchPanel;
    }
    private void setupCityComboBoxListeners() {
        cityComboBox.addActionListener(e -> {
            if (!isUpdatingModel && (e.getActionCommand().equals("comboBoxEdited") ||
                    e.getActionCommand().equals("comboBoxChanged"))) {
                String chosen = String.valueOf(cityComboBox.getSelectedItem()).trim();
                if (!chosen.isEmpty() && !chosen.equals(currentCity)) {
                    currentCity = chosen;
                    updateStatusLabel("Updating weather for " + chosen + "...", PRIMARY_BLUE);
                    SwingUtilities.invokeLater(cityComboBox::hidePopup);
                }
            }
        });
        JTextField editor = (JTextField) cityComboBox.getEditor().getEditorComponent();
        editor.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { scheduleGeoLookup(); }
            @Override public void removeUpdate(DocumentEvent e) { scheduleGeoLookup(); }
            @Override public void changedUpdate(DocumentEvent e) { scheduleGeoLookup(); }
        });
        editor.addFocusListener(new FocusAdapter() {
            @Override public void focusLost(FocusEvent e) {
                SwingUtilities.invokeLater(cityComboBox::hidePopup);
            }
        });
    }
    private void styleDisplayPanels(CurrentConditionsDisplay currentPanel,
                                    StatisticsDisplay statsPanel,
                                    ForecastDisplay forecastPanel) {
        ImageIcon tempIcon = loadIconFromURL(TEMP_ICON_URL);
        currentPanel.setBorder(createIconTitledBorder("Current Conditions", tempIcon));
        currentPanel.setBackground(Color.WHITE);
        currentPanel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        ImageIcon statsIcon = loadIconFromURL(STATS_ICON_URL);
        statsPanel.setBorder(createIconTitledBorder("Statistics", statsIcon));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ImageIcon forecastIcon = loadIconFromURL(FORECAST_ICON_URL);
        forecastPanel.setBorder(createIconTitledBorder("Forecast", forecastIcon));
        forecastPanel.setBackground(Color.WHITE);
        forecastPanel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    }
    private javax.swing.border.TitledBorder createIconTitledBorder(String title, ImageIcon icon) {
        JLabel titleLabel = new JLabel(title, icon, SwingConstants.LEFT);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        titleLabel.setForeground(TEXT_PRIMARY);
        javax.swing.border.TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createEmptyBorder(), title);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 15));
        border.setTitleColor(TEXT_PRIMARY);
        return border;
    }
    private JPanel createCenterPanel(CurrentConditionsDisplay currentPanel,
                                     StatisticsDisplay statsPanel,
                                     ForecastDisplay forecastPanel) {
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(BACKGROUND_LIGHT);
        centerPanel.setBorder(new EmptyBorder(20, 25, 15, 25));

        centerPanel.add(createCardWrapper(currentPanel));
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(createCardWrapper(statsPanel));
        centerPanel.add(Box.createVerticalStrut(25));
        centerPanel.add(createCardWrapper(forecastPanel));
        JScrollPane scrollPane = new JScrollPane(centerPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBackground(BACKGROUND_LIGHT);

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BACKGROUND_LIGHT);
        wrapper.add(scrollPane, BorderLayout.CENTER);
        return wrapper;
    }
    private JPanel createCardWrapper(JPanel contentPanel) {
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int width = getWidth(), height = getHeight(), arc = 12;
                g2d.setColor(new Color(0, 0, 0, 20));
                g2d.fillRoundRect(4, 4, width - 8, height - 8, arc + 2, arc + 2);
                g2d.setColor(CARD_BACKGROUND);
                g2d.fillRoundRect(0, 0, width - 8, height - 8, arc, arc);
                g2d.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(20, 25, 20, 25));
        wrapper.add(contentPanel, BorderLayout.CENTER);
        return wrapper;
    }
    private JPanel createFooterPanel() {
        JPanel footerPanel = new JPanel(new GridBagLayout());
        footerPanel.setBackground(BACKGROUND_LIGHT);
        footerPanel.setBorder(new EmptyBorder(15, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        statusLabel = new JLabel("Ready");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setOpaque(true);
        statusLabel.setBackground(BACKGROUND_LIGHT);
        statusLabel.setForeground(ACCENT_GREEN);
        gbc.gridx   = 0;
        gbc.gridy   = 0;
        gbc.weightx = 1;
        gbc.anchor  = GridBagConstraints.WEST;
        footerPanel.add(statusLabel, gbc);
        lastUpdatedLabel = new JLabel("");
        lastUpdatedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lastUpdatedLabel.setForeground(TEXT_SECONDARY);
        gbc.gridy   = 1;
        gbc.anchor  = GridBagConstraints.WEST;
        footerPanel.add(lastUpdatedLabel, gbc);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        ImageIcon refreshIcon = loadIconFromURL(REFRESH_ICON_URL);
        JButton refreshBtn = createModernButton("Refresh", PRIMARY_BLUE, refreshIcon);
        refreshBtn.addActionListener(e -> {
            updateStatusLabel("Refreshing data...", PRIMARY_BLUE);
            CompletableFuture.runAsync(() -> {
                try {
                    fetchAndUpdate();
                    SwingUtilities.invokeLater(() -> {
                        updateStatusLabel("Data updated - " + currentCity, ACCENT_GREEN);
                        flashStatusBackground(new Color(198, 251, 197), 500);
                        String time = LocalTime.now().withNano(0).toString();
                        lastUpdatedLabel.setText("Last updated: " + time);
                    });
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() ->
                            updateStatusLabel("Failed to update data", ACCENT_RED));
                }
            });
        });
        ImageIcon quitIcon = loadIconFromURL(QUIT_ICON_URL);
        JButton quitBtn = createModernButton("Quit", ACCENT_RED, quitIcon);
        quitBtn.addActionListener(e -> {
            if (scheduler != null && !scheduler.isShutdown()) {
                scheduler.shutdownNow();
            }
            mainFrame.dispose();
        });
        buttonPanel.add(refreshBtn);
        buttonPanel.add(quitBtn);
        gbc.gridy   = 0;
        gbc.gridx   = 1;
        gbc.weightx = 0;
        gbc.anchor  = GridBagConstraints.EAST;
        footerPanel.add(buttonPanel, gbc);
        return footerPanel;
    }
    private JButton createModernButton(String text, Color baseColor, ImageIcon icon) {
        JButton button = new JButton(text, icon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean isHovered = Boolean.TRUE.equals(getClientProperty("hovered"));
                Color bgColor = isHovered ? baseColor.brighter() : baseColor;
                g2d.setColor(bgColor);
                g2d.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                if (getIcon() != null) {
                    int iconX = 8;
                    int iconY = (getHeight() - getIcon().getIconHeight()) / 2;
                    getIcon().paintIcon(this, g2d, iconX, iconY);
                }
                g2d.setColor(Color.WHITE);
                g2d.setFont(getFont());
                FontMetrics fm = g2d.getFontMetrics();
                int textX = getIcon() != null
                        ? 8 + getIcon().getIconWidth() + 8
                        : (getWidth() - fm.stringWidth(getText())) / 2;
                int textY = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2d.drawString(getText(), textX, textY);
                g2d.dispose();
            }
        };
        button.setPreferredSize(new Dimension(icon != null ? 100 : 80, 32));
        button.setFocusPainted(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.putClientProperty("hovered", true);
                btn.repaint();
            }
            @Override public void mouseExited(MouseEvent e) {
                JButton btn = (JButton) e.getSource();
                btn.putClientProperty("hovered", false);
                btn.repaint();
            }
        });
        return button;
    }
    private void updateStatusLabel(String message, Color color) {
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText(message);
            statusLabel.setForeground(color);
        });
    }
    private void flashStatusBackground(Color flashColor, int durationMs) {
        Color original = statusLabel.getBackground();
        statusLabel.setBackground(flashColor);
        new javax.swing.Timer(durationMs, e -> {
            statusLabel.setBackground(original);
            ((javax.swing.Timer) e.getSource()).stop();
        }).start();
    }
    private void scheduleGeoLookup() {
        if (isUpdatingModel) return;
        if (geoDebounceTimer != null && geoDebounceTimer.isRunning()) {
            geoDebounceTimer.stop();
        }
        geoDebounceTimer = new javax.swing.Timer(500, e -> {
            String text = ((JTextField) cityComboBox.getEditor().getEditorComponent()).getText().trim();
            if (!text.isEmpty() && text.length() > 2) {
                updateStatusLabel("Searching for cities...", PRIMARY_BLUE);
                fetchCitySuggestions(text);
            }
            geoDebounceTimer.stop();
        });
        geoDebounceTimer.setRepeats(false);
        geoDebounceTimer.start();
    }
    private void fetchCitySuggestions(String query) {
        CompletableFuture.runAsync(() -> {
            try {
                String url = "https://api.openweathermap.org/geo/1.0/direct"
                        + "?q=" + URIEncoder.encode(query)
                        + "&limit=5"
                        + "&appid=" + API_KEY;
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(5))
                        .GET()
                        .build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) {
                    updateStatusLabel("Search failed", ACCENT_RED);
                    return;
                }
                JSONArray arr = new JSONArray(response.body());
                List<String> suggestions = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name    = obj.optString("name");
                    String country = obj.optString("country");
                    String state   = obj.optString("state");
                    StringBuilder displayName = new StringBuilder(name);
                    if (!state.isEmpty()) {
                        displayName.append(", ").append(state);
                    }
                    if (!country.isEmpty()) {
                        displayName.append(", ").append(country);
                    }
                    String cityString = displayName.toString();
                    if (!suggestions.contains(cityString)) {
                        suggestions.add(cityString);
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    if (!suggestions.isEmpty()) {
                        String currentText = ((JTextField) cityComboBox.getEditor().getEditorComponent()).getText();
                        isUpdatingModel = true;
                        cityModel.removeAllElements();
                        suggestions.forEach(cityModel::addElement);
                        ((JTextField) cityComboBox.getEditor().getEditorComponent()).setText(currentText);
                        isUpdatingModel = false;
                        cityComboBox.setPopupVisible(true);
                        updateStatusLabel("Found " + suggestions.size() + " cities", ACCENT_GREEN);
                    } else {
                        updateStatusLabel("No cities found", TEXT_SECONDARY);
                    }
                });
            } catch (Exception ex) {
                updateStatusLabel("Search error", ACCENT_RED);
            }
        });
    }
    private void startPolling() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            if (currentCity.isEmpty()) return;
            try {
                fetchAndUpdate();
                SwingUtilities.invokeLater(() -> {
                    updateStatusLabel("Data updated - " + currentCity, ACCENT_GREEN);
                    flashStatusBackground(new Color(198, 251, 197), 400);
                    String time = LocalTime.now().withNano(0).toString();
                    lastUpdatedLabel.setText("Last updated: " + time);
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() ->
                        updateStatusLabel("Connection error", ACCENT_RED));
            }
        }, 0, POLL_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }
    private void fetchAndUpdate() throws IOException, InterruptedException {
        String cityParam = currentCity.replace(" ", "%20");
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=" + URIEncoder.encode(cityParam)
                + "&units=metric"
                + "&appid=" + API_KEY;

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("OpenWeatherMap HTTP status: " + response.statusCode());
        }
        JSONObject json = new JSONObject(response.body());
        if (!json.has("main")) {
            throw new IOException("Invalid JSON: missing 'main' object");
        }
        JSONObject mainObj = json.getJSONObject("main");
        float temperature = (float) mainObj.getDouble("temp");
        float humidity    = (float) mainObj.getDouble("humidity");
        float pressure    = (float) mainObj.getDouble("pressure");
        weatherStation.setMeasurements(temperature, humidity, pressure);
    }
    private static class URIEncoder {
        public static String encode(String s) {
            return s.replace(" ", "%20")
                    .replace(",", "%2C")
                    .replace("/", "%2F")
                    .replace("#", "%23")
                    .replace("?", "%3F")
                    .replace("=", "%3D")
                    .replace("&", "%26");
        }
    }
}
