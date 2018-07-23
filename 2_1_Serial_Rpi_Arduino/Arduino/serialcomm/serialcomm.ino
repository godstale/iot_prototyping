#include <SoftwareSerial.h>

SoftwareSerial swSerial(2, 3);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(9600);
  swSerial.begin(9600);

  Serial.println("Hello~!");
}

void loop() {
  // put your main code here, to run repeatedly:
  if (swSerial.available()) {
    Serial.write(swSerial.read());
  }
  if (Serial.available()) {
    swSerial.write(Serial.read());
  }
}
