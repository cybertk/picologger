#!/usr/bin/env python
#
#  -- https://android-autotool.googlecode.com/svn/trunk/logcat.py --
#
#  Python picologger client
#
#  Copyright 2011, Kyan He <kyan.ql.he@gmail.com>
#
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program.  If not, see <http://www.gnu.org/licenses/>

import sys
import socket
import struct

DEFAULT_AGENTD_PORT = 20504
PACKET_MAX_SIZE = 1280
LOG_LEVEL=('S', 'S', 'V', 'D', 'I', 'W', 'E', 'F')

# parse_packet
ts = 0
def parse_packet(data):
    try:
        buf = struct.unpack('>IIIII%ds' % (len(data) - 20), data)
    except struct.error:
        return None

    addr = socket.inet_ntoa(data[0:4])
    sec = buf[1]
    usec = buf[2]

    level = buf[3]
    if level < 0 or level > len(LOG_LEVEL):
        level = 0

    sz = buf[4]
    l = buf[5][:sz]

    # TODO:rm
    ts = 0
    _t = (sec * 1000.0) + (usec / 1000.0)
    delta = _t - ts;
    ts = _t;

    end = l.find(b'\x00')
    tag = l[:end].decode('utf-8')
    log = l[end+1:-1].decode('utf-8')

    print('[%10d]%15s %s/%-10s: %s' % (delta, addr, LOG_LEVEL[level], tag, log))

    return buf[5][sz:]

if len(sys.argv) != 3:
    print('Usage: %s <log server ip> [filter]' % sys.argv[0])
    sys.exit(0)

server = (sys.argv[1], DEFAULT_AGENTD_PORT)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(server)

s.send(sys.argv[2])

while True:
    data = s.recv(PACKET_MAX_SIZE)
    if data[0:4] == "OKAY":
        print("ready to receive logs")
    elif data[0:4] == "FAIL":
        print(data[5:])
        sys.exit(-1)
    else:
        while data:
            data = parse_packet(data)
