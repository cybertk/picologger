#!/usr/bin/env python3

import threading
import time

import http.server


clients = []

class GetHandler(http.server.BaseHTTPRequestHandler):

    def do_GET(self):
        try:
            self.send_response(200)
            self.send_header('Content-type', 'text/html')
            self.end_headers()
            #self.wfile.write(self.path)
            clients = self
            self.wfile.write(bytearray(self.path.encode('utf-8')))
            return
        except IOError:
            self.send_error(404, '%s not found' % self.path)
#            if slef.path.endswith(".html"):
#                f = open(self.path)
#        except KeyboardInterrupt:
#            print('oh no')


HTTPD_ADDR = ('', 10504)

# dedicated thread processing http requests
class WebServer(threading.Thread):
    def run(self):
        httpd = http.server.HTTPServer(HTTPD_ADDR, GetHandler)
        print("Server starting")
        httpd.serve_forever()

LOGD_ADDR = ('', 20504)
LOGD_MAX_PACKET_SIZE = 2048

LOG_LEVEL=('S', 'S', 'V', 'D', 'I', 'W', 'E', 'F')
# dedicated thread processing udp logging info
class LogServer(threading.Thread):

    def run(self):
        import struct
        import socket
        import string

        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.bind(LOGD_ADDR)

        while True:
            data, addr = s.recvfrom(LOGD_MAX_PACKET_SIZE)

            print('get %s bytes form' % len(data), addr)

            ts = 0
            while data:
                try:
                    buf = struct.unpack('>IIII%ds' % (len(data) - 16), data)
                except struct.error:
                    continue

                sec = buf[0]
                usec = buf[1]

                _t = (buf[0] * 1000.0) + (usec / 1000.0)
                delta = _t - ts;
                ts = _t;
                level = buf[2]
                sz = buf[3]

                l = buf[4][:sz]
                end = l.find(b'\x00')

                tag = l[:end].decode('utf-8')
                log = l[end+1:-1].decode('utf-8')

                print('[%10d]%15s %s/%-10s: %s' % (delta, addr[0], LOG_LEVEL[level], tag, log))

#                print(sec, usec, level, sz, buf[4][:sz])

                data = buf[4][sz:]


            #clients.wfile.write(buf)
            #print(buf)



if __name__ == '__main__':
    thread1 = WebServer(name = 't1')
    thread1.start()

    thread2 = LogServer()
    thread2.start()

    while True:
        time.sleep(11000)

