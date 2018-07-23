import os
import sys
import time
import threading
from SerialThread import *

############################################
# Global variables
############################################
# Time
start = time.time()


############################################
# Private functions
############################################
def finalize():
    print('\nCleaning up the resources...')
    t_ser.close()
    print("Elapsed Time: %s" % (time.time() - start))
    print("Bye~\n\n")

def send_msg():
    t_ser.send(bytes("hello".encode()))
    t_timer = threading.Timer(5.0, send_msg)
    t_timer.start()

############################################
# Python starts here
############################################

# Connect serial
t_ser = SerialThread()
t_ser.connect()

# Start main loop
if __name__ == '__main__':
    try:
        # Start serial monitor thread
        t_ser.setDaemon(True)
        t_ser.start()
        # Start serial TX timer
        send_msg()

        # Main loop
        while True:
            continue

    except KeyboardInterrupt:
        print('    KeyboardInterrupt!!! Ending main loop')
        # Quit gracefully
        finalize()
        try:
            sys.exit(0)
        except SystemExit:
            os._exit(0)

# Quit gracefully
finalize()

