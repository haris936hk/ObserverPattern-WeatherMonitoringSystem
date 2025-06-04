package displays;
import interfaces.Observer;
import interfaces.Subject;
import javax.swing.*;
import java.awt.*;

public class ForecastDisplay extends JPanel implements Observer {
    private float lastPressure;
    private float currentPressure;
    private boolean isFirstUpdate = true;
    private final JLabel forecastLabel;
    public ForecastDisplay(Subject station) {
        station.registerObserver(this);
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Forecast"));
        forecastLabel = new JLabel("--");
        forecastLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        forecastLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(forecastLabel, BorderLayout.CENTER);
    }
    @Override
    public void update(float temperature, float humidity, float pressure) {
        String forecastText;
        if (isFirstUpdate) {
            isFirstUpdate    = false;
            lastPressure     = pressure;
            currentPressure  = pressure;
            forecastText     = "No forecast yet";
        } else {
            lastPressure    = currentPressure;
            currentPressure = pressure;
            if (currentPressure > lastPressure) {
                forecastText = "Improving weather on the way!";
            } else if (currentPressure == lastPressure) {
                forecastText = "More of the same.";
            } else {
                forecastText = "Watch out for cooler/rainy weather.";
            }
        }
        final String textToShow = forecastText;
        SwingUtilities.invokeLater(() -> forecastLabel.setText(textToShow));
    }
}
