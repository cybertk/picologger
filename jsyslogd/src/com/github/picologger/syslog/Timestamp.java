/*
 * Copyright (C) 2011 cybertk
 *
 * -- https://github.com/kyan-he/picologger/raw/master/jsyslogd/src/com/github/picologger/syslog/Timestamp.java--
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.picologger.syslog;

import java.util.Calendar;
import java.util.Date;

public class Timestamp
{
    private static Calendar sCalendar = Calendar.getInstance();
    
    public static String currentTimestamp()
    {
        // Add the TIMESTAMP field of the HEADER
        // Time format is "Mmm dd hh:mm:ss". For more info see rfc3164.
        
        long currentTime = System.currentTimeMillis();
        sCalendar.setTime(new Date(currentTime));
        
        String str = "";
        str += sCalendar.get(Calendar.YEAR) + "-";
        
        final int month = sCalendar.get(Calendar.MONTH) + 1;
        if (month < 10)
        {
            str += 0;
        }
        str += month + "-";
        
        final int day = sCalendar.get(Calendar.DAY_OF_MONTH);
        if (day < 10)
        {
            str += 0;
        }
        str += day;
        
        str += "T";
        
        final int hour = sCalendar.get(Calendar.HOUR_OF_DAY);
        if (hour < 10)
        {
            str += 0;
        }
        str += hour + ":";
        
        final int minute = sCalendar.get(Calendar.MINUTE);
        if (minute < 10)
        {
            str += 0;
        }
        str += minute + ":";
        
        final int second = sCalendar.get(Calendar.SECOND);
        if (second < 10)
        {
            str += 0;
        }
        str += second + ".";
        
        final int milli = sCalendar.get(Calendar.MILLISECOND);
        str += milli;
        
        str += "Z";
        
        return str;
    }
}
