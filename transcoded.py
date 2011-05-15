#!/usr/bin/env python


import socket
import time
import subprocess

from datetime import datetime

SERVER = ("0.0.0.0", 38858)
DATE_FMT = "%d/%m/%y %H:%M:%S.%f"

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(SERVER)

f = open("arm_raw", "r")
just_now = time.time();
while True:
    data, addr = s.recvfrom(65535)
    f = open('_amr_tmp', 'w');
    f.write('#!AMR\n')
    f.write(data)
    f.close()

    subprocess.call(['amrnb-decoder', '_amr_tmp', '_pcm_tmp'])

    f = open('_pcm_tmp', 'r')
    data = f.read(1024)

    s.sendto(data, addr)
    delta = (time.time() - just_now) * 1000
    just_now = time.time()
    print "%s [%9.4f] Forwarded %d-bytes data" % (datetime.now().strftime(DATE_FMT), delta, len(data))
