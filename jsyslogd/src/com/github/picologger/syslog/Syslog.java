/*
 * Copyright (C) 2011 cybertk
 *
 * -- https://github.com/kyan-he/picologger/raw/master/jsyslogd/src/com/github/picologger/syslog/Syslog.java --
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


/**
 * Implement RFC5424 and RFC3164
 * 
 * @author cybertk
 * 
 */
public class Syslog
{
    private int facility;
    
    private int severity;
    
    private int version;
    
    private String timestamp;
    
    private String hostname;
    
    private String appname;
    
    private String procid;
    
    private String msgid;
    
    //TODO: define structured-data struct
    private String sd;
    
    private String msg;
    
    // Indicates this is RFC3164 or RFC5424
    private boolean bsd;
    
    public Syslog()
    {
        // Local use 0 (local0).
        facility = 16;
        
        // Debug.
        severity = 7;
        
        // RFC Specified.
        version = 1;
        timestamp = "-";
        hostname = "-";
        appname = "-";
        procid = "-";
        msgid = "-";
        sd = "-";
        msg = "";
        
    }
    
    public int getFacility()
    {
        return facility;
    }
    
    public void setFacility(int facility)
    {
        this.facility = facility;
    }
    
    public int getSeverity()
    {
        return severity;
    }
    
    public void setSeverity(int severity)
    {
        this.severity = severity;
    }
    
    public int getVersion()
    {
        return version;
    }
    
    public void setVersion(int version)
    {
        this.version = version;
    }
    
    public String getTimestamp()
    {
        return timestamp;
    }
    
    public void setTimestamp(String timestamp)
    {
        this.timestamp = timestamp;
    }
    
    public String getHostname()
    {
        return hostname;
    }
    
    public void setHostname(String hostname)
    {
        this.hostname = hostname;
    }
    
    public String getAppname()
    {
        return appname;
    }
    
    public void setAppname(String appname)
    {
        this.appname = appname;
    }
    
    public String getProcid()
    {
        return procid;
    }
    
    public void setProcid(String procid)
    {
        this.procid = procid;
    }
    
    public String getMsgid()
    {
        return msgid;
    }
    
    public void setMsgid(String msgid)
    {
        this.msgid = msgid;
    }
    
    public String getSd()
    {
        return sd;
    }
    
    public void setSd(String sd)
    {
        this.sd = sd;
    }
    
    public String getMsg()
    {
        return msg;
    }
    
    public void setMsg(String msg)
    {
        this.msg = msg;
    }
    
    public String encode()
    {
        String str = "";
        
        // Generates PRI.
        int pri = (facility << 3) + severity;
        str += "<" + pri + ">";
        
        // Generates version.
        str += version + " ";
        str += timestamp + " ";
        str += hostname + " ";
        str += appname + " ";
        str += procid + " ";
        str += msgid + " ";
        str += sd;
        
        // <code>isEmpty()</code> is unavailable in J2ME.
        if (!"".equals(msg))
        {
            str += " " + msg;
        }
        
        return str;
    }
    
    public String toString()
    {
        String str = "";
        
        str += "facility: " + facility + "\n";
        str += "severity: " + severity + "\n";
        str += "version: " + version + "\n";
        str += "timestamp: " + timestamp + "\n";
        str += "hostname: " + hostname + "\n";
        str += "appname: " + appname + "\n";
        str += "procid: " + procid + "\n";
        str += "msgid: " + msgid + "\n";
        str += "sd: " + sd + "\n";
        str += "msg: " + msg + "\n";
        
        return str;
    }
}
