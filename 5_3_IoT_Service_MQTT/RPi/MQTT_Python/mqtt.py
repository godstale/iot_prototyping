import socket
import fcntl
import struct
import paho.mqtt.client as mqtt

def on_connect(client, userdata, flags, rc):
    print("Connected with result code "+str(rc))
    client.subscribe("hello/world")

def on_message(client, userdata, msg):
    print( msg.topic+" "+str(msg.payload))

def get_ipaddress(network):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', network[:15].encode('utf-8'))
    )[20:24])


port = 1883

client = mqtt.Client('RaspberryPI')
client.on_connect = on_connect
client.on_message = on_message

client.connect(get_ipaddress('eth0'), port, 60)
client.loop_forever()
