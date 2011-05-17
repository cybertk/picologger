#!/usr/bin/env python


import socket
import time
import sys

from datetime import datetime

SERVER = ("0.0.0.0", 38858)
DATE_FMT = "%d/%m/%y %H:%M:%S.%f"

if len(sys.argv) != 2:
    print 'Usage: %s <path/to/save>' % sys.argv[0]
    sys.exit(0)

path = sys.argv[1]

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(SERVER)

just_now = time.time();
f = open(path, 'r')

while True:
    data, addr = s.recvfrom(65535)

    data = f.read(320)
    if len(data) < 320:
        f.seek(0)
        data = f.read(320)

    s.sendto(data, addr)

    delta = (time.time() - just_now) * 1000
    just_now = time.time()
    print "%s [%9.4f] Forwarded %d-bytes data" % (datetime.now().strftime(DATE_FMT), delta, len(data))
