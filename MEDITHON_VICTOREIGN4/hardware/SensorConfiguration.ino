#include <WiFi.h>
#include <ThingSpeak.h>
#include <DHT.h>

// ThingSpeak settings
const char* ssid = "Ruban"; // Your WiFi SSID
const char* password = "Ruban125"; // Your WiFi password
unsigned long channelID = 2675875; // ThingSpeak Channel ID
const char* writeAPIKey = "HKORNH8SIVUVSD5A";  // ThingSpeak Write API Key

WiFiClient client;

// Sensor pins
const int flowPin = 15;      // Flow Sensor Pin
const int proximityPin = 34; // Optical Sensor Pin
const int forcePin = 35;     // Force Sensor Pin
const int DHTPin = 4;        // DHT11 sensor pin

// Variables for storing sensor data
volatile int pulseCount = 0;  // Flow sensor pulse count
float flowRate = 0.0;
int proximityValue = 0;
int forceValue = 0;
float temperature = 0.0;
float humidity = 0.0;

// DHT sensor settings
#define DHTTYPE DHT11 // DHT 11 sensor type
DHT dht(DHTPin, DHTTYPE);

// Interrupt function to count flow sensor pulses
void IRAM_ATTR pulseCounter() {
  pulseCount++;
}

void setup() {
  Serial.begin(115200);

  // Connect to Wi-Fi
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("Connected to WiFi");

  // Initialize ThingSpeak
  ThingSpeak.begin(client);

  // Initialize DHT sensor
  dht.begin();

  // Attach interrupt for flow sensor
  pinMode(flowPin, INPUT);
  attachInterrupt(digitalPinToInterrupt(flowPin), pulseCounter, RISING);
}

void loop() {
  // Flow sensor calculation
  flowRate = (pulseCount / 7.5);  // Example: Flow rate calculation based on sensor spec
  pulseCount = 0;  // Reset pulse count

  // Read optical sensor (proximity)
  proximityValue = analogRead(proximityPin);

  // Read force sensor (injection pressure)
  forceValue = analogRead(forcePin);

  // Read DHT11 sensor data (temperature and humidity)
  humidity = dht.readHumidity();
  temperature = dht.readTemperature();

  // Check if any reads failed from DHT11
  if (isnan(humidity) || isnan(temperature)) {
    Serial.println("Failed to read from DHT sensor!");
    return;
  }

  // Send data to ThingSpeak
  ThingSpeak.setField(1, flowRate);
  ThingSpeak.setField(2, proximityValue);
  ThingSpeak.setField(3, forceValue);
  ThingSpeak.setField(4, temperature);
  ThingSpeak.setField(5, humidity);

  int responseCode = ThingSpeak.writeFields(channelID, writeAPIKey);
  if (responseCode == 200) {
    Serial.println("Data uploaded successfully");
  } else {
    Serial.println("Error uploading data");
  }

  // Delay between updates (e.g., every 15 seconds)
  delay(15000);
}