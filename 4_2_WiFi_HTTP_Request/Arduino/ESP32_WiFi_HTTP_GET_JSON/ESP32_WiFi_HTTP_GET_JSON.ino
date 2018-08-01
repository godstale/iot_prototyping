#include <WiFi.h>
#include <HTTPClient.h>
#include <ArduinoJson.h>
 
const char* ssid = "yourNetworkName";
const char* password =  "yourPassword";
 
void setup() {
 
  Serial.begin(115200);
  delay(4000);
  WiFi.begin(ssid, password);
 
  while (WiFi.status() != WL_CONNECTED) {
    delay(1000);
    Serial.println("Connecting to WiFi..");
  }
 
  Serial.println("Connected to the WiFi network");
 
}
 
void loop() {
 
  if ((WiFi.status() == WL_CONNECTED)) { //Check the current connection status
 
    HTTPClient http;
 
    http.begin("http://jsonplaceholder.typicode.com/comments?id=10"); //Specify the URL
    int httpCode = http.GET();                                        //Make the request
 
    if (httpCode > 0) { //Check for the returning code
      String payload = http.getString();

      StaticJsonBuffer<500> JSONBuffer; //Memory pool
      /**
       * If root element is object
       * 
      JsonObject& parsed = JSONBuffer.parseObject(payload); //Parse message
      if(parsed.success()) {
        const char * postId = parsed["postId"]; // Get postId
        const char * id = parsed["id"];         // Get id
        const char * tname = parsed["name"];    // Get name
        const char * email = parsed["email"];   // Get email
        const char * body = parsed["body"];     // Get body

        Serial.println(httpCode);
        Serial.print("postId: "); Serial.println(postId);
        Serial.print("id: "); Serial.println(id);
        Serial.print("tname: "); Serial.println(tname);
        Serial.print("email: "); Serial.println(email);
        Serial.print("body: "); Serial.println(body);
      } else {
        Serial.println(payload);
        Serial.println("JSON parsing failed !!");
      }
      */
      /**
       * If root object is array
       */
      JsonArray &root = JSONBuffer.parseArray(payload);
      for(int i=0; i<root.size(); i++) {
        JsonObject& parsed = root[i];
        const char * postId = parsed["postId"]; // Get postId
        const char * id = parsed["id"];         // Get id
        const char * tname = parsed["name"];    // Get name
        const char * email = parsed["email"];   // Get email
        const char * body = parsed["body"];     // Get body

        Serial.println(httpCode);
        Serial.print("postId: "); Serial.println(postId);
        Serial.print("id: "); Serial.println(id);
        Serial.print("tname: "); Serial.println(tname);
        Serial.print("email: "); Serial.println(email);
        Serial.print("body: "); Serial.println(body);
        Serial.println("------------------------------");
        Serial.println();
      }
    }
    else {
      Serial.println("Error on HTTP request");
    }
 
    http.end(); //Free the resources
  }
 
  delay(10000);
 
}
