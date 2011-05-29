package com.davidko.logclient;

public interface IShellOutputReceiver {
    public void addOutput(byte[] data, int offset, int length);
    public void flush();
    public boolean isCancelled();
};
