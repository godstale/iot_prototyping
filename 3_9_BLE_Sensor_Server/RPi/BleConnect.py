import struct
from bluepy.btle import Scanner, DefaultDelegate, UUID, Peripheral

TARGET_UUID = "4d6fc88bbe756698da486866a36ec78e"
target_dev = None
UART_SERVICE_UUID = UUID("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
uart_service = None
UART_WRITE_UUID = UUID("beb5483e-36e1-4688-b7f5-ea07361b26a8")
write_char = None
UART_READ_UUID = UUID("beb5483e-36e1-4688-b7f5-ea07361b26fa")
read_char = None
read_handle = None
read_cccd = None

#############################################
# Define scan callback
#############################################
class ScanDelegate(DefaultDelegate):
    def __init__(self):
        DefaultDelegate.__init__(self)
        

    def handleDiscovery(self, dev, isNewDev, isNewData):
        if isNewDev:
            print("Discovered device %s" % dev.addr)
        elif isNewData:
            print("Received new data from %s", dev.addr)

#############################################
# Define notification callback
#############################################
class NotifyDelegate(DefaultDelegate):
    #Constructor (run once on startup)  
    def __init__(self, params):
        DefaultDelegate.__init__(self)
      
    #func is caled on notifications
    def handleNotification(self, cHandle, data):
         print("Notification : " + data.decode("utf-8"))

#############################################
# Main starts here
#############################################
scanner = Scanner().withDelegate(ScanDelegate())
devices = scanner.scan(10.0)

for dev in devices:
    print("Device %s (%s), RSSI=%d dB" % (dev.addr, dev.addrType, dev.rssi))

    for (adtype, desc, value) in dev.getScanData():
        # Check iBeacon UUID
        # 255 is manufacturer data (1  is Flags, 9 is Name)
        if adtype is 255 and TARGET_UUID in value:
            target_dev = dev
            print("  +--- found target device!!")
        print("  (AD Type=%d) %s = %s" % (adtype, desc, value))

if target_dev is not None:
    #############################################
    # Connect
    #############################################
    print("Connecting...")
    print(" ")
    p = Peripheral(target_dev.addr, target_dev.addrType)

    try:
        # Set notify callback
        p.setDelegate( NotifyDelegate(p) )

        #############################################
        # For debug
        #############################################
        services=p.getServices()
        # displays all services
        for service in services:
            print(service)
            # displays characteristics in this service
            chList = service.getCharacteristics()
            print("-------------------------------------------------------")
            print("Handle   UUID                                Properties")
            print("-------------------------------------------------------")
            for ch in chList:
                print("  0x"+ format(ch.getHandle(),'02X')  +"   "+str(ch.uuid) +" " + ch.propertiesToString())
            print("-------------------------------------------------------")
            print(" ")

        #############################################
        # Set up characteristics
        #############################################
        uart_service = p.getServiceByUUID(UART_SERVICE_UUID)
        write_char = uart_service.getCharacteristics(UART_WRITE_UUID)[0]
        read_char = uart_service.getCharacteristics(UART_READ_UUID)[0]

        read_handle = read_char.getHandle()
        # Search and get the read-Characteristics "property" 
        # (UUID-0x2902 CCC-Client Characteristic Configuration))
        # which is located in a handle in the range defined by the boundries of the Service
        for desriptor in p.getDescriptors(read_handle, 0xFFFF):  # The handle range should be read from the services 
            if (desriptor.uuid == 0x2902):                   #      but is not done due to a Bluez/BluePy bug :(     
                print("Client Characteristic Configuration found at handle 0x"+ format(desriptor.handle, "02X"))
                read_cccd = desriptor.handle
                p.writeCharacteristic(read_cccd, struct.pack('<bb', 0x01, 0x00))

        #############################################
        # BLE message loop
        #############################################
        while 1:
            if p.waitForNotifications(5.0):
                # handleNotification() was called
                continue
                
            # p.writeCharacteristic(hButtonCCC, struct.pack('<bb', 0x01, 0x00))
            write_char.write(str.encode("hello~"))
        
    finally:
        p.disconnect()

else:
    print("No matching device found...")


print("Close app")
