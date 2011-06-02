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

extern struct listnode clients;

struct command* get_command(char *key)
{
    LOG_FUNCTION_NAME

    struct command *c;
    int i;

    for (i = 0; c = &commands[i], c->key; i++) {
        if (!strcmp(commands[i].key, key))
            break;
    }

    return c;
}

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

int cmd_mute_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME

    client->flags &= ~CLIENT_MONITOR;
}

extern int cmd_fltr_func(struct client *client, int argc, char * const argv[]);

extern int cmd_shel_func(struct client *client, int argc, char * const argv[]);

// TODO: move into commands.h
struct command commands[] = {
    { "HELP",     cmd_help_func,    "list available commands" },
    { "SHELL",    cmd_shel_func,    "shell" },
    { "FLTR",     cmd_fltr_func,    "set filter"          },
    { "MUTE",     cmd_mute_func,    "disable log fowarding"          },
    { 0, 0, 0 }
};

