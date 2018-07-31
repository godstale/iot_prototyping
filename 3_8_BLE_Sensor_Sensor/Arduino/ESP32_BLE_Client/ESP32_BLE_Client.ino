/*
   Based on Neil Kolban example for IDF: https://github.com/nkolban/esp32-snippets/blob/master/cpp_utils/tests/BLE%20Tests/SampleScan.cpp
   Ported to Arduino ESP32 by Evandro Copercini
*/

#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <sstream>

#define GPIO_DEEP_SLEEP_DURATION     10  // sleep x seconds and then wake up

// The remote service we wish to connect to.
#define BEACON_UUID "4d6fc88bbe756698da486866a36ec78e"
static BLEUUID serviceUUID("4fafc201-1fb5-459e-8fcc-c5c9c331914b");
// The characteristic of the remote service we are interested in.
static BLEUUID readCharUUID("beb5483e-36e1-4688-b7f5-ea07361b26fa");
static BLEUUID writeCharUUID("beb5483e-36e1-4688-b7f5-ea07361b26a8");

BLEScan* pBLEScan;
BLEAddress *pServerAddress;
BLEClient* pClient;
BLERemoteService* pRemoteService;
BLERemoteCharacteristic* pReadCharacteristic;
BLERemoteCharacteristic* pWriteCharacteristic;

static boolean doConnect = false;
static boolean connected = false;
int scanTime = 30; //In seconds
std::string m_targetUUID = BEACON_UUID;

static void notifyCallback(BLERemoteCharacteristic* pBLERemoteCharacteristic,
                            uint8_t* pData, size_t length, bool isNotify) {
    Serial.print("Received>  ");
    Serial.println(std::string((char *)pData, length).c_str());
}

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) {
      Serial.printf("Advertised Device: %s \n", advertisedDevice.toString().c_str());
      
      // Check beacon UUID (BEACON_UUID)
      if(advertisedDevice.haveManufacturerData()) {
        std::stringstream ss;
        char *pHex = BLEUtils::buildHexData(nullptr, 
                                            (uint8_t*)advertisedDevice.getManufacturerData().data(), 
                                            advertisedDevice.getManufacturerData().length());
        ss << ", manufacturer data: " << pHex;
        free(pHex);
        std::string m_data = ss.str();
        
        if (m_data.find(m_targetUUID) != std::string::npos) {
          Serial.print("Found our device!  address: "); 
          Serial.println(advertisedDevice.getAddress().toString().c_str());
          advertisedDevice.getScan()->stop();
          pServerAddress = new BLEAddress(advertisedDevice.getAddress());
          doConnect = true;
        }
      }
    }
};


void setup() {
  Serial.begin(115200);

  initBle();
  scanInit();
  scan();
}

void loop() {
  // If the flag "doConnect" is true then we have scanned for and found the desired
  // BLE Server with which we wish to connect.  Now we connect to it.  Once we are 
  // connected we set the connected flag to be true.
  if (doConnect == true) {
    if (connectToServer(*pServerAddress)) {
      Serial.println("We are now connected to the BLE Server.");
      connected = true;
    } else {
      Serial.println("We have failed to connect to the server; there is nothin more we will do.");
      connected = false;
    }
    doConnect = false;
  }

  if(connected && (pClient->isConnected() == false)) {
    connected = false;
    Serial.println("Disconnected!! Reset after 5sec.");
    esp_deep_sleep(1000000LL * GPIO_DEEP_SLEEP_DURATION);
  }

  if(!connected) {
    scan();
  }

  // If we are connected to a peer BLE Server, update the characteristic each time we are reached
  // with the current time since boot.
  if (connected) {
    int input_max = 20;
    char input_char[] = {0x00, 0x00, 0x00, 0x00, 0x00, 
                            0x00, 0x00, 0x00, 0x00, 0x00, 
                            0x00, 0x00, 0x00, 0x00, 0x00, 
                            0x00, 0x00, 0x00, 0x00, 0x00};
    int i=0;
    while(Serial.available()) {
      char input = Serial.read();
      input_char[i] = input;
      i++;
      if(i >= input_max) break;
    }
  
    if(i > 0) {
      // Set the characteristic's value to be the array of bytes that is actually a string.
      pWriteCharacteristic->writeValue((uint8_t*)input_char, i);
    }
  }
  
  delay(3000); // Delay a second between loops.
}


void initBle() {
  // Create the BLE Device
  BLEDevice::init("MyBLE");
}

void scanInit() {
  pBLEScan = BLEDevice::getScan(); //create new scan
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);            //active scan uses more power, but get results faster
}

void scan() {
  Serial.println("Start scanning...");
  pBLEScan->start(scanTime);    // (Blocking call)
  Serial.println("Scan done!");
}

bool connectToServer(BLEAddress pAddress) {
    Serial.print("Forming a connection to ");
    Serial.println(pAddress.toString().c_str());
    
    pClient = BLEDevice::createClient();
    Serial.println(" - Created client");

    // Connect to the remove BLE Server. (Blocking call)
    pClient->connect(pAddress);
    if(pClient->isConnected() == false) {
      return false;
    }
    Serial.println(" - Connected to server");

    // Obtain a reference to the service we are after in the remote BLE server.
    pRemoteService = pClient->getService(serviceUUID);
    if (pRemoteService == nullptr) {
      Serial.print("Failed to find our service UUID: ");
      Serial.println(serviceUUID.toString().c_str());
      return false;
    }
    Serial.println(" - Found our service");

    // Obtain a reference to the characteristic in the service of the remote BLE server.
    pReadCharacteristic = pRemoteService->getCharacteristic(readCharUUID);
    if (pReadCharacteristic == nullptr) {
      Serial.print("Failed to find read characteristic UUID: ");
      Serial.println(readCharUUID.toString().c_str());
      return false;
    }
    Serial.println(" - Found read characteristic");
    pReadCharacteristic->registerForNotify(notifyCallback);
    const uint8_t v[]={0x1,0x0};
    pReadCharacteristic->getDescriptor(BLEUUID((uint16_t)0x2902))->writeValue((uint8_t*)v,2,true);

    pWriteCharacteristic = pRemoteService->getCharacteristic(writeCharUUID);
    if (pWriteCharacteristic == nullptr) {
      Serial.print("Failed to find write characteristic UUID: ");
      Serial.println(writeCharUUID.toString().c_str());
      return false;
    }
    Serial.println(" - Found write characteristic");

    // Read the value of the characteristic.
    //std::string value = pRemoteCharacteristic->readValue();
    //Serial.print("The characteristic value was: ");
    //Serial.println(value.c_str());
}

