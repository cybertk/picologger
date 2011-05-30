package com.davidko.logclient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import com.davidko.logclient.exception.TimeoutException;

public class LogServer {

	private static final String DEFAULT_ENCODING = "ISO-8859-1";
	private static final int WAIT_TIME = 5;// ms

	private boolean isOnline;
	private InetSocketAddress sockAddr = null;
	private String mPreHost = null;
	private int mPrePort = -1;

	private class Response {
		public Response() {
			message = "";
		}

		public boolean okay;
		public String message;
	}

	public boolean isOnLine() {
		return isOnline;
	}

	private void checkConnect() {
		try {
			String mHost = LogClientPreferences.getsLogServerHost();
			int mPort = LogClientPreferences.getsLogServerPort();
			if (!mHost.equals(mPreHost) || mPort != mPrePort) {
				mPreHost = mHost;
				mPrePort = mPort;
				InetAddress sHostAddr = InetAddress.getByName(mPreHost);
				sockAddr = new InetSocketAddress(sHostAddr, mPrePort);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public LogServer() {
		isOnline = true;
	}

	public void closeConnection() {
		isOnline = false;
	}

	private byte[] formRequest(String req) {
		String resultStr = req.trim();
		byte[] result;
		try {
			result = resultStr.getBytes(DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace();
			return null;
		}
		return result;
	}

	private void write(SocketChannel chan, byte[] data, int length, int timeout)
			throws TimeoutException, IOException {
		ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length
				: data.length);
		int numWaits = 0;

		while (buf.position() != buf.limit()) {
			int count;

			count = chan.write(buf);
			if (count < 0) {
				Log.d("localhost", "", "LogServer", "write: channel EOF");
				throw new IOException("channel EOF");
			} else if (count == 0) {
				if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
					Log.d("localhost", "", "LogServer", "write: timeout");
					throw new TimeoutException();
				}
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
				}
				numWaits++;
			} else {
				numWaits = 0;
			}
		}
	}

	private void read(SocketChannel chan, byte[] data, int length, int timeout)
			throws TimeoutException, IOException {
		ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length
				: data.length);
		int numWaits = 0;

		while (buf.position() != buf.limit()) {
			int count;

			count = chan.read(buf);
			if (count < 0) {
				Log.d("localhost", "", "LogServer", "read: channel EOF");
				throw new IOException(buf.toString());
			} else if (count == 0) {
				if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
					Log.d("localhost", "", "LogServer", "read: timeout");
					throw new TimeoutException();
				}
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
				}
				numWaits++;
			} else {
				numWaits = 0;
			}
		}
	}

	private boolean isOkay(byte[] reply) {
		return reply[0] == (byte) 'O' && reply[1] == (byte) 'K'
				&& reply[2] == (byte) 'A' && reply[3] == (byte) 'Y';
	}

	private Response readResponse(SocketChannel chan) throws TimeoutException,
			IOException {
		boolean needFAILString = false;
		Response resp = new Response();
		byte[] reply = new byte[4];
		read(chan, reply, -1, LogClientPreferences.getsTimeOut());
		if (isOkay(reply)) {
			resp.okay = true;
		} else {
			resp.okay = false;
			needFAILString = true;
		}
		
		if(needFAILString) {
			byte[] b = new byte[10240];
			read(chan, b, -1, LogClientPreferences.getsTimeOut());
			resp.message = new String(b);
		}
		
		return resp;
	}

	public void executeShellCommand(String command, IShellOutputReceiver rcvr,
			int maxTimeToOutputResponse) throws TimeoutException, IOException {
		checkConnect();
		System.out.println("execute: " + command);
		SocketChannel chan = SocketChannel.open(sockAddr);
		try {
			chan.configureBlocking(false);

			byte[] request = formRequest(command);
			write(chan, request, -1, LogClientPreferences.getsTimeOut());

			Response resp = readResponse(chan);
            if (resp.okay == false) {
                throw new IOException(resp.message);
            }
			
			byte[] data = new byte[1024];
			ByteBuffer buf = ByteBuffer.wrap(data);
			int timeToResponseCount = 0;
			while (true) {
				int count;

				if (rcvr != null && rcvr.isCancelled()) {
					Log.v("localhost", "", "LogServer", "execute: cancelled");
					break;
				}

				count = chan.read(buf);
				if (count < 0) {
					rcvr.flush();
					Log.v("localhost", "", "LogServer", "execute '" + command
							+ ". EOF hit. Read: " + count);
					break;
				} else if (count == 0) {
					try {
						int wait = WAIT_TIME * 5;
						timeToResponseCount += wait;
						if (maxTimeToOutputResponse > 0
								&& timeToResponseCount > maxTimeToOutputResponse) {
							throw new TimeoutException();
						}
						Thread.sleep(wait);
					} catch (InterruptedException ie) {
					}
				} else {
					timeToResponseCount = 0;

					if (rcvr != null) {
						printMessage(buf);
						rcvr.addOutput(buf.array(), buf.arrayOffset(),
								buf.position());
					}
					buf.rewind();
				}
			}
		} finally {
			if (chan != null) {
				chan.close();
			}
			Log.v("localhost", "", "LogServer", "execute: returning");
		}
	}
	
	private void printMessage(ByteBuffer buf) {
		byte[] b = buf.array();
		int offset = buf.arrayOffset();
		int length = buf.position();
		System.out.print("LOGSERVER<<"+length+">>");
		for(int i=offset;i<length;i++){
			System.out.print(" "+b[i]);
		}
		System.out.println("\n");
	}
}
