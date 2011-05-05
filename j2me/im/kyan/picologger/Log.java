/*
 * Copyright (C) 2006 The Android Open Source Project
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

package im.kyan.picologger;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.microedition.io.Datagram;
import javax.microedition.io.DatagramConnection;

import net.rim.device.api.system.CoverageInfo;
import net.rim.device.api.system.RadioInfo;

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
public abstract class Log {

	/**
	 * Priority constant for the println method; use Log.v.
	 */
	public static final int VERBOSE = 2;

	/**
	 * Priority constant for the println method; use Log.d.
	 */
	public static final int DEBUG = 3;

	/**
	 * Priority constant for the println method; use Log.i.
	 */
	public static final int INFO = 4;

	/**
	 * Priority constant for the println method; use Log.w.
	 */
	public static final int WARN = 5;

	/**
	 * Priority constant for the println method; use Log.e.
	 */
	public static final int ERROR = 6;

	/**
	 * Priority constant for the println method.
	 */
	public static final int ASSERT = 7;

	private Log() {

	}

	/**
	 * Send a {@link #VERBOSE} log message.
	 * 
	 * @param tag
	 *            Used to identify the source of a log message. It usually
	 *            identifies the class or activity where the log call occurs.
	 * @param msg
	 *            The message you would like logged.
	 */
	public static int v(String tag, String msg) {
		return println_native(LOG_ID_MAIN, VERBOSE, tag, msg);
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
	public static int d(String tag, String msg) {
		return println_native(LOG_ID_MAIN, DEBUG, tag, msg);
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
	public static int i(String tag, String msg) {
		return println_native(LOG_ID_MAIN, INFO, tag, msg);
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
	public static int w(String tag, String msg) {
		return println_native(LOG_ID_MAIN, WARN, tag, msg);
	}

	/**
	 * Checks to see whether or not a log for the specified tag is loggable at
	 * the specified level.
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
	public static int e(String tag, String msg) {
		return println_native(LOG_ID_MAIN, ERROR, tag, msg);
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
	public static int println(int priority, String tag, String msg) {
		return println_native(LOG_ID_MAIN, priority, tag, msg);
	}

	/** @hide */
	public static final int LOG_ID_MAIN = 0;
	/** @hide */
	public static final int LOG_ID_RADIO = 1;
	/** @hide */
	public static final int LOG_ID_EVENTS = 2;
	/** @hide */
	public static final int LOG_ID_SYSTEM = 3;

	private static Buffer sBuffer;

	private static LogWritter sWritter;

	static {
		// 100k buffer
		sBuffer = new Buffer(102400);

		sWritter = new LogWritter(sBuffer);
		sWritter.start();
	}

	public static void flush() {

		synchronized (sBuffer) {
			sBuffer.notify();
		}
	}

	private static int println_native(int bufID, int priority,
			String tag, String msg) {

		// @see writeString()
		final int tagSize = tag.length() + 1;
		final int msgSize = msg.length() + 1;

		long now = System.currentTimeMillis();

		synchronized (sBuffer) {

			// determine whether sending buffers out
			if ((tagSize + msgSize + 16) > sBuffer.getAvailableSize()) {
				flush();
				
				//TODO: 
				return -1;
			}
			
			// fill prefix
			// tv_secs
			sBuffer.writeInt((int) (now / 1000));
			// tv_usecs
			sBuffer.writeInt((int) ((now % 1000) * 1000));

			sBuffer.writeInt(priority);
			// payload length, +2 because write string will append \0 at the end
			// of
			// string
			sBuffer.writeInt(tagSize + msgSize);

			sBuffer.writeString(tag);
			sBuffer.writeString(msg);
		}

		// send packet out if the packet is urgent
		if (priority > WARN) {
			flush();
		}

		return 0;
	}

	static class Buffer {

		// should protected by mutex
		private byte[] mBuffers;
		private int mBufferTail;

		public Buffer(int sz) {
			mBuffers = new byte[sz];
			reset();
		}

		/**
		 * clear the buffer
		 */
		synchronized public void reset() {
			mBufferTail = 0;
		}

		/**
		 * get bytes array
		 * 
		 * @return
		 */
		synchronized public byte[] getBytes() {
			return mBuffers;
		}

		/**
		 * get the current buffer size
		 * 
		 * @return
		 */
		synchronized public int length() {
			return mBufferTail;
		}

		/**
		 * get the available size of buffer
		 * 
		 * @return
		 */
		synchronized public int getAvailableSize() {
			return mBuffers.length - mBufferTail;
		}

		synchronized public boolean isEmpty() {
			return mBufferTail == 0;
		}

		/**
		 * write integer to buffer, save as big-endian
		 * 
		 * @param i
		 */
		private void writeInt(int i) {

			// TODO: optimize
			mBuffers[mBufferTail] = (byte) (i >> 24);
			mBuffers[mBufferTail + 1] = (byte) (i >> 16);
			mBuffers[mBufferTail + 2] = (byte) (i >> 8);
			mBuffers[mBufferTail + 3] = (byte) (i);

			mBufferTail += 4;
		}

		/**
		 * write String to buffer, will append \0 at the end
		 * 
		 * @param s
		 */
		private void writeString(String s) {

			final int sz = s.length();

			System.arraycopy(s.getBytes(), 0, mBuffers, mBufferTail, sz);
			mBufferTail += sz;

			mBuffers[mBufferTail] = 0;
			mBufferTail += 1;
		}
	}

	static class LogWritter extends Thread {

		private String logdUri = "datagram://10.60.5.62:20504";

		final private Buffer mBuffers;

		private DatagramConnection mConnection;

		public LogWritter(Buffer buf) {
			super(".LogerWriter");

			mBuffers = buf;

			if ((CoverageInfo.getCoverageStatus(RadioInfo.WAF_WLAN, true) & CoverageInfo.COVERAGE_DIRECT) == CoverageInfo.COVERAGE_DIRECT) {
				logdUri += "/ ;interface=wifi";
			}
			try {
				mConnection = (DatagramConnection) Connector.open(logdUri);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void run() {

			for (;;) {

				synchronized (mBuffers) {
					try {
						mBuffers.wait(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

					if (!mBuffers.isEmpty()) {

						push();
						// reset buffer
						mBuffers.reset();
					}
				}
			}
		}

		private void push() {

			Datagram dg;
			try {
				dg = mConnection.newDatagram(mBuffers.getBytes(),
						mBuffers.length(), logdUri);
				mConnection.send(dg);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
