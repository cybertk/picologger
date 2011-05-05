/*
 *   Copyright 2011, Kyan He <kyan.ql.he@gmail.com>
 *
 *   -- service.h --
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

#ifndef __SERVICES_H__
#define __SERVICES_H__

#include <time.h>
#include <unistd.h>
#include "list.h"

struct command;
struct service {
    /* list of all test services */
    struct listnode slist;

    const char *name;
    const char *classname;  /* TODO:? */
    const char *clientname;

    struct command *cmd;
    int id;                 /* task ID */

    time_t time_started;    /* time of last start */
    time_t time_stopped;    /* time of last start */
    int nr_started;         /* number of times this services started */

    uid_t uid;
    gid_t gid;
    pid_t pid;

    int nargs;
    char **args;
};

struct service *service_find_by_pid(pid_t pid);
typedef void (*cmd_func)(struct service *svc);

/* all services we support */
void cmd_list_func(struct service *s);
void cmd_capt_func(struct service *s);
void cmd_qury_func(struct service *s);

struct command {
    const char *key;
    cmd_func func;
    const char *desc;
};

extern struct command commands[];
#endif
