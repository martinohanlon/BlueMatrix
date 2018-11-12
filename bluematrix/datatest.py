from btcomm import BluetoothServer
from time import sleep, time
from signal import pause

def data_received(data):
    print("data received '{}'".format(data))
    s.send("msg1\nmsg2\n")
    sleep(1)
    s.send("hello")
    sleep(1)
    s.send("bye\n")

def client_connects():
    print("client connected")
    sleep(1)
    s.send("hi\nbye\n")

s = BluetoothServer(data_received)
s.when_client_connects = client_connects
pause()
