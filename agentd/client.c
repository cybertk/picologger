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
 *      Kyan He <kyan.ql.he@gmail.com> @ Sat May  7 19:30:06 CST 2011
 *
 *
 */

#include "client.h"
#include "list.h"
#include "log.h"
#include "fdevent.h"

struct client* client_create()
{

    struct client *c;

    c = calloc(1, sizeof(struct client));
    if (!c)
        return 0;

#define MAX_CLIENT_NAME     128
    c->name = malloc(MAX_CLIENT_NAME);
    if (!c->name)
        return 0;

    return c;
}

void client_destroy(struct client *c)
{

    D("Client %s destroied", c->name);

    fdevent_remove(&c->fde);
    close(c->fde.fd);

    free(c);
}

void client_dump(struct client *c)
{
    struct listnode *node;

    D("    name   : %s\n"
        "    socket : %d",
            c->name, c->fde.fd);
}

