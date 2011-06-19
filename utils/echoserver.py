#!/usr/bin/env python
#
#  -- https://github.com/kyan-he/picologger/raw/master/utils/echoserver.py --
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
#  along with this program.  If not, see <http://www.gnu.org/licenses/>.


import socket
import time

from datetime import datetime

SERVER = ("0.0.0.0", 38858)
DATE_FMT = "%d/%m/%y %H:%M:%S.%f"

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
s.bind(SERVER)

just_now = time.time();
while True:
    data, addr = s.recvfrom(65535)

#    data = f.read(32)
#
#    if not data:
#        f.seek(0);
#        data = f.read(32)

    s.sendto(data, addr)
    delta = (time.time() - just_now) * 1000
    just_now = time.time()
    print("%s [%9.4f] Forwarded %d-bytes data" % (datetime.now().strftime(DATE_FMT), delta, len(data)))
