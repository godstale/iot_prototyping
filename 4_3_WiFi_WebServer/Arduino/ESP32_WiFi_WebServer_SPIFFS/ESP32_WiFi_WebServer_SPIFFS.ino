//
// A simple server implementation showing how to:
//  * serve HTML and resources in SPIFFS
//  * handle missing pages / 404s
//

#include <FS.h>
#include <SPIFFS.h>
#include <WiFi.h>
#include <AsyncTCP.h>
#include <ESPAsyncWebServer.h>

AsyncWebServer server(80);

const char* ssid = "your_ssid";
const char* password = "your_pass";


void notFound(AsyncWebServerRequest *request) {
    request->send(404, "text/plain", "Not found");
}



void setup() {

    Serial.begin(115200);
    SPIFFS.begin(true);
    
    WiFi.mode(WIFI_STA);
    WiFi.begin(ssid, password);
    if (WiFi.waitForConnectResult() != WL_CONNECTED) {
        Serial.printf("WiFi Failed!\n");
        return;
    }

    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());

    server.on("/", HTTP_GET, [](AsyncWebServerRequest *request){
        request->send(SPIFFS, "/index.html");
    });

    server.on("/image/bg.jpg", HTTP_GET, [](AsyncWebServerRequest *request){
        request->send(SPIFFS, "/image/bg.jpg");
    });

    server.on("/image/member.jpg", HTTP_GET, [](AsyncWebServerRequest *request){
        request->send(SPIFFS, "/image/member.jpg");
    });

    server.onNotFound(notFound);
    server.begin();
}



void loop() {
}
