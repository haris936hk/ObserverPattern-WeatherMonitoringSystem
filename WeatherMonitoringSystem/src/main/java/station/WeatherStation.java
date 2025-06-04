package station;
import interfaces.Observer;
import interfaces.Subject;
import java.util.ArrayList;
import java.util.List;

public class WeatherStation implements Subject {
    private final List<Observer> observers;
    private float temperature;
    private float humidity;
    private float pressure;
    public WeatherStation() {
        observers = new ArrayList<>();
    }
    @Override
    public void registerObserver(Observer o) {
        if (o == null) return;
        if (!observers.contains(o)) {
            observers.add(o);
        }
    }
    @Override
    public void removeObserver(Observer o) {
        observers.remove(o);
    }
    @Override
    public void notifyObservers() {
        for (Observer o : observers) {
            o.update(temperature, humidity, pressure);
        }
    }
    public void setMeasurements(float temperature, float humidity, float pressure) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.pressure = pressure;
        measurementsChanged();
    }
    private void measurementsChanged() {
        notifyObservers();
    }
}
