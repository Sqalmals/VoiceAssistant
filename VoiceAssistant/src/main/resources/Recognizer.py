#!/usr/bin/env python3

import queue
import sounddevice as sd
import vosk
import sys
import socket

q = queue.Queue()

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind(('localhost', 25567))
s.listen(5)

def callback(indata, frames, time, status):
    """This is called (from a separate thread) for each audio block."""
    if status:
        print(status, file=sys.stderr)
    q.put(bytes(indata))

try:
    model = "model"
    device_info = sd.query_devices(sd.default.device, 'input')
    samplerate = int(device_info['default_samplerate'])

    model = vosk.Model(model)
    with sd.RawInputStream(samplerate=samplerate, blocksize = 8000, device=sd.default.device, dtype='int16',
                            channels=1, callback=callback):
            print('#' * 80)
            print('Press Ctrl+C to stop the recording')
            print('#' * 80)

            rec = vosk.KaldiRecognizer(model, samplerate)

            clientSocket, address = None,None

            while clientSocket == None and address == None:
                clientSocket, address = s.accept()

            print(f"connection from {address} has been established")
            clientSocket.send(bytes("Connected!", "utf-8"))

            while True:
                data = q.get()
                if rec.AcceptWaveform(data):
                    result = rec.Result()
                    print(result)
                    clientSocket.send(bytes(result, "utf-8"))
                    if(result == "{\n  \"text\" : \"stop\"\n}"):
                        print("Stopping...")
                        clientSocket.close()
                        break
                    
                '''else:
                    print(rec.PartialResult())'''
except KeyboardInterrupt:
    print('\nDone')
    s.close()
except Exception as e:
    print(str(e))



