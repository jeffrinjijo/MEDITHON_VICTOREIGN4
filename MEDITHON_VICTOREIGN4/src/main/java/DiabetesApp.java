import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class DiabetesApp {
    private static boolean insulinTaken = false;
    private static final int REQUIRED_DOSAGE = 20; // The required insulin dosage
    private static final String THINGSPEAK_API_KEY = "K3N1WQXO6H59IFMP";
    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/2675380/feeds.json?api_key=" + THINGSPEAK_API_KEY + "&results=1";

    private static Timer shortIntervalTimer1 = new Timer();
    private static Timer shortIntervalTimer2 = new Timer();
    private static Timer shortIntervalTimer3 = new Timer();

    // UI components for sensor data
    private static JLabel flowRateLabel;
    private static JLabel tempLabel;
    private static JLabel humidityLabel;

    public static void main(String[] args) {
        // Create JFrame for the window
        JFrame frame = new JFrame("Diabetes App");
        frame.setSize(400, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Label to show insulin status
        JLabel label = new JLabel("Insulin not taken.");
        label.setBounds(50, 50, 300, 20);
        frame.add(label);

        // Label to show dosage input
        JLabel dosageLabel = new JLabel("Enter Insulin Dosage:");
        dosageLabel.setBounds(50, 100, 150, 20);
        frame.add(dosageLabel);

        // Text field for dosage input
        JTextField dosageField = new JTextField();
        dosageField.setBounds(200, 100, 100, 20);
        frame.add(dosageField);

        // Button to mark insulin as taken
        JButton button = new JButton("Submit Dosage");
        button.setBounds(100, 150, 150, 30);
        frame.add(button);

        // Button action listener
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the dosage entered by the user
                String dosageText = dosageField.getText();
                try {
                    int enteredDosage = Integer.parseInt(dosageText);

                    // Check if the dosage is exactly 20 units
                    if (enteredDosage == REQUIRED_DOSAGE) {
                        insulinTaken = true;
                        label.setText("Insulin taken correctly.");
                        saveToDatabase(enteredDosage);
                        cancelAlarms(); // Cancel alarms when insulin is taken
                    } else {
                        label.setText("Incorrect dosage! Please enter exactly 20 units.");
                    }
                } catch (NumberFormatException ex) {
                    label.setText("Invalid input! Please enter a valid number.");
                }
            }
        });

        // Labels to display sensor data
        flowRateLabel = new JLabel("Flow Rate: ");
        flowRateLabel.setBounds(50, 180, 300, 20);
        frame.add(flowRateLabel);

        tempLabel = new JLabel("Temperature: ");
        tempLabel.setBounds(50, 210, 300, 20);
        frame.add(tempLabel);

        humidityLabel = new JLabel("Humidity: ");
        humidityLabel.setBounds(50, 240, 300, 20);
        frame.add(humidityLabel);

        // Set layout and make the window visible
        frame.setLayout(null);
        frame.setVisible(true);

        // Fetch data from ThingSpeak and schedule periodic updates
        scheduleDataFetching();

        // Schedule reminders at short intervals (1-minute intervals for testing)
        scheduleShortIntervalReminders();
    }

    // Method to fetch data from ThingSpeak and display sensor values on the UI
    private static void fetchThingSpeakData() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(THINGSPEAK_CHANNEL_URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                System.out.println("Fetched Data from ThingSpeak: " + jsonResponse);

                // Parse the JSON response
                JSONObject json = new JSONObject(jsonResponse);
                JSONArray feeds = json.getJSONArray("feeds");
                JSONObject latestEntry = feeds.getJSONObject(0);

                float flowRate = latestEntry.getFloat("field1");
                int proximityValue = latestEntry.getInt("field2");
                int forceValue = latestEntry.getInt("field3");
                float temperature = latestEntry.getFloat("field4");
                float humidity = latestEntry.getFloat("field5");

                // Update the UI with the sensor values
                flowRateLabel.setText("Flow Rate: " + flowRate + " L/min");
                tempLabel.setText("Temperature: " + temperature + "°C");
                humidityLabel.setText("Humidity: " + humidity + "%");

            } else {
                System.out.println("Failed to fetch data from ThingSpeak. Response code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Schedule data fetching from ThingSpeak every minute
    private static void scheduleDataFetching() {
        Timer fetchTimer = new Timer();
        fetchTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                fetchThingSpeakData();  // Fetch data from ThingSpeak and update UI
            }
        }, 0, 60000); // Fetch every 60 seconds
    }

    // Method to save insulin data to SQLite database
    private static void saveToDatabase(int dosage) {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:diabetes.db");
            Statement statement = connection.createStatement();

            // Create table for insulin tracking if not exists
            String sql = "CREATE TABLE IF NOT EXISTS InsulinLog (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, dosage INTEGER)";
            statement.execute(sql);

            // Insert a record with current timestamp and the dosage
            String insertSql = "INSERT INTO InsulinLog (date, dosage) VALUES (CURRENT_TIMESTAMP, " + dosage + ")";
            statement.execute(insertSql);

            statement.close();
            connection.close();

            System.out.println("Insulin data saved to the database.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Method to schedule short interval reminders (1-minute intervals for testing)
    private static void scheduleShortIntervalReminders() {
        Calendar now = Calendar.getInstance();

        // Schedule first reminder 1 minute from now
        Calendar firstReminder = (Calendar) now.clone();
        firstReminder.add(Calendar.MINUTE, 1);
        scheduleAlarm(firstReminder, "Time to take your first insulin!", shortIntervalTimer1);

        // Schedule second reminder 2 minutes from now
        Calendar secondReminder = (Calendar) now.clone();
        secondReminder.add(Calendar.MINUTE, 2);
        scheduleAlarm(secondReminder, "Time to take your second insulin!", shortIntervalTimer2);

        // Schedule third reminder 3 minutes from now
        Calendar thirdReminder = (Calendar) now.clone();
        thirdReminder.add(Calendar.MINUTE, 3);
        scheduleAlarm(thirdReminder, "Time to take your third insulin!", shortIntervalTimer3);
    }

    // Method to schedule an alarm at a specific time
    private static void scheduleAlarm(Calendar alarmTime, String message, Timer timer) {
        long delay = alarmTime.getTimeInMillis() - Calendar.getInstance().getTimeInMillis();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!insulinTaken) {
                    triggerAlarm(message);
                }
            }
        }, delay);  // No repeat needed for short-interval testing
    }

    // Method to trigger an alarm (sound and popup)
    private static void triggerAlarm(String message) {
        Toolkit.getDefaultToolkit().beep();  // System beep sound
        JOptionPane.showMessageDialog(null, message, "Insulin Reminder", JOptionPane.WARNING_MESSAGE);
    }

    // Method to cancel all alarms when insulin is taken
    private static void cancelAlarms() {
        shortIntervalTimer1.cancel();
        shortIntervalTimer2.cancel();
        shortIntervalTimer3.cancel();
        resetTimers(); // Reset timers in case we need to schedule future reminders
        System.out.println("Alarms cancelled.");
    }

    // Reset timers so they can be used again
    private static void resetTimers() {
        shortIntervalTimer1 = new Timer();
        shortIntervalTimer2 = new Timer();
        shortIntervalTimer3 = new Timer();
    }
}
