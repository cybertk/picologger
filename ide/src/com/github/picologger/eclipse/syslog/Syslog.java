package com.github.picologger.eclipse.syslog;

public class Syslog {

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
}
