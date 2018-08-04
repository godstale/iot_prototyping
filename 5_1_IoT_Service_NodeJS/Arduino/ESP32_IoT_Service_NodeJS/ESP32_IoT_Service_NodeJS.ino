#include <WiFi.h>
#include <HTTPClient.h>

const char* ssid     = "your_ssid";    // 와이파이 SSID
const char* password = "your_pw";      // 와이파이 비밀번호

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

}

void loop()
{
    HTTPClient http;
 
    http.begin("http://www.server_addr.com:3000/data");        // 서버 주소
    http.addHeader("Content-Type", "application/x-www-form-urlencoded");

    // 랜덤 번호 생성
    esp_random();
    String num = String(random(0, 256));
    Serial.println("num : " + num);

    // POST 후 결과 받음
    int httpResponseCode = http.POST("data="+num);
    if(httpResponseCode>0){
        String response = http.getString();
        Serial.println(httpResponseCode);
        Serial.println(response);
    }else{
        Serial.print("Error on sending POST: ");
        Serial.println(httpResponseCode);
    }
 
    http.end();  // 리소스 해제
    delay(10000);
}

