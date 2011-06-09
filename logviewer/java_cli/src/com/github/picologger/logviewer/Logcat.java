package com.github.picologger.logviewer;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;

import com.github.picologger.syslog.Parser;
import com.github.picologger.syslog.Syslog;

public class Logcat
{
    
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        // Demo
        
        if (args.length >= 2)
        {
            InetSocketAddress addr = null;
            try
            {
                addr = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
                
            }
            catch (Exception e)
            {
                System.out.println("Cannot connect to remote server.");
                return;
            }
            
            // Construct argv.
            String s = "";
            if (args.length > 2)
            {
                for (int i = 2; i < args.length; i++)
                {
                    s += " " + args[i];
                }
            }
            else
            {
                s = " -s";
            }
            
            WorkerThread worker = new WorkerThread(addr, new LogStream(null));
            
            worker.sendCommand("FLTR" + s);
            worker.start();
        }
        else
        {
            System.out.println("Usage: logcat SERVER PORT [-a hostname]...");
        }
        
    }
    
    static class LogStream extends PrintStream
    {
        
        public LogStream(OutputStream out)
        {
            super(System.out);
        }
        
        @Override
        public PrintStream append(CharSequence csq)
        {
            try
            {
                Syslog log = Parser.parse((String) csq);
                System.out.println("<" + log.getFacility() + "> "
                        + log.getProcid() + ": " + log.getMsg());
            }
            catch (IllegalArgumentException e)
            {
                System.out.println(csq);
            }
            
            return this;
        }
    }
}
