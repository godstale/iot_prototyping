#include <WiFi.h>
#include <HTTPClient.h>
#include <PubSubClient.h>
#include <sstream>
#include "DHT.h"

// Uncomment one of the lines below for whatever DHT sensor type you're using!
#define DHTTYPE DHT11   // DHT 11
//#define DHTTYPE DHT21   // DHT 21 (AM2301)
//#define DHTTYPE DHT22   // DHT 22  (AM2302), AM2321

// DHT Sensor
const int DHTPin = 16;

// Initialize DHT sensor.
DHT dht(DHTPin, DHTTYPE);

// Network settings
const char* ssid = "your_ssid";
const char* password =  "your_pass";

// Server info
#define SERVER_POST_URL "http://your_server:port_num/data"
#define SERVER_CHANNEL 3
#define SERVER_AUTHCODE "authcode"
#define HTTP_POST_INTERVAL 10000L

// MQTT info
const char* mqttServer = "your_server"; // do not use "http://"
const int mqttPort = 1883;
const char* mqttUser = "";
const char* mqttPassword = "";
const char* topic_sub = "3/status";   // 3 is channel number.

WiFiClient espClient;
PubSubClient mqttClient(espClient);

// Global
boolean sensorPower = true;
unsigned long prevPostTime = 0L;


/**
 * Connect to WiFi network
 */
void connectWiFi() {
  while (WiFi.status() != WL_CONNECTED) { //Check for the connection
    delay(1000);
    Serial.println("Connecting to WiFi..");
  }
 
  Serial.println("WiFi connected");
  Serial.println("IP address: ");
  Serial.println(WiFi.localIP());
}

/**
 * Connect to MQTT server
 */
void connectMqtt() {
  while (!mqttClient.connected()) {
    Serial.println("Connecting to MQTT...");
    
    if (mqttClient.connect("Temperature sensor 1", mqttUser, mqttPassword )) {
      Serial.println("connected");
    } else {
      Serial.print("failed with state ");
      Serial.print(mqttClient.state());
      delay(2000);
    }
  }

  delay(1000);
  mqttClient.subscribe(topic_sub);
  mqttClient.publish(topic_sub, "ESP32 logged in");
}

/**
 * MQTT subscribe callback
 */
void mqttCallback(char* topic, byte* payload, unsigned int length) {
  Serial.println("MQTT start -----------------------");
  Serial.print("Command arrived in topic: ");
  Serial.println(topic_sub);
 
  Serial.print("Command: ");
  for (int i = 0; i < length; i++) {
    if(payload[i] == 0x30) {          // Char '0'
      sensorPower = false;
      Serial.print("[Sensor off], ");
    }
    else if(payload[i] == 0x31) {     // Char '1'
      sensorPower = true;
      Serial.print("[Sensor on], ");
    } else {
      Serial.print("0x");
      Serial.print(payload[i], HEX);
      Serial.print(", ");
    }
  }
 
  Serial.println();
  Serial.println("MQTT end -----------------------");
}


void setup() {
  Serial.begin(115200);
  // initialize the DHT sensor
  dht.begin();
  
  delay(2000);   //Delay needed before calling the WiFi.begin
 
  WiFi.begin(ssid, password); 
  connectWiFi();

  mqttClient.setServer(mqttServer, mqttPort);
  mqttClient.setCallback(mqttCallback);
  
  connectMqtt();
}

 
void loop() {
  if(WiFi.status() == WL_CONNECTED) {   //Check WiFi connection status
    mqttClient.loop();

    unsigned long currentTime = millis();
    
    if(sensorPower && currentTime > prevPostTime + HTTP_POST_INTERVAL) {
      // Sensor readings may also be up to 2 seconds 'old' (its a very slow sensor)
      float h = dht.readHumidity();
      // Read temperature as Celsius (the default)
      float t = dht.readTemperature();
      // Read temperature as Fahrenheit (isFahrenheit = true)
      float f = dht.readTemperature(true);
      
      // Check if any reads failed and exit early (to try again).
      if(isnan(h) || isnan(t) || isnan(f)) {
        Serial.println("Failed to read from DHT sensor!");
      } else {
        Serial.println("HTTP start --------------------------------");
        HTTPClient http;
     
        http.begin(SERVER_POST_URL);  //Specify destination for HTTP request
        http.addHeader("Content-Type", "application/json");             //Specify content-type header
  
        // Make JSON string
        std::stringstream ss;
        ss << "{\"channel\":" << SERVER_CHANNEL << ",\"auth_code\":\"" << SERVER_AUTHCODE << "\",\"data\":" << t << "}";
  
        // send HTTP POST request
        Serial.println(ss.str().c_str());
        int httpResponseCode = http.POST(ss.str().c_str());   //Send the actual POST request
  
        if(httpResponseCode > 0){
          String response = http.getString();                       //Get the response to the request
          Serial.println(httpResponseCode);   //Print return code
          Serial.println(response);           //Print request answer
        } else {
          Serial.print("Error on sending POST: ");
          Serial.println(httpResponseCode);
        }
        http.end();  //Free resources
        Serial.println("HTTP end --------------------------------");
      }
      prevPostTime = currentTime;
    }  // End of if(sensorPower)
 
  } else {
    Serial.println("Error in WiFi connection... Try to reconnect WiFi and MQTT.");   
    connectWiFi();
    connectMqtt();
  }
}

