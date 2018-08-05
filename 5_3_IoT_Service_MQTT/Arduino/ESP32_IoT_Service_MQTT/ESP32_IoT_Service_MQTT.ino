#include <WiFi.h>
#include <PubSubClient.h>
#include <sstream>      // std::stringstream
 
const char* ssid = "your_ssid";
const char* password =  "your_pass";
const char* mqttServer = "192.168.1.9";
const int mqttPort = 1883;
const char* mqttUser = "yourMQTTuser";
const char* mqttPassword = "yourMQTTpassword";
const char* topic_pub = "messagebox";
const char* topic_sub = "messagebox2";

#define UPDATE_INTERVAL 5000L
unsigned long prevUpdateTime = 0L;

WiFiClient espClient;
PubSubClient mqttClient(espClient);


void callback(char* topic, byte* payload, unsigned int length) {
  Serial.print("Message arrived in topic: ");
  Serial.println(topic_sub);
 
  Serial.print("Message: ");
  for (int i = 0; i < length; i++) {
    Serial.print((char)payload[i]);
  }
 
  Serial.println();
  Serial.println("-----------------------");
}


void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
 
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.println("Connecting to WiFi..");
  }
  Serial.println("Connected to the WiFi network");
 
  mqttClient.setServer(mqttServer, mqttPort);
  mqttClient.setCallback(callback);
 
  while (!mqttClient.connected()) {
    Serial.println("Connecting to MQTT...");
    
    if (mqttClient.connect("ESP32Client", mqttUser, mqttPassword )) {
      Serial.println("connected");
    } else {
      Serial.print("failed with state ");
      Serial.print(mqttClient.state());
      delay(2000);
    }
  }

  mqttClient.subscribe(topic_sub);
  mqttClient.publish(topic_pub, "ESP32 logged in");
  prevUpdateTime = millis();
}

void loop() {
  mqttClient.loop();

  unsigned long currentTime = millis();
  if(currentTime > prevUpdateTime + UPDATE_INTERVAL) {
    int i = random(100);
    std::stringstream st;
    st<<i;
    mqttClient.publish(topic_pub, st.str().c_str());
    prevUpdateTime = currentTime;
  }
}
