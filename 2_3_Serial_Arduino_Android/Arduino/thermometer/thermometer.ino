#include <DHT11.h>
int pin=2;    // 연결한 아두이노 디지털 핀 번호
DHT11 dht11(pin); 

unsigned long prev_time = 0L;
const int READ_INTERVAL = 2000;
boolean is_on = true;

void setup() {
   Serial.begin(9600);
}

void loop() {
  unsigned long current_time = millis();

  // Read sensor every 2 sec.
  if(current_time > prev_time + READ_INTERVAL && is_on) {
    int err;
    float temp, humi;
    if((err=dht11.read(humi, temp))==0) {
      Serial.print("temperature:");
      Serial.print(temp);
      Serial.print(", humidity:");
      Serial.print(humi);
      Serial.println();
    } else {
      Serial.println();
      Serial.print("Error No :");
      Serial.print(err);
      Serial.println();    
    }
    prev_time = current_time;
  }

  if(Serial.available()) {
    char received = Serial.read();
    if(received == '1') {
      is_on = true;
      Serial.println("Sensor on");
    }
    else if(received == '0') {
      is_on = false;
      Serial.println("Sensor off");
    }
  }
}

