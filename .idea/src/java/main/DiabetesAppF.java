
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.*;
import java.awt.Toolkit;
import java.util.Timer;

public class DiabetesAppF {
    private static boolean insulinTaken = false;
    private static final String THINGSPEAK_API_KEY = "CIT4RWXB271CI47S";
    private static final String THINGSPEAK_CHANNEL_URL = "https://api.thingspeak.com/channels/2671583/fields/1.json?api_key=CIT4RWXB271CI47S" + THINGSPEAK_API_KEY;

    private static Timer shortIntervalTimer1 = new Timer();
    private static Timer shortIntervalTimer2 = new Timer();
    private static Timer shortIntervalTimer3 = new Timer();

    public static void main(String[] args) {
        // Create JFrame for the window
        JFrame frame = new JFrame("Diabetes App");
        frame.setSize(400, 200);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Label to show insulin status
        JLabel label = new JLabel("Insulin not taken.");
        label.setBounds(50, 50, 300, 20);
        frame.add(label);

        // Button to mark insulin as taken
        JButton button = new JButton("Take Insulin");
        button.setBounds(100, 100, 150, 30);
        frame.add(button);

        // Button action listener
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                insulinTaken = true;
                label.setText("Insulin taken.");
                saveToDatabase();
                cancelAlarms(); // Cancel alarms when insulin is taken
            }
        });

        // Set layout and make the window visible
        frame.setLayout(null);
        frame.setVisible(true);

        // Fetch data from ThingSpeak
        fetchThingSpeakData();

        // Schedule reminders at short intervals (1 minute apart)
        scheduleShortIntervalReminders();
    }

    // Method to fetch data from ThingSpeak
    private static void fetchThingSpeakData() {
        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(THINGSPEAK_CHANNEL_URL)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful()) {
                String jsonResponse = response.body().string();
                System.out.println("Fetched Data from ThingSpeak: " + jsonResponse);
            } else {
                System.out.println("Failed to fetch data from ThingSpeak. Response code: " + response.code());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Method to save insulin data to SQLite database
    private static void saveToDatabase() {
        try {
            Connection connection = DriverManager.getConnection("jdbc:sqlite:diabetes.db");
            Statement statement = connection.createStatement();

            // Create table for insulin tracking if not exists
            String sql = "CREATE TABLE IF NOT EXISTS InsulinLog (id INTEGER PRIMARY KEY AUTOINCREMENT, date TEXT, dosage INTEGER)";
            statement.execute(sql);

            // Insert a sample record
            String insertSql = "INSERT INTO InsulinLog (date, dosage) VALUES (CURRENT_TIMESTAMP, 10)";
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

