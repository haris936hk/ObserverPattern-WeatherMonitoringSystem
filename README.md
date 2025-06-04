# Weather Monitoring System

A Java Swing application demonstrating the **Observer Design Pattern** with real-time weather updates via OpenWeatherMap APIs. The app allows users to search for a city, displays current conditions, forecasts, and statistics, and enables dynamic observer registration/removal at runtime.

## 🌟 Features

### Observer Design Pattern
- `WeatherStation` (Subject) notifies registered display panels (Observers) of updates.
- Panels:
  - **CurrentConditionsDisplay** – Shows temperature and humidity.
  - **StatisticsDisplay** – Tracks average, min, and max temperatures.
  - **ForecastDisplay** – Predicts weather trends based on pressure.

### Dynamic Observer Management
- Register/unregister panels at runtime via checkboxes.
- Panels update instantly when active.

### Swing GUI with Nimbus Look & Feel
- Responsive layout with city autocomplete, refresh button, and status messages.

### Weather Polling
- Automatic data refresh every 10 seconds.
- Manual refresh button available.

### City Search with Autocomplete
- Uses OpenWeatherMap Geocoding API to suggest cities.

## 🧰 Requirements

- **Java 11+**
- **org.json** library (via Maven or manual JAR)
- Internet connection (for API calls)

## 🚀 Getting Started

### 1. Clone the Repository

```bash
git clone https://github.com/haris936hk/WeatherMonitoringSystem.git
cd WeatherMonitoringSystem
```

### 2. Obtain OpenWeatherMap API Key
- Register at [openweathermap.org](https://openweathermap.org)
- Replace `API_KEY` in `WeatherApp.java`:

```java
private static final String API_KEY = "YOUR_KEY_HERE";
```

### 3. Add JSON Library

**Using Maven:**
```xml
<dependency>
  <groupId>org.json</groupId>
  <artifactId>json</artifactId>
  <version>20230227</version>
</dependency>
```

**Manual:** Download `json-20230227.jar` and place it under `lib/`

## 🖥️ Running the Application

**Compile:**
```bash
javac -cp ".:lib/json-20230227.jar" src/**/*.java
```

**Run:**
```bash
java -cp ".:lib/json-20230227.jar" WeatherApp
```

*(Use `;` instead of `:` on Windows)*

## 📁 Project Structure

```
WeatherMonitoringSystem/
├─ lib/
│   └─ json-20230227.jar
├─ src/
│   ├─ interfaces/
│   │   ├─ Observer.java
│   │   └─ Subject.java
│   ├─ station/
│   │   └─ WeatherStation.java
│   ├─ displays/
│   │   ├─ CurrentConditionsDisplay.java
│   │   ├─ StatisticsDisplay.java
│   │   └─ ForecastDisplay.java
│   └─ WeatherApp.java
└─ README.md
```

## 🔁 Observer Pattern Overview

### Subject: `WeatherStation`
- Stores list of observers
- `registerObserver`, `removeObserver`, `notifyObservers`

### Observers
- Implement `Observer` interface
- Automatically update their UI upon receiving data

## 🧪 Dynamic Registration Demo

Users can toggle observer panels at runtime using checkboxes:

| Checkbox | Action | 
|----------|--------|
| Show Current | Toggle `CurrentConditionsDisplay` |
| Show Statistics | Toggle `StatisticsDisplay` |
| Show Forecast | Toggle `ForecastDisplay` |

Unchecking removes the observer (via `removeObserver`) and hides its panel. Re-checking re-registers the observer and shows it again.

## 🕹️ Usage

1. Launch the app.
2. Search for a city.
3. Wait for automatic updates or click **Refresh**.
4. Use checkboxes to enable/disable observer panels.
5. Click **Quit** to exit.

## 📄 License

MIT License © 2025

Created as part of a software engineering coursework to demonstrate the Observer Design Pattern in a user-friendly Java Swing interface.
