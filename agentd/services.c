/*
 *   Copyright 2011, Kyan He <kyan.ql.he@gmail.com>
 *
 *   -- services.c --
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

#include "services.h"
#include "log.h"

struct command commands[] = {
    { "LIST",     cmd_list_func,    "list"                  },
    { "CAPT",     cmd_capt_func,    "capture framebuffer"   },
    { "QURY",     cmd_qury_func,    "query status"          },
    { 0, 0, 0 }
};

void cmd_list_func(struct service *svc)
{
    LOG_FUNCTION_NAME

    svc->pid = fork();
    if (svc->pid < 0) {
        perror("fork");
    }

    if (svc->pid == 0) {

        /* redirect stdout, stderr to network */
        //dup2(fd, 1);
        //dup2(fd, 2);

        D("pid %d exit", getpid());
        exit(0);
    }
}

void cmd_capt_func(struct service *argv)
{
    LOG_FUNCTION_NAME
}

void cmd_qury_func(struct service *argv)
{
    LOG_FUNCTION_NAME
}
