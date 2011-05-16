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

list_declare(clients);

//extern struct command commands[];
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
int parse_cmds(char *line, size_t sz, char ***argv)
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

static int g_task_id = 1;

#define CMD_MAX_SIZE 1024
static void handle_socketio_func(int fd, unsigned events, void* cookie)
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
        D("recv %d bytes from remote", sz);
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
    for (i = 0; argv[i]; i++)
        free(argv[i]);
    free(argv);
}

static void handle_connect_func(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    struct sockaddr_in sin;
    socklen_t slen;
    int s;

    struct client *c = alloc_client();
    if (!c)
        return;

    slen = sizeof(sin);
    s = accept(fd, (struct sockaddr *)&sin, &slen);
    if (s < 0) {
        E("accept error, %s", strerror(errno));
        free(c);
        return;
    }
    D("%s:%d connected\n", inet_ntoa(sin.sin_addr), ntohs(sin.sin_port));

    c->ip.s_addr = sin.sin_addr.s_addr;
    sprintf(c->name, "%d", ntohs(sin.sin_port));

    fcntl(s, F_SETFL, O_NONBLOCK);
    //fcntl(c->socket, F_SETFD, FD_CLOEXEC);


    fdevent_install(&c->fde, s, handle_socketio_func, c);
    fdevent_set(&c->fde, FDE_READ);

    /* make sure we don't close after fdevent_remove */
    fdevent_add(&c->fde, FDE_DONT_CLOSE);

    list_add_tail(&clients, &c->clist);
    dump_clients();
}

struct log_record {
    struct in_addr from;
    struct timeval ts;
    int priority;
    int size;
    char tag[];
};

// check whether current log'address match client's filter, 1 for ture
static int addr_match(struct client *c, struct in_addr *in)
{
    LOG_FUNCTION_NAME

    struct listnode *node;
    struct addr_filter *af;

    // match all if not filter set
    if (list_empty(&c->addrs))
        return 1;

    // match address
    list_for_each(node, &c->addrs) {
        af = node_to_item(node, struct addr_filter, list);

        if (af->addr.s_addr == in->s_addr) {
            D("address match");
            return 1;
        }
    }

    return 0;
}

// check whether current log'tag match client's filter, 1 for ture
static int tag_match(struct client *c, const char *tag, int priority)
{
    LOG_FUNCTION_NAME

    struct listnode *node;
    struct tag_filter *tf;

    // match all if not filter set
    if (list_empty(&c->tags))
        return 1;

    // match tags
    list_for_each(node, &c->tags) {
        tf = node_to_item(node, struct tag_filter, list);

        if ((priority >= tf->priority) && !(strcmp(tag, tf->tag))) {
            D("tag match");
            return 1;
        }
    }

    return 0;
}

// check whether current log match client's filter, 1 for ture
static int filter_match(struct client *c, struct log_record *l)
{
    LOG_FUNCTION_NAME

    return addr_match(c, &l->from) && tag_match(c, l->tag, ntohl(l->priority));
}

static void notify_clients(char *buf, size_t sz)
{
        // parse packet
        //
        struct log_record *l;

        l = (struct log_record *)buf;

        D("prioriry: %d, %s\n", ntohl(l->priority), l->tag);

        /* notify clients */
        struct listnode *node;
        struct client *c;
        list_for_each(node, &clients) {
            c = node_to_item(node, struct client, clist);
            if (c->flags & CLIENT_MONITOR && filter_match(c, l)) {

                D("nofity client %s", c->name);
                write(c->fde.fd, buf, sz + sizeof(struct in_addr));
            }
        }

}

//TODO: re-calculate
#define LOGD_MAX_PACKET_SZ 1200
static void handle_logger_func(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    // insert ip address at header head
    char buf[LOGD_MAX_PACKET_SZ + sizeof(struct in_addr)];
    int sz;
    struct sockaddr_in sa;
    int x = sizeof(sa);

    //sz = read(fd, buf, LOGD_MAX_PACKET_SZ);
    sz = recvfrom(fd, buf + sizeof(struct in_addr),
            LOGD_MAX_PACKET_SZ, 0,
            (struct sockaddr *)&sa, &x);
    if (sz == 0) {
        /* close by remote */
        // TODO: check all services under client, and destroy client
        D("--- logger IO CLOSE ---");
        return;
    } else if (sz < 0) {
        perror("read");
        return;
    } else {
        D("recv %d bytes from %s\n", sz, inet_ntoa(sa.sin_addr));

        memcpy(buf, &sa.sin_addr, sizeof(struct in_addr));

        notify_clients(buf, sz);

    }
}

static void handle_sigchld_func(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    pid_t pid;
    int status;
    char tmp[1024];

    read(fd, tmp, 1024);

    while ( (pid = waitpid(-1, &status, WNOHANG)) == -1 && errno == EINTR);
    if (pid <= 0) {
        D("watpid error");
        return;
    }

    D("waitpid returned pid %d, status=%08x", pid, status);

}

#define LOGD_PORT           20504
#define LOGD_SOCKET_PATH    "syslog-relay"
#define AGENTD_PORT         LOGD_PORT

int main()
{
    int daemon_fd;
    int logd_fd;
    int relay_fd;
    int signal_recv_fd;
    int fd_count;
    int s[2];
    struct pollfd ufds[2];
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

    /* init logd datagram */
    logd_fd = local_datagram(LOGD_PORT);

    /* init syslog relay unix datagram */
    relay_fd = unix_datagram_server(LOGD_SOCKET_PATH);

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
        (logd_fd < 0) ||
        (relay_fd < 0) ||
        (signal_recv_fd < 0)) {
        E("init failture");
        exit(1);
    }

    /* make sure we don't close-on-exec */
    fcntl(daemon_fd, F_SETFD, 0);

    fde = fdevent_create(daemon_fd, handle_connect_func, 0);
    fdevent_set(fde, FDE_READ);

    fde = fdevent_create(logd_fd, handle_logger_func, 0);
    fdevent_set(fde, FDE_READ);

    fde = fdevent_create(relay_fd, handle_logger_func, 0);
    fdevent_set(fde, FDE_READ);

    fde = fdevent_create(signal_recv_fd, handle_sigchld_func, 0);
    fdevent_set(fde, FDE_READ);

    D("agentd start");
    /* loop */
    fdevent_loop();

    D("agentd exit");
    exit(0);
}
