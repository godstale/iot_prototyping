import os
import sys
import time
import traceback
import threading
import serial


############################################
# Global variables
############################################
# Serial
BAUDRATE=9600

############################################
# Serial monitoring thread
############################################
class SerialThread(threading.Thread):
    quit = False

    def __init__(self):
        threading.Thread.__init__(self)
        self.ser = None

    def connect(self):
        # Connect serial
        print('Serial thread: started !!!')
        print('Connect to serial...')

        # HW UART setup
        try:
            self.ser = serial.Serial('/dev/ttyS0', baudrate=BAUDRATE,
                                    parity=serial.PARITY_NONE,
                                    stopbits=serial.STOPBITS_ONE,
                                    bytesize=serial.EIGHTBITS)
            print('\tPort ttyS0 is opened')
        except serial.SerialException as ex:
            print(ex)

    def run(self):
        if self.ser is None:
            return

        while True:
            # Check serial input
            try:
                # HW UART monitoring
                while self.ser is not None:
                    # data = self.ser.read()      # Read one byte
                    # data = self.ser.readline()  # Read line
                    data = self.ser.read(self.ser.in_waiting or 1)    # Read bytes
                    if data:
                        # buffer = bytearray(data)
                        #print("-------------------------------------------------")
                        #print("".join("%02x " % b for b in buffer))
                        #print("-------------------------------------------------")
                        print("received: " + str(data, 'utf-8'))
                        self.send(data)

            except Exception as e:
                print(e)
                # Get current system exception
                ex_type, ex_value, ex_traceback = sys.exc_info()
                # Extract un-formatter stack traces as tuples
                trace_back = traceback.extract_tb(ex_traceback)
                # Format stacktrace
                # stack_trace = list()
                # for trace in trace_back:
                #    stack_trace.append("File : %s , Line : %d, Func.Name : %s, Message : %s"
                #                           % (trace[0], trace[1], trace[2], trace[3]))

                print("Exception type : %s " % ex_type.__name__)
                print("Exception message : %s" % ex_value)
                print("Stack trace : %s" % trace_back)

                print('Oooops!!! Serial port is not available!!!')
                break

            # End signal
            if self.quit:
                break

            time.sleep(0.01)

        # Close serial
        self.close()
        return

    def send(self, byte_array):
        # HW UART write
        self.ser.write(byte_array)
        pass

    def close(self):
        print('Serial thread: stopped !!!')
        self.quit = True
        if self.ser is not None:
            self.ser.close()
            self.ser = None

    # End of class SerialThread

