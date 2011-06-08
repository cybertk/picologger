package com.github.picologger.syslog;

import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Demo
{
    
    private static String record = "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It's time to make the do-nuts.";
    
    private static String recordWithSd = "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1111\"] [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry...";
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        
        // TODO Auto-generated method stub
        
        Syslog l = Parser.parse(record);
        System.out.println(l);
        
        l = Parser.parse(recordWithSd);
        System.out.println(l);
        
        Syslog l1 = new Syslog();
        String raw = l1.encode();
        System.out.println(raw);
        
        l = Parser.parse(raw);
        System.out.println(l);
        System.out.println(Timestamp.currentTimestamp());
        
        ServerSocket s = null;
        try
        {
            s = new ServerSocket(58858);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        while (true)
        {
            Socket c;
            try
            {
                c = s.accept();
                
                InputStream is = c.getInputStream();
                
                byte[] bytes = new byte[4096];
                int bytesRead = is.read(bytes);
                
                if (bytesRead == -1)
                {
                    break;
                }
                
                Syslog log = Parser.parse(new String(bytes, 0, bytesRead));
                System.out.println(log);
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}
