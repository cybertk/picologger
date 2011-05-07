/*
 *   Copyright 2011, Kyan He <kyan.ql.he@gmail.com>
 *
 *   -- commands.c --
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
 *      Kyan He <kyan.ql.he@gmail.com> @ Thu Mar  3 17:30:22 CST 2011
 *
 */

#include <unistd.h>
#include <errno.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "client.h"
#include "commands.h"
#include "log.h"
#include "list.h"

int cmd_help_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME

    FILE *f;
    int i;
    struct command *c;

    f = fdopen(client->fde.fd, "a");

    for(i = 0; c = &commands[i], c->key; i++) {

        fprintf(f, "%-10s%s\n", c->key, c->desc);
        fflush(f);
    }
}

int cmd_list_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME
}

int cmd_fltr_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME

    int opt;

    client->flags |= CLIENT_MONITOR;

    //TODO: reset_filter

    struct addr_filter *af;
    struct tag_filter *tf;

    // reset getopt
    // sems argv[argc] = 0 will not reset optind automatically
    optind = 1;
    while ((opt = getopt(argc, argv, "a:t:")) != -1) {
        switch (opt) {
            case 'a':
                af = malloc(sizeof(struct addr_filter));
                if (!af)
                    return -ENOMEM;

                if (inet_aton(optarg, &af->addr) < 0)
                    return -EINVAL;

                //TODO: find duplicate
                list_add_tail(&client->addrs, &af->list);
                break;

            case 't':
                //TODO: support priority

                tf = malloc(sizeof(struct tag_filter));
                if (!tf)
                    return -ENOMEM;

                memcpy(tf->tag, optarg, strlen(optarg));

                //TODO: find duplicate
                list_add_tail(&client->tags, &tf->list);
                break;
        }
    }

    dump_client(client);
}

int cmd_mute_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME

    client->flags &= ~CLIENT_MONITOR;
}

struct command commands[] = {
    { "HELP",     cmd_help_func,    "list available commands" },
    { "LIST",     cmd_list_func,    "list current connectd client" },
    { "FLTR",     cmd_fltr_func,    "set filter"          },
    { "MUTE",     cmd_mute_func,    "disable log fowarding"          },
    { 0, 0, 0 }
};

