/*
 *   Copyright 2011, Kyan He <kyan.ql.he@gmail.com>
 *
 *   -- agentd.c --
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
 *      Kyan He <kyan.ql.he@gmail.com> @ Fri May  6 01:15:33 CST 2011
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <errno.h>

#include <sys/socket.h>
#include <sys/wait.h>
#include <sys/types.h>
#include <netinet/in.h>
#include <sys/poll.h>

#include <unistd.h>
#include <signal.h>
#include <fcntl.h>

#include "fdevent.h"
#include "util.h"
#include "log.h"
#include "client.h"
#include "commands.h"
#include "syslog.h"

list_declare(clients);

#if 0
extern struct command commands[];
#endif

/* used to communicate with child */
static int signal_fd = -1;

static void sigchld_handler(int s)
{
    LOG_FUNCTION_NAME

    write(signal_fd, &s, 1);
    return;
}

static void dump_clients(void)
{
    struct client *c;
    struct listnode *node;

    D("--- clients dump ---\n");
    list_for_each(node, &clients) {
        c = node_to_item(node, struct client, clist);
        dump_client(c);
    }
}

//TODO: rm
//TODO: performance, optimize
// return argc
static int parse_cmds(char *line, size_t sz, char ***argv)
{
    LOG_FUNCTION_NAME

    char **args, *s;
    int nargs, i;

    // trim leading space
    while (*line == ' ') line++;

    /* trim, make sure line ends with \0 */
    if (line[sz-1] == '\n') {
        sz--;
        line[sz] = 0;
    } else {
        line[sz] = 0;
    }

    s = line;


    if (!*line) {
        D("parsing error, no argument found");
        return 0 ;
    } else {
        nargs = 1;
    }

    /* determine nargs and replace space with \0 */
    while (1) {

        // find space, break if not found
        if (!(s = strchr(s, ' '))) break;

        // replace space with \0
        while (*s == ' ') *s++ = 0;
        nargs++;
    }

    D("got %d args", nargs);

    args = malloc((nargs + 1) * sizeof(void *));
    if (!args) return 0;

    s = line;

    // copy arguments
    for (i = 0; i < nargs; i++) {

        /* advance to non-zero */
        while (!*s) s++;

        args[i] = strdup(s);
        D("argv[%d] : %s", i, args[i]);
        s += strlen(s);
    }

    /* due to calloc */
    /* cmd->args[nargs] = 0; */

    *argv = args;
    return nargs;
}

static void srvsock_io_handler(int fd, unsigned events, void* cookie)
{
    /* pass */
}

#define CMD_MAX_SIZE 1024
static void ctlsock_io_handler(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    struct service *s;
    struct client *c;
    struct command *cmd;
    char **argv;
    int argc;
    char linebuf[CMD_MAX_SIZE];
    int sz;

    // get client
    c = (struct client *)cookie;

    D("--- %s IO ---", c->name);

    sz = read(fd, linebuf, CMD_MAX_SIZE);
    if (sz == 0) {
        /* close by remote */
        // TODO: check all services under client, and destroy client
        D("--- %s IO CLOSE ---", c->name);
        fdevent_remove(&c->fde);
        list_remove(&c->clist);
        dump_clients();
        return;
    } else if (sz < 0) {
        perror("read");
        return;
    } else {
        //D("recv %d bytes from remote", sz);
    }

    hexdump(linebuf, sz);

    // parse linebuf, and get command need invoked
    argc = parse_cmds(linebuf, sz, &argv);
    if (!argc) {
        write(fd, "FAIL:0:protocol error\n", 22);
        return;
    }

    cmd = get_command(argv[0]);
    if (!cmd || !cmd->func) {
        write(fd, "FAIL:command not found\n", 22);
        return;
    }

    // command accepted
    sz = sprintf(linebuf, "OKAY\n");
    write(fd, linebuf, sz);

    // invoke
    cmd->func(c, argc, argv);

    // free argv
    int i;
    for (i = 0; i < argc; i++)
        free(argv[i]);
    free(argv);
}

/**
 * Handle the connect logic of control socket(daemonfd).
 */
static void ctlsock_connect_handler(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    struct sockaddr_in sin;
    socklen_t slen = sizeof(sin);
    int s;

    /* Alloc new client. */
    struct client *c = alloc_client();
    if (!c)
        return;

    /* Accept the client's connect, this is not a blocking call. */
    s = accept(fd, (struct sockaddr *)&sin, &slen);
    if (s < 0) {
        E("accept error, %s", strerror(errno));
        free(c);
        return;
    }
    D("%s:%d connected\n", inet_ntoa(sin.sin_addr), ntohs(sin.sin_port));

    c->ip.s_addr = sin.sin_addr.s_addr;
    sprintf(c->name, "%d", ntohs(sin.sin_port));

    /* Setup features of client side control socket. */
    fcntl(s, F_SETFL, O_NONBLOCK);
#if 0
    fcntl(c->socket, F_SETFD, FD_CLOEXEC);
#endif

    /* Add socket to fdevent loop. */
    fdevent_install(&c->fde, s, ctlsock_io_handler, c);
    fdevent_set(&c->fde, FDE_READ);

    /* make sure we don't close after fdevent_remove. */
    fdevent_add(&c->fde, FDE_DONT_CLOSE);

    /* Add this client to list. */
    list_add_tail(&clients, &c->clist);
    dump_clients();
}

/* Name derived from 2011-05-04, which is start time of this project. */
#define AGENTD_PORT         10504

int main()
{
    int daemon_fd;
    int signal_recv_fd;
    int fd_count;
    int s[2];
    struct sigaction act;
    struct fdevent *fde;

    /* setup SIGCHLD handler */
    {
        act.sa_handler = sigchld_handler;
        act.sa_flags = SA_NOCLDSTOP;
        sigemptyset(&act.sa_mask);
        sigaddset(&act.sa_mask, SIGCHLD);
        act.sa_restorer = NULL;
        sigaction(SIGCHLD, &act, 0);
    }

    /* init agent socket */
    daemon_fd = local_socket(AGENTD_PORT);

    /* create a mechansim for sigchld handler */
    if (socketpair(AF_UNIX, SOCK_STREAM, 0, s) == 0) {
        signal_fd = s[0];
        signal_recv_fd = s[1];
        fcntl(s[0], F_SETFD, FD_CLOEXEC);
        fcntl(s[0], F_SETFL, O_NONBLOCK);
        fcntl(s[1], F_SETFD, FD_CLOEXEC);
        fcntl(s[1], F_SETFL, O_NONBLOCK);
    }

    /* make sure we are ready */
    if ((daemon_fd < 0) ||
        (signal_recv_fd < 0)) {
        E("init failture");
        exit(1);
    }

    /* make sure we don't close-on-exec */
    fcntl(daemon_fd, F_SETFD, 0);

    fde = fdevent_create(daemon_fd, ctlsock_connect_handler, 0);
    fdevent_set(fde, FDE_READ);

    fde = fdevent_create(signal_recv_fd, sigchld_handler, 0);
    fdevent_set(fde, FDE_READ);

    D("agentd start");
    /* main loop */
    fdevent_loop();

    D("agentd exit");
    exit(0);
}
