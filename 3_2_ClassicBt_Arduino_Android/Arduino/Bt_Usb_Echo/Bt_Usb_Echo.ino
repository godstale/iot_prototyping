#include <SoftwareSerial.h>

SoftwareSerial BTSerial(2, 3); //Connect HC-06. Use your (TX, RX) settings

void setup()  
{
  Serial.begin(9600);
  Serial.println("Hello!");

  BTSerial.begin(9600);  // set the data rate for the BT port
}

void loop()
{
  // BT –> Data –> Serial
  if (BTSerial.available()) {
    Serial.write(BTSerial.read());
  }
  // Serial –> Data –> BT
  if (Serial.available()) {
    BTSerial.write(Serial.read());
  }
}
