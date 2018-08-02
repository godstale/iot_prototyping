#include <WiFi.h>
#include <WebSocketClient.h>

WebSocketClient webSocketClient;
WiFiClient client;

const char* ssid     = "your_ssid";
const char* password = "your_pass";

char path[] = "/";
char host[] = "192.168.1.9";    // 웹소켓 서버 주소

int count = 0;

void setup()
{
    Serial.begin(115200);
    delay(10);
    
    Serial.println();
    Serial.println();
    Serial.print("Connecting to ");
    Serial.println(ssid);

    WiFi.begin(ssid, password);

    // 와이파이망에 연결
    while (WiFi.status() != WL_CONNECTED) {
        delay(500);
        Serial.print(".");
    }

    Serial.println("");
    Serial.println("WiFi connected");
    Serial.println("IP address: ");
    Serial.println(WiFi.localIP());  
    delay(5000);

    // 서버에 연결
    if (client.connect(host, 80)) {
        Serial.println("Connected");
    } else {
        Serial.println("Connection failed.");
    }
    delay(1000); 

    webSocketClient.path = path;
    webSocketClient.host = host;
 
    if (webSocketClient.handshake(client)) {
        Serial.println("Handshake successful");
    } else {
        Serial.println("Handshake failed.");
    }
}

void loop() {
  delay(1000);
  String data;

  if (client.connected()) {
    // 데이터 전송
    webSocketClient.sendData("msg count : "+String(count++));
    // 데이터 수신
    webSocketClient.getData(data);
    if (data.length() > 0) {
      Serial.println(data);
    }
  } else {
    Serial.println("Client disconnected.");
  }
 
  delay(3000);
}
