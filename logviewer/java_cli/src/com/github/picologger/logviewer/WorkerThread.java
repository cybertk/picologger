package com.github.picologger.logviewer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.NoSuchElementException;
import java.util.Vector;

public class WorkerThread extends Thread
{
    /**
     * Remote socket address.
     */
    InetSocketAddress mRemoteSocketAddress;
    
    /**
     * Input Reader.
     */
    private BufferedReader mIn;
    
    /**
     * Output Writer.
     */
    private BufferedWriter mOut;
    
    /**
     * Printer.
     */
    private PrintStream mPrintWriter;
    
    /**
     * Command Queue, vector is synchronized.
     */
    private Vector<String> mCommandQueue = new Vector<String>();
    
    WorkerThread(InetSocketAddress addr, PrintStream writter)
    {
        mRemoteSocketAddress = addr;
        mPrintWriter = writter;
    }
    
    public void sendCommand(String cmd)
    {
        mCommandQueue.add(cmd);
    }
    
    @Override
    public void run()
    {
        try
        {
            // Init socket and connect to remote.
            Socket s = new Socket();
            s.connect(mRemoteSocketAddress);
            
            // Init Stream.
            mIn = new BufferedReader(new InputStreamReader(s.getInputStream()));
            mOut = new BufferedWriter(new OutputStreamWriter(
                    s.getOutputStream()));
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return;
        }
        
        for (;;)
        {
            threadLoop();
        }
    }
    
    private void threadLoop()
    {
        try
        {
            // Send commands.
            for (;;)
            {
                String cmd;
                try
                {
                    // Pop up the first element.
                    cmd = mCommandQueue.firstElement();
                    mCommandQueue.remove(0);
                    
                }
                catch (NoSuchElementException e)
                {
                    // Empty Queue.
                    break;
                }
                
                mPrintWriter.append("Send command: " + cmd);
                mOut.append(cmd);
                mOut.flush();
            }
            
            // Get outputs from remote.
            String str = mIn.readLine();
            mPrintWriter.append(str);
        }
        catch (IOException e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
