package com.davidko.logclient;

import java.util.ArrayList;

public abstract class MultiLineReceiver implements IShellOutputReceiver {

	private static final byte SEP_EOF = '\0';

	private byte[] mUnfinishedLine = null;
	
	private final ArrayList<LineStrut> mArray = new ArrayList<LineStrut>();

	public static class LineStrut {
		private byte[] tempLine = null;
		private String msg = "";
		private int tagEndIndex;

		public int getTagEndIndex() {
			return tagEndIndex;
		}
		
		public String getMsg() {
			return msg;
		}

		public byte[] getLine() {
			return tempLine;
		}

		public LineStrut(byte[] tempLine) {
			this.tempLine = tempLine;
		}
	}

	private byte[] conn(byte[] old, byte[] now) {
		byte[] b= new byte[old.length+now.length];
		for(int i=0;i<old.length;i++){
			b[i]=old[i];
		}
		for(int j=0;j<now.length;j++){
			b[j]=now[j];
		}
		return b;
	}
	
	private int indexOf(byte b, byte[] bs, int offset) {
		for(int i=offset;i<bs.length;i++) {
			if(bs[i] == b) {
				return i;
			}
		}
		return -1;
	}
	
	private byte[] copy(byte[] d) {
		
		int count = 0;
		for(int i=0;i<5;i++) {
			if (d[i] == '\0') {
				count++;
			}
		}
		if (count >= 4) return null;
		
		byte[] b= new byte[d.length];
		for(int i=0;i<d.length;i++){
			b[i]=d[i];
		}
		return b;
	}
	
	private LineStrut gen(byte[] data, int offset, int tagIndex, int length) {//0 24 33
		byte[] b = new byte[length];
		for(int i=offset;i<length;i++){
			b[i]=data[i];
		}
		LineStrut line = new LineStrut(b);
		line.tagEndIndex = tagIndex;
		line.msg = genMessage(data, tagIndex+1);
		return line;
	}
	
	private String genMessage(byte[] data, int offset) {
		String msg = "";
		for (int i = offset;; i++) {
			if(data[i] != '\0') {
			    msg+=(char)data[i];
			} else {
				break;
			}
		}
		return msg;
	}
	
	public synchronized final void addOutput(byte[] data, int offset, int length) {
        if (isCancelled() == false) {
        	
        	byte[] currentData = data;
        	// ok we've got a package data
        	if (data != null && data.length > 0) {
        		
        		// if we had an unfinished line we add it.
        		if (mUnfinishedLine != null) {
        			currentData = conn(mUnfinishedLine, currentData);
                    mUnfinishedLine = null;
                }
        		
        		// now we split the lines
                mArray.clear();
                int start = 20;
                do {
                    int index = indexOf(SEP_EOF, currentData, start);//24

                    // if \0 was not found, this is an unfinished line
                    // and we store it to be processed for the next packet
                    if (index == -1) {
                        mUnfinishedLine = copy(currentData);
                        break;
                    }

                    //so we found a \0;
                    //try to find the second one
                    int index2 = indexOf(SEP_EOF, currentData, index + 1);//33

                    // if the second \0 was not found, this is an unfinished line
                    // and we store it to be processed for the next packet
                    if (index2 == -1) {
                        mUnfinishedLine = copy(currentData);
                        break;
                    }
                    
                    //maybe the first \0 and the second \0 at the same position
                    //we wait for the next packet
                    if(index2 <= index) {
                    	mUnfinishedLine = copy(currentData);
                        break;
                    }
                    
                    //check
                    if (index2 - index == 1) {
                    	mUnfinishedLine = null;
                    	break;
                    }
                    
                    // so we found the second \0;
                    // extract the line
                    LineStrut lineOver = gen(currentData, start-20, index, index2);
                    mArray.add(lineOver);

                    // move start to after the second \0 we found
                    start = index2 + 1 + 20;
                } while (true);

                if (mArray.size() > 0) {
                    // at this point we've split all the lines.
                    // make the array
                	LineStrut[] lines = mArray.toArray(new LineStrut[mArray.size()]);

                    // send it for final processing
                	System.out.println("MultiLineReceiver.addOutput() Will process new messages:"+lines.length);
                    processNewLines(lines);
                }
        	}
        }
    }

	public final void flush() {
		if (mUnfinishedLine != null || mUnfinishedLine.length > 0) {
			LineStrut[] line = new LineStrut[1];
			line[0] = new LineStrut(mUnfinishedLine);
			System.out.println("flush() Will process new messages:"+line.length+";"+this);
			processNewLines(line);
		}

		done();
	}

	public void done() {
	}

	public abstract void processNewLines(LineStrut[] line);
}
