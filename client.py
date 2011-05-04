#!/usr/bin/env python

import socket
import struct

SERVER = ("127.0.0.1", 20504)
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

data = struct.pack('>III6s', 1, 1, 1, 'abcdef')
s.sendto(data, SERVER)
