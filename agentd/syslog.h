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

#ifndef __SYSLOG_H__
#define __SYSLOG_H__

/**
 * Syslog Structure.
 */
struct syslog_record {
    char facility;
    char severity;
    char version;
    char *timestamp;
    char *hostname;
    char *appname;
    char *procid;
    char *msgid;
    //TOOD: define structured-data struct
    void *sd;
    char *msg;

    // Indicates this is RFC3164 or RFC5424
    int bsd;
};

typedef struct syslog_record syslog_record;

/**
 * Syslog Parser.
 */
int syslog_parse(char*, int, syslog_record *);

/**
 * Allocate helper.
 */
syslog_record* syslog_alloc();

/**
 * Deallocate helper.
 */
void syslog_destroy(syslog_record*);

#endif
