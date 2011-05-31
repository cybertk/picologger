#!/usr/bin/env python
#
#  -- https://github.com/kyan-he/picologger/raw/master/logcat.py --
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
LOG_PRIORITY=('S', 'S', 'V', 'D', 'I', 'W', 'E', 'F')

# parse_packet
prev_ts = 0
def parse_packet(data):
    buf = struct.unpack('>IIII%ds' % (len(data) - 16), data)

    sec = buf[0]
    usec = buf[1]

    prio = buf[2]
    if prio < 0 or prio > len(LOG_PRIORITY):
        prio = 0

    sz = buf[3]
    l = buf[4][:sz]

    # convert to millis second
    ts = (sec * 1000.0) + (usec / 1000.0)

    end = l.find(b'\x00')
    tag = l[:end].decode('utf-8')
    log = l[end+1:-1].decode('utf-8')

    #print('[%10d]%15s %s/%-10s: %s' % (delta, addr, LOG_prio[level], tag, log))

    return (buf[4][sz:], ts, prio, tag, log)

if len(sys.argv) < 2 or len(sys.argv) > 3:
    print('Usage: %s <log server ip> [filter]' % sys.argv[0])
    sys.exit(0)

server = (sys.argv[1], DEFAULT_AGENTD_PORT)

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.connect(server)

ts_base = 0

if len(sys.argv) == 3:
    s.send(sys.argv[2])

f = open('logs', 'w')
while True:
    data = s.recv(PACKET_MAX_SIZE)
    if data[0:4] == "OKAY":
        print("ready to receive logs")
    elif data[0:4] == "FAIL":
        print(data[5:])
        sys.exit(-1)
    else:

        # get from address
        addr = socket.inet_ntoa(data[0:4])
        data = data[4:]

        # parse log in loop
        while data:

            try:
                data, ts, prio, tag, log = parse_packet(data)
            except struct.error:
                data = None
                print "struct error"
            except UnicodeDecodeError:
                data = None
                print "struct error"

            # setup ts base
            if ts_base == 0:
                ts_base = ts;
            # calculate delta time between two log records
            delta = ts - prev_ts
            prev_ts = ts

            log = '[%7.3f] %s %s/%s: %s' % ((ts - ts_base) / 1000.0, addr, LOG_PRIORITY[prio], tag, log)

            f.write(log)
            f.write('\n')
            print log
