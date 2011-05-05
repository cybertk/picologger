/*
 *   Copyright 2010, Kyan He <kyan.ql.he@gmail.com>
 *
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Modified: 
 *      Kyan He <kyan.ql.he@gmail.com> @ Wed Mar  2 13:23:33 CST 2011
 *
 */

#ifndef __AGENTD_H__
#define __AGENTD_H__

#include "list.h"
//#include <sys/stat.h>

struct socketinfo {
    //TODO: implement linked
    //struct socketinfo *next;

    const char *name;
    int fd;
};

struct client {
    // list of all clients
    struct listnode clist;
    //TODO: rm
    struct listnode services;

    char *name;

    // separated by ","
    char *filter;
    struct in_addr ip;

#define CLIENT_MONITOR 0x0001
    unsigned flags;

    struct fdevent fde;
};

#endif
