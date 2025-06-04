package displays;
import interfaces.Observer;
import interfaces.Subject;
import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class CurrentConditionsDisplay extends JPanel implements Observer {
    private final JLabel tempValueLabel;
    private final JLabel humValueLabel;
    private final DecimalFormat df = new DecimalFormat("#.##");
    public CurrentConditionsDisplay(Subject station) {
        station.registerObserver(this);
        setLayout(new GridLayout(2, 2, 10, 5));
        setBorder(BorderFactory.createTitledBorder("Current Conditions"));
        JLabel tempLabel = createTitleLabel("Temperature:");
        JLabel humLabel  = createTitleLabel("Humidity:");
        tempValueLabel = createValueLabel("--");
        humValueLabel  = createValueLabel("--");
        add(tempLabel);
        add(tempValueLabel);
        add(humLabel);
        add(humValueLabel);
    }
    @Override
    public void update(float temperature, float humidity, float pressure) {
        final String tempText = df.format(temperature) + " °C";
        final String humText  = df.format(humidity)    + " %";

        SwingUtilities.invokeLater(() -> {
            tempValueLabel.setText(tempText);
            humValueLabel.setText(humText);
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
