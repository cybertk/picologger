#!/usr/bin/env python

import socket
import struct

SERVER = ("127.0.0.1", 20504)
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

data = struct.pack('>IIII6sI', 1, 1, 165536, 3, 'abcdef', 0)
s.sendto(data, SERVER)
