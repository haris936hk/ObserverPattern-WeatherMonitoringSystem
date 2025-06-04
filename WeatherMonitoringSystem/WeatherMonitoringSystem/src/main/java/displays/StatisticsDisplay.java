package displays;
import interfaces.Observer;
import interfaces.Subject;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class StatisticsDisplay extends JPanel implements Observer {
    private float maxTemp = Float.MIN_VALUE;
    private float minTemp = Float.MAX_VALUE;
    private float tempSum = 0.0f;
    private int numReadings = 0;
    private final JLabel avgValueLabel;
    private final JLabel maxValueLabel;
    private final JLabel minValueLabel;
    private final DecimalFormat df = new DecimalFormat("#.##");
    public StatisticsDisplay(Subject station) {
        station.registerObserver(this);
        setLayout(new GridLayout(3, 2, 10, 5));
        setBorder(BorderFactory.createTitledBorder("Temperature Statistics"));
        JLabel avgLabel = createTitleLabel("Avg Temp:");
        JLabel maxLabel = createTitleLabel("Max Temp:");
        JLabel minLabel = createTitleLabel("Min Temp:");
        avgValueLabel = createValueLabel("--");
        maxValueLabel = createValueLabel("--");
        minValueLabel = createValueLabel("--");
        add(avgLabel);
        add(avgValueLabel);
        add(maxLabel);
        add(maxValueLabel);
        add(minLabel);
        add(minValueLabel);
    }
    @Override
    public void update(float temperature, float humidity, float pressure) {
        tempSum += temperature;
        numReadings++;
        if (temperature > maxTemp) {
            maxTemp = temperature;
        }
        if (temperature < minTemp) {
            minTemp = temperature;
        }
        float avg = tempSum / numReadings;
        final String avgText = df.format(avg) + " °C";
        final String maxText = df.format(maxTemp) + " °C";
        final String minText = df.format(minTemp) + " °C";

        SwingUtilities.invokeLater(() -> {
            avgValueLabel.setText(avgText);
            maxValueLabel.setText(maxText);
            minValueLabel.setText(minText);
        });
    }
    private JLabel createTitleLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("SansSerif", Font.BOLD, 14));
        return label;
    }
    private JLabel createValueLabel(String initialText) {
        JLabel label = new JLabel(initialText);
        label.setFont(new Font("SansSerif", Font.PLAIN, 14));
        label.setPreferredSize(new Dimension(80, 20));
        return label;
    }
}
