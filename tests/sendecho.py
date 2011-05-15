#!/usr/bin/env python

import socket
import sys

if len(sys.argv) != 4:
    print 'Usage: <server> <port> <msg>'
    sys.exit(0)

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

s.sendto(sys.argv[3], (sys.argv[1], int(sys.argv[2])))
