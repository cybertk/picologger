package com.github.picologger.logviewer;

import java.io.OutputStream;
import java.io.PrintStream;
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
        
        WorkerThread worker = new WorkerThread(new InetSocketAddress(
                "10.60.5.62", 10504), new LogStream(null));
        
        worker.sendCommand("FLTR -a 10.60.5.92");
        worker.start();
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
