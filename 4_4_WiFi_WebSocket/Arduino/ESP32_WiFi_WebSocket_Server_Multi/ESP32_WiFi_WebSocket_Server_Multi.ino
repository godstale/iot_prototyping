/* 
  Based on WiFiTelnetToSerial
*/
#include <WiFi.h>
#include <WebSocketServer.h>//https://github.com/morrissinger/ESP8266-Websocket

//how many clients should be able to WebSocket to this ESP8266
#define MAX_SRV_CLIENTS 10

const char* ssid = "your_ssid";
const char* password = "your_pass";

String inputString = "";         // a string to hold incoming data
boolean stringComplete = false;  // whether the string is complete

WiFiServer server(80);
WebSocketServer webSocketServer;
WiFiClient serverClients[MAX_SRV_CLIENTS];

void setup() {
  Serial.begin(115200);
  WiFi.begin(ssid, password);
  
  Serial.print("\nConnecting to "); Serial.println(ssid);
  uint8_t i = 0;
  
  while (WiFi.status() != WL_CONNECTED && i++ < 20) delay(500);
  if(i == 21){
    Serial.print("Could not connect to"); Serial.println(ssid);
    while(1) delay(500);
  }
  
  //start UART and the server
  server.begin();
  server.setNoDelay(true);
  
  Serial.print("Ready! Use 'telnet ");
  Serial.print(WiFi.localIP());
  Serial.println(" 80' to connect");
}

void loop() {
  uint8_t i;
  if (server.hasClient()){
    for(i = 0; i < MAX_SRV_CLIENTS; i++){
      if (!serverClients[i] || !serverClients[i].connected()){
        if(serverClients[i]) serverClients[i].stop();
        serverClients[i] = server.available();
        while(!serverClients[i].available())
        {
          //Serial.println("wait");
        }
        webSocketServer.handshake(serverClients[i]);
        Serial.print("New client: "); Serial.println(i);
        break;
      }
    }
    
    WiFiClient serverClient = server.available();
    serverClient.stop();
  }
  //check clients for data
  for(i = 0; i < MAX_SRV_CLIENTS; i++){
    if (serverClients[i] && serverClients[i].connected()){
      if(serverClients[i].available()){
        while(serverClients[i].available()){
          String data = webSocketServer.getData(serverClients[i]);
          if (data.length() > 0) {
            Serial.print("receive Data from client ");
            Serial.print(i);
            Serial.print(":");
            Serial.println(data);
            //ECHO to the client:
            webSocketServer.sendData("you sent :"+data, serverClients[i]);
          }
        }
      }
    }
  }

  while (Serial.available()) {
    // get the new byte:
    char inChar = (char)Serial.read();
    // add it to the inputString:
    inputString += inChar;
    // if the incoming character is a newline, set a flag
    // so the main loop can do something about it:
    if (inChar == '\n') {
      for(i = 0; i < MAX_SRV_CLIENTS; i++){
        if (serverClients[i] && serverClients[i].connected()){
          webSocketServer.sendData(inputString, serverClients[i]);
          delay(1);
        }
      }

      inputString = "";
    }
  }
}
