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

/**
 * Implement RFC5424 and RFC3164
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include "log.h"
#include "syslog.h"

#define SP   ' '
#define NILVALUE "-"


/**
 * Returns syslog message.
 */
char* encode(syslog_record *record)
{
    /*TODO: Implement*/

    return 0;
}

/**
 * strtok() will cause SIGSEGV
 */
char* parse_token(const char *s, char token, char* saved)
{
    char *pos, *pos0;

    pos = (char *)s;

    if (NULL == pos) {
        saved = "-";
        return NULL;
    }

    // Find token
    pos0 = strchr(s, ' ');
    if (!pos0) {
        saved = "-";
        return NULL;
    }

    /* TODO: validate the LENGTH */
    strncpy(saved, pos, (size_t)(pos0 - pos));

    return ++pos0;
}

static int parse_rfc3164(const char* log, size_t sz, syslog_record *record)
{

    char *pos, *pos0, *end;

    pos = (char *)log;
    pos0 = pos;
    end = pos + sz;

    // Special case for no HEADER.
    if (*pos == ' ') {

        strncpy(record->msg, pos, end - pos);
        return 0;
    }

    // Parse timestamp, uncompatible with RFC5424.
    int tokens = 3;
    while (tokens) {

        pos0 = strchr(pos0, ' ');
        if (!pos0) {
            D("malformed syslog record.");
            return -1;
        }
        ++pos0;
        --tokens;
    }
    strncpy(record->timestamp, pos, pos0 - pos);
    pos = pos0;

    // Parse hostname
    pos = parse_token(pos, SP, record->hostname);

    // Parse msg
    record->msg = strndup(pos, end - pos);
}

static int parse_rfc5424(const char* log, size_t sz, syslog_record *record)
{
    char *pos, *pos0, *end;

    pos = (char *)log;
    end = pos + sz;

    // Skip version
    pos += 2;

    // Skip Header
    pos0 = (char *)log;
    int tokens = 6;
    while (tokens) {

        pos0 = strchr(pos0, ' ');
        if (!pos0) {
            D("malformed syslog record.");
            return -1;
        }
        ++pos0;
        --tokens;
    }

    // Parse timestamp
    pos = parse_token(pos, SP, record->timestamp);

    // Parse hostname
    pos = parse_token(pos, SP, record->hostname);

    // Parse app-name
    pos = parse_token(pos, SP, record->appname);

    // Parse procid
    pos = parse_token(pos, SP, record->procid);

    // Parse msgid
    pos = parse_token(pos, SP, record->msgid);

    // Parse sd
    if (*pos0 == '[') {

        char *end;

        pos = pos0;

        // There may be more than one sd-element
        while (pos = strchr(pos, ']')) {

            ++pos;
            if (*(pos - 2) != '\\')
                end = pos;
        }

        pos = end;
    } else {
        // NILVALUE, "-".
        pos = pos0 + 1;
    }
    record->sd = strndup(pos0, pos - pos0);

    // Parse message
    if ((pos - log) < strlen(log)) {

        pos += 1;
        // TODO: BOM supports
        record->msg = strndup(pos, end - pos);
    } else {

        record->msg = "";
    }
}
/**
 * ABNF definition of syslog message:
 *
 * SYSLOG-MSG      = HEADER SP STRUCTURED-DATA [SP MSG]
 * HEADER          = PRI VERSION SP TIMESTAMP SP HOSTNAME
 *                   SP APP-NAME SP PROCID SP MSGID
 *
 * Returns -1 if error occurs
 */
static int parse_line(const char* line, size_t sz, syslog_record *record)
{
    char *pos, *pos0, *end;

    pos = (char *)line;
    end = pos + sz;

    // Reset.
    memset(record, 0, sizeof(syslog_record));

    // Validate syslog format.
    pos0 = strchr(pos, '>');
    if (*pos != '<' || (pos0 - pos) > 4){
        D("malformed syslog record, cannot parse pri.");
        return -1;
    }
    ++pos;

    {
        // Parse facility and severity.
        int pri = atoi(pos);
        record->facility  = pri >> 3;
        record->severity = pri & 0x7;
    }
    pos = pos0 + 1;

    // Parse syslog version.
    record->version = *pos - '0';

    if (record->version == SYSLOG_VERSION_RFC5424) {
        return parse_rfc5424(pos, end - pos, record);
    } else {
        return parse_rfc3164(pos, end - pos, record);
    }
}

/**
 * Parse syslogs from given data.
 *
 * Returns -1 if error occurs
 */
int syslog_parse(char *data, size_t sz, syslog_record *record)
{
    // TODO: support 1-line.
    return parse_line(data, sz, record);
}

void syslog_dump(syslog_record *record)
{
    D("Facility: %d", record->facility);
    D("severity: %d", record->severity);
    D("Version: %d", record->version);
    D("Timestamp: %s", record->timestamp);
    D("Hostname: %s", record->hostname);
    if (SYSLOG_VERSION_RFC5424 == record->version) {
        D("App-name: %s", record->appname);
        D("Procid: %s", record->procid);
        D("Msgid: %s", record->msgid);
        D("Sd: %s", record->sd);
    }
    D("Msg: %s", record->msg);
    D("");
}

syslog_record* syslog_alloc()
{
    syslog_record *x = malloc(sizeof(syslog_record));

    if (x)
        memset(x, 0, sizeof(syslog_record));

    return x;
}

void syslog_destroy(syslog_record* record)
{

    if (record->sd)
        free(record->sd);

    if (record->msg)
        free(record->msg);

    free(record);
}

#ifdef SYSLOG_TEST
int main()
{
    syslog_record r;
    char *log = "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It\'s time to make the do-nuts.";

    char *log_with_sds = "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1111\"] [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry...";

    char *log_without_msg = "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - -";

    printf("== Nomral Log ==\n");
    parse_line(log, strlen(log), &r);
    syslog_dump(&r);

    printf("== Log with two SDs ==\n");
    parse_line(log_with_sds, strlen(log_with_sds), &r);
    syslog_dump(&r);

    printf("== Log without both SD and Message ==\n");
    parse_line(log_without_msg, strlen(log_without_msg), &r);
    syslog_dump(&r);
}
#endif
