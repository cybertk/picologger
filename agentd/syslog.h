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

#define SYSLOG_VERSION_RFC5424  1
#define SYSLOG_VERSION_RFC3164  0
    char version;
/**
 * @see RFC5424 section-6.2.6
 *
 * HOSTNAME        = NILVALUE / 1*255PRINTUSASCII
 *
 * APP-NAME        = NILVALUE / 1*48PRINTUSASCII
 * PROCID          = NILVALUE / 1*128PRINTUSASCII
 * MSGID           = NILVALUE / 1*32PRINTUSASCII
 *
 * TIMESTAMP       = NILVALUE / FULL-DATE "T" FULL-TIME
 * FULL-DATE       = DATE-FULLYEAR "-" DATE-MONTH "-" DATE-MDAY
 * DATE-FULLYEAR   = 4DIGIT
 * DATE-MONTH      = 2DIGIT  ; 01-12
 * DATE-MDAY       = 2DIGIT  ; 01-28, 01-29, 01-30, 01-31 based on
 *                           ; month/year
 * FULL-TIME       = PARTIAL-TIME TIME-OFFSET
 * PARTIAL-TIME    = TIME-HOUR ":" TIME-MINUTE ":" TIME-SECOND
 *                   [TIME-SECFRAC]
 * TIME-HOUR       = 2DIGIT  ; 00-23
 * TIME-MINUTE     = 2DIGIT  ; 00-59
 * TIME-SECOND     = 2DIGIT  ; 00-59
 * TIME-SECFRAC    = "." 1*6DIGIT
 * TIME-OFFSET     = "Z" / TIME-NUMOFFSET
 * TIME-NUMOFFSET  = ("+" / "-") TIME-HOUR ":" TIME-MINUTE
 */
#define RFC5424_HOSTNAME_MAX_LEN  255
#define RFC5424_APPNAME_MAX_LEN   48
#define RFC5424_PROCID_MAX_LEN    128
#define RFC5424_MSGID_MAX_LEN     32
#define RFC5424_TIMESTAMP_MAX_LEN ((4+1+2+1+2)+1+(2+1+2+1+2+7+1+5))

    // plus 1 because the string is null ended.
    char timestamp[RFC5424_TIMESTAMP_MAX_LEN+1];
    char hostname[RFC5424_HOSTNAME_MAX_LEN+1];
    char appname[RFC5424_APPNAME_MAX_LEN+1];
    char procid[RFC5424_PROCID_MAX_LEN+1];
    char msgid[RFC5424_MSGID_MAX_LEN+1];

    //TOOD: define structured-data struct
    void *sd;
    void *msg;

    // Indicates this is RFC3164 or RFC5424
    int bsd;
};

typedef struct syslog_record syslog_record;

/**
 * Syslog Parser.
 */
int syslog_parse(char*, size_t, syslog_record *);

/**
 * Allocate helper.
 */
syslog_record* syslog_alloc();

/**
 * Deallocate helper.
 */
void syslog_destroy(syslog_record*);

#endif
