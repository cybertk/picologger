package com.github.picologger.syslog;

/**
 * Implement RFC5424 and RFC3164
 * 
 * @author kyanhe
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
    
    public Syslog(String record)
    {
        decode(record);
    }
    
    public Syslog()
    {
    }
    
    public void encode() {
        
    }
    
    @Override
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

    private void decode(String record) throws IllegalArgumentException
    {
        int pos0 = 0;
        int pos = 0;
        
        // Validate format.
        pos = record.indexOf('>');
        if (record.charAt(0) != '<' || pos > 4)
        {
            throw new IllegalArgumentException("Malformed syslog record.");
        }
        
        // Parse Header.
        
        // Parse facility and severity.
        int pri = Integer.decode(record.substring(1,pos));
        facility = pri >> 3;
        severity = pri & 0x7;
        
        // Parse Version.
        ++pos;
        version = record.charAt(pos) - 0x30;
            
        
        String[] token = record.split(" +", 7);
        
        timestamp = token[1];
        hostname = token[2];
        appname = token[3];
        procid = token[4];
        msgid = token[5];
        
        // Parse SD
        while (true)
        {
            pos0 = token[6].indexOf(']', pos0);
            
            if (pos0 == -1)
            {
                break;
            }
            
            ++pos0;
            
            // Record the index.
            if (token[6].charAt(pos0 - 2) != '\\')
            {
                // Make sure it's not a escaped "]".
                pos = pos0;
            }
        }
        
        sd = token[6].substring(0, pos);
        msg = token[6].substring(pos + 1);
    }
}
