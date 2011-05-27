package com.github.picologger.syslog;

public class Demo {

    private static String record = "<165>1 2003-08-24T05:14:15.000003-07:00 192.0.2.1 myproc 8710 - - %% It's time to make the do-nuts.";

    private static String recordWithSd = "<165>1 2003-10-11T22:14:15.003Z mymachine.example.com evntslog - ID47 [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1111\"] [exampleSDID@32473 iut=\"3\" eventSource=\"Application\" eventID=\"1011\"] BOMAn application event log entry...";

    /**
     * @param args
     */
    public static void main(String[] args) {
	// TODO Auto-generated method stub

	Syslog l = new Syslog(recordWithSd);
	System.out.println(l);
    }
}
