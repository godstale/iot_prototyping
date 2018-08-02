import socket
import fcntl
import struct
import asyncio
import websockets

def get_ipaddress(network):
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    return socket.inet_ntoa(fcntl.ioctl(
        s.fileno(),
        0x8915,  # SIOCGIFADDR
        struct.pack('256s', network[:15].encode('utf-8'))
    )[20:24])


async def echo(websocket, path):
    async for message in websocket:
        print(message)
        await websocket.send(message)

print("Server start : "+get_ipaddress('eth0'))

port = 80
asyncio.get_event_loop().run_until_complete(
    websockets.serve(echo, get_ipaddress('eth0'), port))
asyncio.get_event_loop().run_forever()
