#!/usr/bin/env python

import socket
import struct
import sys

if len(sys.argv) != 9:
    print 'Usage: %s <ip> <port> <sec> <usec> <priority> <size> <tag> <log>' % sys.argv[0]
    sys.exit(0)

# encode args
ip = sys.argv[1]
port = sys.argv[2]
sec = sys.argv[3]
usec = sys.argv[4]
prio = sys.argv[5]
sz = sys.argv[6]
tag = sys.argv[7]
log = sys.argv[8]

server = (ip, int(port))
s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)

str = "%s\x00%s\x00" % (tag,log)
data = struct.pack('>IIII%ds' % int(sz), int(sec), int(usec), int(prio), int(sz), str)
s.sendto(data, server)
