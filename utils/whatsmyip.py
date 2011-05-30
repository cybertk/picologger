#!/usr/bin/env python3
#
#  -- https://github.com/kyan-he/picologger/raw/master/utils/whatsmyip.py --
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

import sys
import http.server

DEFAULT_PORT = 38859

class GetHandler(http.server.BaseHTTPRequestHandler):

    def do_GET(self):
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()

        # Write client address back.
        self.wfile.write(bytearray(self.client_address[0].encode('UTF-8')))

        return

# Get port number.
port = DEFAULT_PORT

if len(sys.argv) > 1:
    try:
        port = int(sys.argv[1])
    except:
        print('Usage: %s [port]' % sys.argv[0])
        sys.exit(0)

httpd_addr = ('', port)

# Start httpd.
httpd = http.server.HTTPServer(httpd_addr, GetHandler)
httpd.serve_forever()
