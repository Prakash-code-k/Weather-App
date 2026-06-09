import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.List;
import org.json.*;

public class WeatherApp extends JFrame {

    private static final String API_KEY  = "YOUR_API_KEY_HERE";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final String GEO_URL  = "http://ip-api.com/json";

    private static final Color BG_TOP      = new Color(0x0A1628);
    private static final Color BG_BOTTOM   = new Color(0x0D2B55);
    private static final Color CARD_BG     = new Color(255, 255, 255, 22);
    private static final Color CARD_BORDER = new Color(255, 255, 255, 45);
    private static final Color ACCENT      = new Color(0x4FC3F7);
    private static final Color ACCENT2     = new Color(0xFF8A65);
    private static final Color TEXT_PRI    = new Color(0xF0F8FF);
    private static final Color TEXT_SEC    = new Color(0xB0C4DE);
    private static final Color SUCCESS     = new Color(0x81C784);
    private static final Color WARN        = new Color(0xFFD54F);

    private GradientPanel   mainPanel;
    private JTextField      cityField;
    private JButton         searchBtn, locationBtn, unitToggleBtn;
    private JLabel          cityLabel, tempLabel, descLabel, feelsLabel;
    private JLabel          humidLabel, windLabel, visLabel, pressLabel;
    private JLabel          sunriseLabel, sunsetLabel, uvLabel, dewLabel;
    private JLabel          weatherIconLabel, lastUpdLabel, unitLabel;
    private JPanel          forecastPanel;
    private JProgressBar    loadingBar;
    private JLabel          statusLabel;

    private boolean     useMetric   = true;
    private String      currentCity = "";
    private HttpClient  http;

    public WeatherApp() {
        super("WeatherScope - Pinnacle Lab");
        http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        buildUI();
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(920, 780);
        setMinimumSize(new Dimension(780, 680));
        setLocationRelativeTo(null);
        setVisible(true);
        autoDetectLocation();
    }

    private void buildUI() {
        mainPanel = new GradientPanel(BG_TOP, BG_BOTTOM);
        mainPanel.setLayout(new BorderLayout(0, 0));
        setContentPane(mainPanel);
        mainPanel.add(buildHeader(),    BorderLayout.NORTH);
        mainPanel.add(buildCenter(),    BorderLayout.CENTER);
        mainPanel.add(buildStatusBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(12, 0));
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 22, 12, 22));

        JLabel logo = new JLabel("WeatherScope");
        logo.setFont(new Font("Segoe UI", Font.BOLD, 22));
        logo.setForeground(TEXT_PRI);

        JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        searchRow.setOpaque(false);

        unitToggleBtn = glassButton("C / F", ACCENT);
        locationBtn   = glassButton("Use My Location", ACCENT2);
        cityField     = new JTextField(18);
        styleTextField(cityField, "Enter city name...");
        searchBtn     = glassButton("Search", SUCCESS);

        searchRow.add(unitToggleBtn);
        searchRow.add(locationBtn);
        searchRow.add(cityField);
        searchRow.add(searchBtn);

        header.add(logo,      BorderLayout.WEST);
        header.add(searchRow, BorderLayout.EAST);

        loadingBar = new JProgressBar();
        loadingBar.setIndeterminate(false);
        loadingBar.setForeground(ACCENT);
        loadingBar.setBackground(new Color(255, 255, 255, 20));
        loadingBar.setBorderPainted(false);
        loadingBar.setPreferredSize(new Dimension(0, 3));

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(header,     BorderLayout.CENTER);
        wrap.add(loadingBar, BorderLayout.SOUTH);

        searchBtn.addActionListener(e -> searchByCity());
        cityField.addActionListener(e -> searchByCity());
        locationBtn.addActionListener(e -> autoDetectLocation());
        unitToggleBtn.addActionListener(e -> toggleUnits());

        return wrap;
    }

    private JPanel buildCenter() {
        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(0, 22, 0, 22));
        center.add(buildCurrentCard(), BorderLayout.NORTH);
        center.add(buildDetailsRow(),  BorderLayout.CENTER);
        center.add(buildForecastRow(), BorderLayout.SOUTH);
        return center;
    }

    private JPanel buildCurrentCard() {
        GlassCard card = new GlassCard();
        card.setLayout(new BorderLayout(16, 0));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);
        weatherIconLabel = new JLabel("Weather", SwingConstants.CENTER);
        weatherIconLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
        weatherIconLabel.setForeground(Color.WHITE);
        descLabel = new JLabel("-", SwingConstants.CENTER);
        descLabel.setFont(new Font("Segoe UI", Font.ITALIC, 16));
        descLabel.setForeground(TEXT_SEC);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; left.add(weatherIconLabel, gbc);
        gbc.gridy = 1; left.add(descLabel, gbc);

        JPanel mid = new JPanel();
        mid.setOpaque(false);
        mid.setLayout(new BoxLayout(mid, BoxLayout.Y_AXIS));
        cityLabel  = bigLabel("-", 28, TEXT_PRI, Font.BOLD);
        tempLabel  = bigLabel("-", 72, Color.WHITE, Font.BOLD);
        feelsLabel = new JLabel("Feels like -");
        feelsLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        feelsLabel.setForeground(TEXT_SEC);
        unitLabel  = new JLabel("Metric  (C , km/h)");
        unitLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        unitLabel.setForeground(new Color(255, 255, 255, 100));
        mid.add(cityLabel);
        mid.add(Box.createVerticalStrut(4));
        mid.add(tempLabel);
        mid.add(feelsLabel);
        mid.add(Box.createVerticalStrut(6));
        mid.add(unitLabel);

        lastUpdLabel = new JLabel("-", SwingConstants.RIGHT);
        lastUpdLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lastUpdLabel.setForeground(TEXT_SEC);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        right.setOpaque(false);
        right.add(lastUpdLabel);

        card.add(left,  BorderLayout.WEST);
        card.add(mid,   BorderLayout.CENTER);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildDetailsRow() {
        JPanel row = new JPanel(new GridLayout(1, 6, 10, 0));
        row.setOpaque(false);

        humidLabel   = metricLabel("-");
        windLabel    = metricLabel("-");
        visLabel     = metricLabel("-");
        pressLabel   = metricLabel("-");
        sunriseLabel = metricLabel("-");
        sunsetLabel  = metricLabel("-");

        row.add(metricCard("Humidity",   humidLabel));
        row.add(metricCard("Wind",       windLabel));
        row.add(metricCard("Visibility", visLabel));
        row.add(metricCard("Pressure",   pressLabel));
        row.add(metricCard("Sunrise",    sunriseLabel));
        row.add(metricCard("Sunset",     sunsetLabel));
        return row;
    }

    private JPanel metricCard(String title, JLabel valueLabel) {
        GlassCard card = new GlassCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 4, 4, 4);

        JLabel titleLbl = new JLabel(title, SwingConstants.CENTER);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        titleLbl.setForeground(TEXT_SEC);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);

        g.gridx = 0;
        g.gridy = 0;
        g.anchor = GridBagConstraints.CENTER;
        card.add(titleLbl, g);
        g.gridy = 1;
        card.add(valueLabel, g);
        return card;
    }

    private JPanel buildForecastRow() {
        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);

        JLabel title = new JLabel("5-Day Forecast");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(TEXT_SEC);
        title.setBorder(new EmptyBorder(0, 4, 0, 0));

        forecastPanel = new JPanel(new GridLayout(1, 5, 10, 0));
        forecastPanel.setOpaque(false);
        for (int i = 0; i < 5; i++) forecastPanel.add(emptyForecastCard());

        wrap.add(title,         BorderLayout.NORTH);
        wrap.add(forecastPanel, BorderLayout.CENTER);
        return wrap;
    }

    private JPanel emptyForecastCard() {
        GlassCard card = new GlassCard();
        card.setPreferredSize(new Dimension(0, 110));
        return card;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 22, 10, 22));

        statusLabel = new JLabel("Ready - enter a city or use location detection");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(TEXT_SEC);

        JLabel credit = new JLabel("Weather data by OpenWeatherMap | Pinnacle Lab Internship");
        credit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        credit.setForeground(new Color(255, 255, 255, 60));

        bar.add(statusLabel, BorderLayout.WEST);
        bar.add(credit,      BorderLayout.EAST);
        return bar;
    }

    private void searchByCity() {
        String city = cityField.getText().trim();
        if (city.isEmpty() || city.equals("Enter city name...")) {
            status("Please enter a city name.", WARN);
            return;
        }
        fetchWeather(city);
    }

    private void autoDetectLocation() {
        status("Detecting your location...", ACCENT);
        setLoading(true);
        new Thread(() -> {
            try {
                String json = get(GEO_URL);
                JSONObject obj = new JSONObject(json);
                if ("success".equals(obj.optString("status"))) {
                    String city    = obj.optString("city", "");
                    String country = obj.optString("countryCode", "");
                    SwingUtilities.invokeLater(() -> {
                        cityField.setText(city);
                        status("Location detected: " + city + ", " + country, SUCCESS);
                    });
                    fetchWeatherAsync(city);
                } else {
                    SwingUtilities.invokeLater(() -> {
                        status("Location detection failed. Enter city manually.", WARN);
                        setLoading(false);
                    });
                }
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    status("Network error: " + ex.getMessage(), WARN);
                    setLoading(false);
                });
            }
        }).start();
    }

    private void fetchWeather(String city) {
        setLoading(true);
        status("Fetching weather for \"" + city + "\"...", ACCENT);
        new Thread(() -> fetchWeatherAsync(city)).start();
    }

    private void fetchWeatherAsync(String city) {
        try {
            String units   = useMetric ? "metric" : "imperial";
            String encCity = URLEncoder.encode(city, "UTF-8");

            String curUrl  = BASE_URL + "weather?q=" + encCity + "&appid=" + API_KEY + "&units=" + units;
            String curJson = get(curUrl);
            JSONObject cur = new JSONObject(curJson);

            if (cur.has("cod") && !String.valueOf(cur.get("cod")).equals("200")) {
                String msg = cur.optString("message", "Unknown error");
                SwingUtilities.invokeLater(() -> {
                    status("API error: " + msg, WARN);
                    setLoading(false);
                });
                return;
            }

            String fcUrl  = BASE_URL + "forecast?q=" + encCity + "&appid=" + API_KEY + "&units=" + units + "&cnt=40";
            String fcJson = get(fcUrl);
            JSONObject fc = new JSONObject(fcJson);

            currentCity = city;
            SwingUtilities.invokeLater(() -> {
                updateCurrentWeather(cur);
                updateForecast(fc);
                setLoading(false);
                status("Weather updated for " + cur.optString("name", city), SUCCESS);
            });

        } catch (Exception ex) {
            SwingUtilities.invokeLater(() -> {
                status("Error: " + ex.getMessage(), WARN);
                setLoading(false);
            });
        }
    }

    private void updateCurrentWeather(JSONObject data) {
        String name    = data.optString("name", "Unknown");
        JSONObject sys = data.optJSONObject("sys");
        String country = sys != null ? sys.optString("country", "") : "";
        cityLabel.setText(name + (country.isEmpty() ? "" : ", " + country));

        JSONObject main = data.optJSONObject("main");
        if (main != null) {
            double temp  = main.optDouble("temp",       0);
            double feels = main.optDouble("feels_like", 0);
            double hum   = main.optDouble("humidity",   0);
            double pres  = main.optDouble("pressure",   0);
            String unit  = useMetric ? "C" : "F";
            tempLabel .setText(String.format("%.0f%s", temp,  unit));
            feelsLabel.setText(String.format("Feels like  %.0f%s", feels, unit));
            humidLabel.setText(String.format("%.0f%%", hum));
            pressLabel.setText(String.format("%.0f hPa", pres));
        }

        JSONObject wind = data.optJSONObject("wind");
        if (wind != null) {
            double spd = wind.optDouble("speed", 0);
            String wu  = useMetric ? " km/h" : " mph";
            if (useMetric) spd *= 3.6;
            windLabel.setText(String.format("%.1f%s", spd, wu));
        }

        int vis = data.optInt("visibility", -1);
        if (vis >= 0) {
            visLabel.setText(useMetric
                ? String.format("%.1f km", vis / 1000.0)
                : String.format("%.1f mi", vis / 1609.34));
        } else {
            visLabel.setText("N/A");
        }

        if (sys != null) {
            long sr = sys.optLong("sunrise", 0) * 1000L;
            long ss = sys.optLong("sunset",  0) * 1000L;
            sunriseLabel.setText(fmtTime(sr));
            sunsetLabel .setText(fmtTime(ss));
        }

        JSONArray weathers = data.optJSONArray("weather");
        if (weathers != null && weathers.length() > 0) {
            JSONObject w    = weathers.getJSONObject(0);
            String desc     = w.optString("description", "");
            String iconCode = w.optString("icon", "");
            descLabel.setText(capitalize(desc));
            weatherIconLabel.setText(weatherTextFor(iconCode));
        }

        unitLabel  .setText(useMetric ? "Metric  (C , km/h)" : "Imperial  (F , mph)");
        lastUpdLabel.setText("Updated: " + DateTimeFormatter.ofPattern("HH:mm").format(LocalDateTime.now()));
    }

    private void updateForecast(JSONObject data) {
        forecastPanel.removeAll();
        JSONArray list = data.optJSONArray("list");
        if (list == null) return;

        Map<String, JSONObject> daily = new LinkedHashMap<>();
        for (int i = 0; i < list.length(); i++) {
            JSONObject entry = list.getJSONObject(i);
            String dt  = entry.optString("dt_txt", "");
            String day = dt.length() >= 10 ? dt.substring(0, 10) : "";
            if (!daily.containsKey(day) && dt.contains("12:00")) daily.put(day, entry);
        }
        if (daily.size() < 5) {
            daily.clear();
            for (int i = 0; i < list.length(); i++) {
                JSONObject entry = list.getJSONObject(i);
                String dt  = entry.optString("dt_txt", "");
                String day = dt.length() >= 10 ? dt.substring(0, 10) : "";
                daily.putIfAbsent(day, entry);
            }
        }

        int count = 0;
        for (Map.Entry<String, JSONObject> e : daily.entrySet()) {
            if (count++ >= 5) break;
            forecastPanel.add(buildForecastCard(e.getKey(), e.getValue()));
        }
        while (forecastPanel.getComponentCount() < 5) forecastPanel.add(emptyForecastCard());
        forecastPanel.revalidate();
        forecastPanel.repaint();
    }

    private JPanel buildForecastCard(String dateStr, JSONObject entry) {
        GlassCard card = new GlassCard();
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 2, 4);
        g.anchor = GridBagConstraints.CENTER;

        String dayName = "-";
        try {
            LocalDate ld = LocalDate.parse(dateStr);
            dayName = ld.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if (ld.equals(LocalDate.now())) dayName = "Today";
        } catch (Exception ignored) {}

        JLabel dayLbl = new JLabel(dayName, SwingConstants.CENTER);
        dayLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        dayLbl.setForeground(TEXT_PRI);

        JSONArray wa       = entry.optJSONArray("weather");
        String    iconCode = wa != null && wa.length() > 0 ? wa.getJSONObject(0).optString("icon", "01d") : "01d";
        JLabel ico = new JLabel(weatherTextFor(iconCode), SwingConstants.CENTER);
        ico.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        ico.setForeground(TEXT_SEC);

        JSONObject main = entry.optJSONObject("main");
        double hi = main != null ? main.optDouble("temp_max", main.optDouble("temp", 0)) : 0;
        double lo = main != null ? main.optDouble("temp_min", main.optDouble("temp", 0)) : 0;
        String unit = useMetric ? "C" : "F";

        JLabel hiLbl = new JLabel(String.format("%.0f%s", hi, unit), SwingConstants.CENTER);
        hiLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
        hiLbl.setForeground(TEXT_PRI);

        JLabel loLbl = new JLabel(String.format("%.0f%s", lo, unit), SwingConstants.CENTER);
        loLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        loLbl.setForeground(TEXT_SEC);

        String desc = wa != null && wa.length() > 0
            ? capitalize(wa.getJSONObject(0).optString("description", "")) : "";
        JLabel descLbl = new JLabel("<html><center>" + desc + "</center></html>", SwingConstants.CENTER);
        descLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        descLbl.setForeground(TEXT_SEC);

        g.gridx = 0; g.gridy = 0; card.add(dayLbl,  g);
        g.gridy = 1;              card.add(ico,      g);
        g.gridy = 2;              card.add(hiLbl,    g);
        g.gridy = 3;              card.add(loLbl,    g);
        g.gridy = 4;              card.add(descLbl,  g);
        return card;
    }

    private void toggleUnits() {
        useMetric = !useMetric;
        unitToggleBtn.setText(useMetric ? "Switch to F" : "Switch to C");
        if (!currentCity.isEmpty()) fetchWeather(currentCity);
    }

    private void setLoading(boolean on) {
        loadingBar.setIndeterminate(on);
        searchBtn.setEnabled(!on);
        locationBtn.setEnabled(!on);
    }

    private void status(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private String get(String urlStr) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(urlStr))
                .timeout(Duration.ofSeconds(10))
                .GET().build();
        HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
        return res.body();
    }

    private String fmtTime(long epochMs) {
        return DateTimeFormatter.ofPattern("HH:mm")
            .format(Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()));
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String weatherTextFor(String code) {
        if (code == null) return "Weather";
        return switch (code.substring(0, Math.min(2, code.length()))) {
            case "01" -> code.endsWith("d") ? "Clear" : "Clear Night";
            case "02" -> "Partly Cloudy";
            case "03" -> "Scattered Clouds";
            case "04" -> "Cloudy";
            case "09" -> "Rain Showers";
            case "10" -> "Rain";
            case "11" -> "Thunderstorm";
            case "13" -> "Snow";
            case "50" -> "Mist";
            default   -> "Weather";
        };
    }

    private JButton glassButton(String text, Color accent) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed()
                    ? accent.darker()
                    : getModel().isRollover()
                        ? accent.brighter()
                        : new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(accent.brighter());
                g2.setStroke(new BasicStroke(1.2f));
                g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 20, 20);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setForeground(TEXT_PRI);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(7, 16, 7, 16));
        return btn;
    }

    private void styleTextField(JTextField f, String placeholder) {
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setForeground(TEXT_SEC);
        f.setBackground(new Color(255, 255, 255, 30));
        f.setCaretColor(Color.WHITE);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(20, CARD_BORDER),
            new EmptyBorder(6, 14, 6, 14)));
        f.setText(placeholder);
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                if (f.getText().equals(placeholder)) { f.setText(""); f.setForeground(TEXT_PRI); }
            }
            public void focusLost(FocusEvent e) {
                if (f.getText().isEmpty()) { f.setText(placeholder); f.setForeground(TEXT_SEC); }
            }
        });
    }

    private JLabel bigLabel(String text, int size, Color color, int style) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", style, size));
        l.setForeground(color);
        return l;
    }

    private JLabel metricLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 15));
        l.setForeground(TEXT_PRI);
        return l;
    }

    static class GradientPanel extends JPanel {
        private final Color top, bot;
        GradientPanel(Color top, Color bot) { this.top = top; this.bot = bot; setOpaque(true); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setPaint(new GradientPaint(0, 0, top, 0, getHeight(), bot));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    static class GlassCard extends JPanel {
        GlassCard() { setOpaque(false); }
        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(CARD_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 18, 18);
            g2.setColor(CARD_BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedBorder implements Border {
        private final int r; private final Color c;
        RoundedBorder(int r, Color c) { this.r = r; this.c = c; }
        public void paintBorder(Component comp, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(c); g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(x, y, w-1, h-1, r, r);
            g2.dispose();
        }
        public Insets getBorderInsets(Component c) { return new Insets(0, 0, 0, 0); }
        public boolean isBorderOpaque() { return false; }
    }

    public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
        SwingUtilities.invokeLater(WeatherApp::new);
    }
}
