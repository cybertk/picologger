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
 *      Kyan He <kyan.ql.he@gmail.com> @ Tue May 17 00:06:05 CST 2011
 *
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <netinet/in.h>
#include <getopt.h>
#include <errno.h>
#include <time.h>

#include "log.h"
#include "syslog.h"
#include "list.h"
#include "client.h"

extern struct listnode clients;

static struct timeval g_last_active_time;

struct syslog_filter {

    /* linked list */
    struct listnode list;

    /* filter item */
    syslog_record filter;
};

list_declare(history);

/**
 * Client Statistics.
 */
struct statistics {

    /* Host address */
    in_addr_t addr;

    /* Host port */
    short port;

    /* Receiving log counts */
    int count;

    struct listnode list;
};

/**
 * Update statistics.
 */
static void history_update(in_addr_t addr, short port)
{

    struct listnode *node;
    struct statistics *s;
    int found = 0;

    list_for_each(node, &history) {

        s = node_to_item(node, struct statistics, list);

        if (addr == s->addr && port == s->port) {

            /* Update log counter */
            s->count++;

            found = 1;
            break;
        }
    }

    if (!found) {

        /* Insert new host into history */
        s = malloc(sizeof(struct statistics));
        if (!s)
            return;

        s->addr = addr;
        s->port = port;
        s->count = 1;

        list_add_tail(&history, &s->list);
    }
}

/**
 * Reset History
 */
static void history_clear()
{

    struct listnode *node;
    struct statistics *s;

    list_for_each(node, &history) {

        s = node_to_item(node, struct statistics, list);

        /* Remove from history list. */
        list_remove(&s->list);

        /* free mem we use. */
        free(s);
    }
}

/**
 * Dump history.
 */
static void history_dump(int fd)
{
    struct listnode *node;
    struct statistics *s;
    struct timeval now;

    gettimeofday(&now, NULL);

    FILE *o = fdopen(fd, "w");

    fprintf(o, "[%d.%d] History of last %d second.\n",
            now.tv_sec,
            now.tv_usec,
            now.tv_sec - g_last_active_time.tv_sec);

    list_for_each(node, &history) {

        s = node_to_item(node, struct statistics, list);

        fprintf(o, "    %s.%d: %d\n", inet_ntoa(s->addr), ntohs(s->port), s->count);
    }

    fflush(o);
}

/**
 * Check whether current log'address match client's filter
 *
 * Returns 1 if given syslog passed the filter.
 */
static int filter_match(struct listnode *filters, syslog_record *log)
{
    LOG_FUNCTION_NAME

    struct listnode *node;
    struct syslog_filter *f;
    list_for_each(node, filters) {

        f = node_to_item(node, struct syslog_filter, list);

        /* TODO: match all components */
        char *hostname = f->filter.hostname;
        if (hostname && !strcmp(hostname, log->hostname)) {
            return 1;
        }

    }

    return 0;
}

/**
 *
 * @param from
 *      Ip address of the log sender.
 */
static void notify_clients(char *from, char *buf, size_t sz)
{
    syslog_record r;

#if 0
    D("%s syslog: %s", from, buf);
#endif

    buf[sz] = 0;
    syslog_parse(buf, sz, &r);

    /* TODO: remove this feature */
    /* Append linefeed */
    if (buf[sz - 1] != '\n') {
        buf[sz] = '\n';
        ++sz;
    }

    char *syslog = buf;

    if (!r.hostname ||
            !strcmp(r.hostname, "picologger_server")) {
        r.hostname = from;

        // TODO:
        //syslog = syslog_encode();
    }

    /* notify clients */
    struct listnode *node;
    struct client *c;
    list_for_each(node, &clients) {
        c = node_to_item(node, struct client, clist);
        //if (c->flags & CLIENT_MONITOR && filter_match(c, l)) {
        // TODO: Notify clients according to filtes.
        if ((c->running_command.cmd == get_command("FLTR"))
                && filter_match(
                    (struct listnode *) c->running_command.cookie, &r)) {

            write(c->fde.fd, buf, sz);
        }
    }
}

//TODO: re-calculate
#define LOGD_MAX_PACKET_SZ 1200
static void syslog_sock_handler(int fd, unsigned events, void* cookie)
{
    LOG_FUNCTION_NAME

    char buf[LOGD_MAX_PACKET_SZ];
    int sz;
    struct sockaddr_in sa;
    int x = sizeof(sa);

    //sz = read(fd, buf, LOGD_MAX_PACKET_SZ);
    sz = recvfrom(fd, buf, LOGD_MAX_PACKET_SZ, 0,
            (struct sockaddr *)&sa, &x);

    if (sz == 0) {
        /* Close by remote */
        // TODO: check all services under client, and destroy client
        D("--- logger IO CLOSE ---");
        return;
    } else if (sz < 0) {
        perror("read");
        return;
    } else {

        history_update(sa.sin_addr.s_addr, sa.sin_port);

#if 0
        /* send statistics to stdout */
        struct timeval now;

        gettimeofday(&now, NULL);
        if ((now.tv_sec - g_last_active_time.tv_sec) > 10) {

            /* dump the statistics in last 10s, then reset it. */
            history_dump();
            history_clear();

            g_last_active_time = now;
        }
#endif

        // Relay to clients.
        notify_clients(inet_ntoa(sa.sin_addr), buf, sz);
    }
}

#define LOGD_PORT 10505
static int syslog_service_start()
{
    /* init logd datagram */
    int logd_fd = local_datagram(LOGD_PORT);

    struct fdevent* fde = fdevent_create(logd_fd, syslog_sock_handler, 0);
    fdevent_set(fde, FDE_READ);
}

/**
 * Dump syslog filters.
 */
static void dump_filters(struct listnode *filters)
{
    struct listnode *node;
    struct syslog_filter *f;

    list_for_each(node, filters) {

        f = node_to_item(node, struct syslog_filter, list);

        D("host: %s, tag: %s", f->filter.hostname, f->filter.procid);
    }
}

int cmd_fltr_func(struct client *client, int argc, char * const argv[])
{
    LOG_FUNCTION_NAME

    int opt;
    struct client *c;

    client->flags |= CLIENT_MONITOR;

    /* Check whether the service is started. */
    int service_found = 0;
    struct listnode *node;

    list_for_each(node, &clients) {
        c = node_to_item(node, struct client, clist);

        /* TODO: If the same client send FLTR more than one time, 
         * the syslog server will start every time. */
        if ((c->running_command.cmd == get_command("FLTR"))
                && c != client)
            service_found = 1;
    }

    /* Start syslog receiver if it have not been started. */
    if (!service_found) {
        D("No FLTR found, start service.");
        syslog_service_start();
    }


    /* Init filter list. */
    /* TODO: use syslog_filter as list head? */
    struct listnode *filters = malloc(sizeof(*filters));
    if (!filters)
        return -ENOMEM;

    list_init(filters);

    /* Reset getopt. */
    /* Seems argv[argc] = 0 will not reset optind automatically */
    optind = 1;

    /**
     * Usage: 
     *      Setup filter.
     *      syslog [ -a hostname ] [ -t tag ]
     *
     *      Dump statistics.
     *      syslog -s
     *
     */
#if 0
    int x = 0;
    while (x < argc) {
        D(" argv[%d], %p: %s, %d", x, argv[x], argv[x], strlen(argv[x]));
        ++x;
    }
#endif
    while ((opt = getopt(argc, argv, "sa:t:")) != -1) {

        struct syslog_filter *f;

        switch (opt) {
            case 's':

                history_dump(client->fde.fd);

                // mission completed.
                client_destroy(client);
                list_remove(&client->clist);

                break;

            case 'a':

                f = malloc(sizeof(*f));
                if (!f)
                    return -ENOMEM;

                f->filter.hostname = strdup(optarg);

                //TODO: find duplicate
                list_add_tail(filters, &f->list);
                break;

            case 't':

                //TODO: support priority

                f= malloc(sizeof(*f));
                if (!f)
                    return -ENOMEM;

                f->filter.procid = strdup(optarg);

                //TODO: find duplicate
                list_add_tail(filters, &f->list);
                break;
        }
    }

    /* Store filter list in client. */
    client->running_command.cookie = filters;

    dump_filters(filters);
}

