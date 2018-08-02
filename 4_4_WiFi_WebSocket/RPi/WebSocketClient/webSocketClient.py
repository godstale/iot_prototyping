import websocket
import time

ws = websocket.WebSocket()
ws.connect("ws://192.168.1.12/")

i = 0
nrOfMessages = 200

while i < nrOfMessages:
    ws.send("message nr: " + str(i))
    result = ws.recv()
    print(result)
    i=i+1
    time.sleep(1)

ws.close()
