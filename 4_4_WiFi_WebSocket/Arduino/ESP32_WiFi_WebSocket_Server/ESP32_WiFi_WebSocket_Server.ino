#include<WebSocketServer.h>
#include <WiFi.h>

const char* ssid     = "your_ssid";
const char* password = "your_pass";

// 서버 생성시 연결될 포트 지정
WiFiServer server(80);
WebSocketServer webSocketServer;

void setup() {
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
    
    startServer();
}

// 서버 시작
void startServer() {
    Serial.println("Server start");
    server.begin();
}

void loop() {
    // 클라이언트 연결 대기
    WiFiClient client = server.available();

    // 클라이언트가 연결되면 파일 전송 시작
    if(client.connected() && webSocketServer.handshake(client)) {
        String data;
        while(client.connected()) {
            data = webSocketServer.getData();
            if(data.length() > 0) {
                Serial.println("received: "+data);
                webSocketServer.sendData("send back - "+data); 
            }
            delay(10);
        }
        Serial.println("The client is disconnected");
        delay(100);
    }
    delay(100);
}
