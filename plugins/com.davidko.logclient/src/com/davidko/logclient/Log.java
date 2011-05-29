package com.davidko.logclient;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.channels.SocketChannel;


public final class Log {

	public enum LogLevel {
		VERBOSE(2, "verbose", 'V'),
		DEBUG(3, "debug", 'D'),
		INFO(4, "info", 'I'),
		WARN(5, "warn", 'W'),
		ERROR(6, "error", 'E'),
		ASSERT(7, "assert", 'A');

		private int mPriorityLevel;
		private String mStringValue;
		private char mPriorityLetter;

		LogLevel(int intPriority, String stringValue, char priorityChar) {
			mPriorityLevel = intPriority;
			mStringValue = stringValue;
			mPriorityLetter = priorityChar;
		}
		
		public static LogLevel getbyPriority(int level) {
			for (LogLevel mode : values()) {
				if (mode.mPriorityLevel == level) {
					return mode;
				}
			}
			return null;
		}

		public char getPriorityLetter() {
			return mPriorityLetter;
		}

		public int getPriority() {
			return mPriorityLevel;
		}

		public String getStringValue() {
			return mStringValue;
		}
	}

	public interface ILogOutput {
		public void printLog(LogLevel logLevel, String tag, String message);
		public void printAndPromptLog(LogLevel logLevel, String tag,
				String message);
	}

	private static LogLevel mLevel = LogClientPreferences.getLogLevel();

	private static ILogOutput sLogOutput;

	private static final char[] mSpaceLine = new char[72];

	static {
		int i = mSpaceLine.length - 1;
		while (i >= 0)
			mSpaceLine[i--] = ' ';
		
		mSpaceLine[0] = mSpaceLine[1] = mSpaceLine[2] = mSpaceLine[3] = '0';
		mSpaceLine[4] = '-';
	}

	static final class Config {
		static final boolean LOGV = true;
		static final boolean LOGD = true;
	};

	private Log() {
	}

	public static void v(String clientIP, String timestemp, String tag, String message) {
		println(clientIP, timestemp, LogLevel.VERBOSE, tag, message);
	}

	public static void d(String clientIP, String timestemp, String tag, String message) {
		println(clientIP, timestemp, LogLevel.DEBUG, tag, message);
	}

	public static void i(String clientIP, String timestemp, String tag, String message) {
		println(clientIP, timestemp, LogLevel.INFO, tag, message);
	}

	public static void w(String clientIP, String timestemp, String tag, String message) {
		println(clientIP, timestemp, LogLevel.WARN, tag, message);
	}

	public static void e(String clientIP, String timestemp, String tag, String message) {
		println(clientIP, timestemp, LogLevel.ERROR, tag, message);
	}

	public static void logAndDisplay(String clientIP, String timestemp, LogLevel logLevel, String tag,
			String message) {
		if (sLogOutput != null) {
			sLogOutput.printAndPromptLog(logLevel, tag, message);
		} else {
			println(clientIP, timestemp, logLevel, tag, message);
		}
	}

	public static void e(String clientIP, String timestemp, String tag, Throwable throwable) {
		if (throwable != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);

			throwable.printStackTrace(pw);
			println(clientIP, timestemp, LogLevel.ERROR, tag,
					throwable.getMessage() + '\n' + sw.toString());
		}
	}

	static void setLevel(LogLevel logLevel) {
		mLevel = logLevel;
	}

	public static void setLogOutput(ILogOutput logOutput) {
		sLogOutput = logOutput;
	}

	private static void println(String clientIP, String timestemp, LogLevel logLevel, String tag, String message) {
		if (logLevel.getPriority() >= mLevel.getPriority()) {
			if (sLogOutput != null) {
				sLogOutput.printLog(logLevel, tag, message);
			} else {
				printLog(clientIP, timestemp, logLevel, tag, message);
			}
		}
	}

	public static void printLog(String clientIP, String timestemp, LogLevel logLevel, String tag, String message) {
		System.out.print(getLogFormatString(clientIP, timestemp, logLevel, tag, message));
	}

	public static String getLogFormatString(String clientIP, String timestemp, LogLevel logLevel, String tag,
			String message) {
		return String.format("%s %s %c/%s: %s\n", clientIP, timestemp,
				logLevel.getPriorityLetter(), tag, message);
	}
}
