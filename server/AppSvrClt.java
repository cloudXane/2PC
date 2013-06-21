package server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import util.Setting;

/* connect with AppServer, submit to it the 
 * transaction, and wait for result */
public class AppSvrClt extends Thread {
    private String       name;
    private boolean      isAuditer;   

    /* only contain ONE element to serialized the 
     * distributed transactions; overlapping the 
     * subtrans of the multiple distributed transactions 
     * has even more complicated concurrency issue */
    Queue<String>        subtran;

    /* subclass have to give these two 
     * variables valid values */
    String svrHost  = null;
    int    svrPort  = 65536;

    Socket sock     = null;
    PrintWriter out = null;
    BufferedReader in = null;

    VotingResult vr = null;

    public AppSvrClt (String n) {
	this(n, false);
    }

    public AppSvrClt (String n, boolean is) {
	this.name = n;
	this.isAuditer = is;
	subtran = new ConcurrentLinkedQueue<String>();

	/* delay to subclass for a valid port number 
	 * connect(); 
	 * start(); */
    }

    public String getSvrName() { 
	return name; 
    }

    public synchronized void beDummy() {
	name = Setting.DUMMY; 
    }

    public synchronized boolean isDummy() {
	return name.equals(Setting.DUMMY);
    }

    public String toString() {
	String str = name;
	/* if (isAuditer) str += ", as auditer, ";
	   else str += ", "; */

	str += ("(" + svrHost + ":" + svrPort + ")");

	return str;
    }

    public void run() {
	long threadID = Thread.currentThread().getId();
	System.out.println ("New " + name + " AppSvrClt Thread[" 
		+ threadID + "] Start");

	connect();

	String cmt = Setting.theSplit + Setting.COMMIT;
	String abt = Setting.theSplit + Setting.ABORT;

	try {
	    while(true) {
		if (! subtran.isEmpty()) {

		    String t = subtran.poll();
		    /* System.out.println(name + " submits\t" + t); */

		    if (t.indexOf(cmt) > 0 || t.indexOf(abt) > 0)
			commit(t);
		    else 
			prep(t);

		} else 
		    Thread.sleep(Setting.WAITING_TIME);  
	    } 
	} catch(Exception e) {
	    e.printStackTrace(); 
	}

	close();
	System.out.println (name + " AppSvrClt Thread[" + threadID 
		+ "] Stop");
    }

    private void commit(String t) throws Exception {
	try {
	    /* SEND CMT or ABT */
	    out.println(t);

	    /* GET ACK 
	     * String ret = in.readLine(); 
	     * System.out.println(name + " gets ACK"); */
	} catch (Exception e) {
	    System.err.println(name + "rmt svr down"); 

	    /* If AppServer votes YES, it will time out when 
	     * waiting for decision. Then Termination Protocol 
	     * will handle the following. */

	    /* to close this client thread to require 
	     * reconnect explictedly */
	    throw e;
	}
    }

    private void prep(String t) throws Exception {
	try {
	    /* SEND sub-tran param */
	    out.println(t);

	    /* check tID to avoid re-ordering */
	    String[] tokens = t.split(Setting.theSplit);
	    String s_tid = tokens[0];

	    /* GET voting result */
	    String vret = in.readLine();

	    /* peer socket is closed */
	    if (vret == null)
		throw new IOException(name + " rmt svr down");

	    tokens = vret.split(Setting.theSplit);

	    /* treat as peer socket is closed */
	    if (tokens == null || tokens.length != 2)
		throw new IOException(name + " unknown voting");

	    String r_tid = tokens[0];
	    System.out.println("snd tid " + s_tid 
		    + " rcv tid " + r_tid);

	    /* treat as peer socket is closed */
	    if (! s_tid.trim().equals(r_tid.trim())) {
		throw new IOException(name + " packets re-ordered");
	    }

	    /* update the voting result */
	    if (tokens[1].equals(Setting.SEND_YES))
		vr.addYes(this);
	    else 
		vr.addNo(this, Setting.REASON_VOTE_NO);

	    vr = null;

	} catch (SocketTimeoutException e) {
	    vr.addNo(this, Setting.REASON_TIMEOUT);

	    /* to close this client thread to require 
	     * reconnect explictedly */
	    throw new Exception("socket time out");

	} catch (IOException e) {
	    vr.addNo(this, Setting.REASON_SVR_DOWN);

	    /* to close this client thread to require 
	     * reconnect explictedly */
	    throw e;
	}
    }

    private void connect() {
	System.out.println(name + " connects to " + svrHost 
		+ ":" + svrPort);
	try {
	    sock = new Socket(svrHost, svrPort);

	    sock.setSoTimeout(Setting.TIMEOUT);

	    out = new PrintWriter(sock.getOutputStream(), true);
	    in = new BufferedReader(new InputStreamReader(
			sock.getInputStream()));

	} catch (UnknownHostException e) {
	    System.err.println("Don't know about host: " + svrHost);
	    /* System.exit(1); */

	    /* if cannot connect, make it as dummy, 
	     * ConnWatcher thread will retry */
	    System.err.println("Bad luck, " + name + " becomes dummy");
	    name = Setting.DUMMY;

	} catch (IOException e) {
	    System.err.println("Couldn't get I/O for "
		    + "the connection to: " + svrHost);
	    /* System.exit(1); */

	    System.err.println("Bad luck, " + name + " becomes dummy");
	    name = Setting.DUMMY;
	}

    }

    public void close() {
	try {
	    out.close();
	    in.close();
	    sock.close();

	    out = null;
	    in = null;
	    sock = null;

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

    public void issue(String t, VotingResult vr) {
	if(subtran.isEmpty()) {
	    subtran.add(t);

	    this.vr = vr;
	} else
	    System.err.println("Not allow to overlap multiple" + 
		    "distributed transactions");
    }
}
