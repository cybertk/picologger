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

#ifndef __CLIENT_H__
#define __CLIENT_H__

#include <sys/stat.h>
#include <netinet/in.h>
#include "list.h"
#include "fdevent.h"

struct running_command {

    /* Should point to commands */
    struct command *cmd;

    /* command specific data */
    void *cookie;
};

struct client {

    /* Client name. */
    char *name;

    /* List of all clients. */
    struct listnode clist;

    /* Command running */
    struct running_command running_command;

    struct in_addr ip;

#define CLIENT_MONITOR 0x0001
    unsigned flags;

    struct fdevent fde;
};

struct client* alloc_client();
void client_destory(struct client*);
void client_dump(struct client*);

#endif
