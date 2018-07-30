/*
   Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleScan.cpp
   Ported to Arduino ESP32 by pcbreflux
*/
/*
   Create a BLE server that will send periodic iBeacon frames.
   The design of creating the BLE server is:
   1. Create a BLE Server
   2. Create advertising data
   3. Start advertising.
   4. wait
   5. Stop advertising.
   6. deep sleep
   
*/
#include "sys/time.h"

#include "BLEUtils.h"
#include "BLEDevice.h"
#include "BLEServer.h"
#include "BLEBeacon.h"
#include "esp_sleep.h"

// See the following for generating UUIDs: https://www.uuidgenerator.net/
#define BEACON_UUID           "8ec76ea3-6668-48da-9866-75be8bc86f4d" // UUID 1 128-Bit (may use linux tool uuidgen or random numbers via https://www.uuidgenerator.net/)
struct timeval now;

#define GPIO_DEEP_SLEEP_DURATION     5  // sleep x seconds and then wake up
RTC_DATA_ATTR static time_t last;        // remember last boot in RTC Memory
RTC_DATA_ATTR static uint32_t bootcount; // remember number of boots in RTC Memory

BLEServer *pServer;
BLEAdvertising *pAdvertising;


void setup() {
  gettimeofday(&now, NULL);
  Serial.begin(115200);
  Serial.printf("start ESP32 %d\n", bootcount++);
  Serial.printf("deep sleep (%lds since last reset, %lds since last boot)\n", now.tv_sec, now.tv_sec-last);
  last = now.tv_sec;

  initBle();
  
  startAdvertising();
  Serial.println("Advertizing started...");
  delay(5000);
  
  stopAdvertising();

  // Deep sleep
  Serial.printf("enter deep sleep\n");
  delay(100);
  esp_deep_sleep(1000000LL * GPIO_DEEP_SLEEP_DURATION);
}

void loop() {
}

void initBle() {
  // Create the BLE Device
  BLEDevice::init("MyBeacon");
  // Create the BLE Server
  pServer = BLEDevice::createServer();
}

void startAdvertising() {
  // Create advertising manager
  pAdvertising = pServer->getAdvertising();

  // Set beacon data
  setBeacon();
  
  // Start advertising
  pAdvertising->start();
}

void stopAdvertising() {
  // Stop advertising
  pAdvertising->stop();
}

void setBeacon() {
  BLEBeacon oBeacon = BLEBeacon();
  oBeacon.setProximityUUID(BLEUUID(BEACON_UUID));
  oBeacon.setMajor((bootcount & 0xFFFF0000) >> 16);
  oBeacon.setMinor(bootcount&0xFFFF);
  oBeacon.setManufacturerId(0x4C00); // fake Apple 0x004C LSB (ENDIAN_CHANGE_U16!)
  
  BLEAdvertisementData oAdvertisementData = BLEAdvertisementData();
  BLEAdvertisementData oScanResponseData = BLEAdvertisementData();
  
  oAdvertisementData.setFlags(0x04); // BR_EDR_NOT_SUPPORTED 0x04
  
  std::string strServiceData = "";
  strServiceData += (char)26;     // Len
  strServiceData += (char)0xFF;   // Type
  strServiceData += oBeacon.getData(); 
  oAdvertisementData.addData(strServiceData);
  
  pAdvertising->setAdvertisementData(oAdvertisementData);
  pAdvertising->setScanResponseData(oScanResponseData);
}

