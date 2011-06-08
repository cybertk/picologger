/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * -- https://github.com/kyan-he/picologger/raw/master/j2me/com/github/picologger/Log.java --
 * 
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

package com.github.picologger;

import java.io.IOException;
import java.util.Vector;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;
import javax.microedition.io.SocketConnection;

import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.RadioInfo;
import net.rim.device.api.system.WLANInfo;

import com.github.picologger.syslog.Syslog;
import com.github.picologger.syslog.Timestamp;

/**
 * API for sending log output.
 * 
 * <p>
 * Generally, use the Log.v() Log.d() Log.i() Log.w() and Log.e() methods.
 * 
 * <p>
 * The order in terms of verbosity, from least to most is ERROR, WARN, INFO,
 * DEBUG, VERBOSE. Verbose should never be compiled into an application except
 * during development. Debug logs are compiled in but stripped at runtime.
 * Error, warning and info logs are always kept.
 * 
 * <p>
 * <b>Tip:</b> A good convention is to declare a <code>TAG</code> constant in
 * your class:
 * 
 * <pre>
 * private static final String TAG = &quot;MyActivity&quot;;
 * </pre>
 * 
 * and use that in subsequent calls to the log methods.
 * </p>
 * 
 * <p>
 * <b>Tip:</b> Don't forget that when you make a call like
 * 
 * <pre>
 * Log.v(TAG, &quot;index=&quot; + i);
 * </pre>
 * 
 * that when you're building the string to pass into Log.d, the compiler uses a
 * StringBuilder and at least three allocations occur: the StringBuilder itself,
 * the buffer, and the String object. Realistically, there is also another
 * buffer allocation and copy, and even more pressure on the gc. That means that
 * if your log message is filtered out, you might be doing significant work and
 * incurring significant overhead.
 */
public abstract class Log
{
    
    /**
     * Priority constant for the log method; use Log.v.
     */
    public static final int VERBOSE = 2;
    
    /**
     * Priority constant for the log method; use Log.d.
     */
    public static final int DEBUG = 3;
    
    /**
     * Priority constant for the log method; use Log.i.
     */
    public static final int INFO = 4;
    
    /**
     * Priority constant for the log method; use Log.w.
     */
    public static final int WARN = 5;
    
    /**
     * Priority constant for the log method; use Log.e.
     */
    public static final int ERROR = 6;
    
    /**
     * Priority constant for the log method.
     */
    public static final int ASSERT = 7;
    
    /**
     * Send a {@link #VERBOSE} log message.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int v(String tag, String msg)
    {
        return log(LOG_ID_MAIN, VERBOSE, tag, msg);
    }
    
    /**
     * Send a {@link #DEBUG} log message.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int d(String tag, String msg)
    {
        return log(LOG_ID_MAIN, DEBUG, tag, msg);
    }
    
    /**
     * Send an {@link #INFO} log message.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int i(String tag, String msg)
    {
        return log(LOG_ID_MAIN, INFO, tag, msg);
    }
    
    /**
     * Send a {@link #WARN} log message.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int w(String tag, String msg)
    {
        return log(LOG_ID_MAIN, WARN, tag, msg);
    }
    
    /**
     * TODO: Checks to see whether or not a log for the specified tag is
     * loggable at the specified level.
     * 
     * The default level of any tag is set to INFO. This means that any level
     * above and including INFO will be logged. Before you make any calls to a
     * logging method you should check to see if your tag should be logged. You
     * can change the default level by setting a system property: 'setprop
     * log.tag.&lt;YOUR_LOG_TAG> &lt;LEVEL>' Where level is either VERBOSE,
     * DEBUG, INFO, WARN, ERROR, ASSERT, or SUPPRESS. SUPPRESS will turn off all
     * logging for your tag. You can also create a local.prop file that with the
     * following in it: 'log.tag.&lt;YOUR_LOG_TAG>=&lt;LEVEL>' and place that in
     * /data/local.prop.
     * 
     * @param tag
     *            The tag to check.
     * @param level
     *            The level to check.
     * @return Whether or not that this is allowed to be logged.
     * @throws IllegalArgumentException
     *             is thrown if the tag.length() > 23.
     */
    public static native boolean isLoggable(String tag, int level);
    
    /**
     * Send an {@link #ERROR} log message.
     * 
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     */
    public static int e(String tag, String msg)
    {
        return log(LOG_ID_MAIN, ERROR, tag, msg);
    }
    
    /** @hide */
    public static final int LOG_ID_MAIN = 0;
    
    /** @hide */
    public static final int LOG_ID_RADIO = 1;
    
    /** @hide */
    public static final int LOG_ID_EVENTS = 2;
    
    /** @hide */
    public static final int LOG_ID_SYSTEM = 3;
    
    /**
     * Logging Queue.
     */
    private static LogQueue sQueue;
    
    /**
     * Log dumper.
     */
    private static LogWritter sWritter;
    
    /**
     * Server will translate to real device ip.
     */
    final private static String DEFAULT_HOSTNAME = "picologger_server";
    
    /**
     * Host name.
     */
    private static String sHostname;
    
    // Init.
    static
    {
        // Init hostname.
        sHostname = getHostname();
        if (null == sHostname)
        {
            sHostname = DEFAULT_HOSTNAME;
        }
        
        // Init queue.
        sQueue = new LogQueue(1000);
        
        // Init writter.
        sWritter = new LogWritter(sQueue);
        sWritter.start();
    }
    
    /**
     * Returns the local IP address.
     */
    private static String getHostname()
    {
        String localAddr = null;
        SocketConnection conn = null;
        
        try
        {
            conn = (SocketConnection) Connector.open("socket://www.google.com:80");
            localAddr = conn.getLocalAddress();
        }
        catch (Throwable e)
        {
        }
        finally
        {
            if (null != conn)
            {
                try
                {
                    conn.close();
                }
                catch (IOException e)
                {
                    
                }
            }
        }
        
        return localAddr;
    }
    
    /**
     * Low-level logging call.
     * 
     * @param priority
     *            The priority/type of this log message
     * @param tag
     *            Used to identify the source of a log message. It usually
     *            identifies the class or activity where the log call occurs.
     * @param msg
     *            The message you would like logged.
     * @return The number of bytes written.
     */
    private static int log(int priority, String tag, String msg)
    {
        return log(LOG_ID_MAIN, priority, tag, msg);
    }
    
    private static int log(int bufID, int priority, String tag, String msg)
    {
        // Setup Syslog.
        Syslog log = new Syslog();
        log.setTimestamp(Timestamp.currentTimestamp());
        log.setHostname(sHostname);
        log.setFacility(priority);
        log.setProcid(tag);
        log.setMsg(msg);
        
        // Push to queue.
        sQueue.push(log);
        
        return 0;
    }
    
    static class LogQueue
    {
        // should protected by mutex
        private Vector mQueue = new Vector();
        
        private int mQueueMaxSize;
        
        public LogQueue(int sz)
        {
            mQueueMaxSize = sz;
            reset();
        }
        
        /**
         * Clear the buffer.
         */
        private void reset()
        {
            mQueue.removeAllElements();
        }
        
        /**
         * Push one log record into queue.
         */
        public void push(Syslog log)
        {
            if (mQueue.size() == mQueueMaxSize)
            {
                // Queue overflow, drop the log.
                
                return;
            }
            
            // TODO: The system will hang, if the push is called in high-frequency.
            // TODO: We do not want acquire a mutex-lock.
            synchronized (mQueue)
            {
                mQueue.addElement(log);
                mQueue.notify();
            }
        }
        
        /**
         * Returns more than one records.
         */
        public Syslog[] pop()
        {
            synchronized (mQueue)
            {
                try
                {
                    mQueue.wait();
                }
                catch (InterruptedException e)
                {
                    // hmm...
                }
                
                Syslog[] logs = new Syslog[mQueue.size()];
                mQueue.copyInto(logs);
                reset();
                
                return logs;
            }
        }
    }
    
    static class LogWritter extends Thread
    {
        
        private String logdUri = "datagram://10.60.5.62:10505";
        
        final private LogQueue mQueue;
        
        private DatagramConnection mConnection;
        
        public LogWritter(LogQueue buf)
        {
            super(".LogerWriter");
            
            mQueue = buf;
            
            if ((CoverageInfo.getCoverageStatus(RadioInfo.WAF_WLAN, true) & CoverageInfo.COVERAGE_DIRECT) == CoverageInfo.COVERAGE_DIRECT)
            {
                logdUri += "/ ;interface=wifi";
            }
            try
            {
                mConnection = (DatagramConnection) Connector.open(logdUri);
            }
            catch (IOException e)
            {
            }
        }
        
        public void run()
        {
            for (;;)
            {
                // Get the date from queue and send them out.
                Syslog[] logs = mQueue.pop();
                write(logs);
            }
        }
        
        /**
         * Write the logs out.
         * 
         * @param logs
         *            The logs need be written.
         */
        private void write(Syslog[] logs)
        {
            
            Datagram dg;
            String data = "";
            try
            {
                for (int i = 0; i < logs.length; i++)
                {
                    
                    String raw = logs[i].encode();
                    
                    if (data.length() + raw.length() > 1000)
                    {
                        dg = mConnection.newDatagram(data.getBytes(),
                                data.length(),
                                logdUri);
                        mConnection.send(dg);
                        
                        // Reset buffer.
                        data = "";
                    }
                    
                    data += raw + "\n";
                }
                
                // Send the last packet.
                dg = mConnection.newDatagram(data.getBytes(),
                        data.length(),
                        logdUri);
                mConnection.send(dg);
            }
            catch (IOException e)
            {
                // ahh...
            }
        }
    }
}
