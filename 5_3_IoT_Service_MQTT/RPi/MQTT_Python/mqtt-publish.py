import paho.mqtt.client as mqtt
import time

mqttc = mqtt.Client('RaspberryPI')
mqttc.connect('test.mostquitto.org', 1883)
while mqttc.loop() == 0:
     mqttc.publish('rfd', 'Publish')
     time.sleep(10)
