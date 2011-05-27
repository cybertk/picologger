#!/usr/bin/env python

import socket
import struct
import sys

if len(sys.argv) != 3:
    print 'Usage: %s <ip> <port>' % sys.argv[0]
    sys.exit(0)

# encode args
ip = sys.argv[1]
port = sys.argv[2]

server = (ip, int(port))
s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(server)

str = "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1111\"] [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry..."
s.send(str)
