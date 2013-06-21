package dbmgr;

import java.util.*;
import java.util.logging.*;
import java.io.*;
import java.net.*;

import util.Setting;
import util.MyLogger;

/* JDBC connection is reusable */
public abstract class AppServer {

    private String       name;
    private boolean      isAuditer;   

    Logger LOG2PC = null; 

    DBWorker dbwk;
    int      svrPort  = 65536;

    /* if vote YES, but get no decision from coordinator
     * need start termination protocol. */
    boolean startTerminator = false;

    public AppServer (String n) {
	this(n, false);
    }

    public AppServer (String n, boolean is) {
	this.name = n;
	this.isAuditer = is;
    }

    /* initialize the listening socket and 
     * DBMS connection */
    public abstract void start();

    public String toString() {
	String str = name;
	/* if (isAuditer) str += ", as auditer, "; 
	 * else str += ", "; */

	return str;
    }

    /* implement the termination protocol */
    class Terminator extends Thread {
	public void run() {
	}
    }

    /* 
     * DBMFrontend receives transaction requests, 
     * and submit transactions to the DBMS
     * */
    class DBMFrontend extends Thread {

	protected Socket clientSocket;
	PrintWriter out = null;
	BufferedReader in = null;

	private DBMFrontend (Socket clientSoc) {
	    clientSocket = clientSoc;

	    start();
	}

	/* GET decision from TM, 
	 * but might timeout and re-ordering */
	public boolean asDecided(List<String> plist) throws IOException {
	    String inputLine, r_tid1, r_tid2;
	    String[] tokens = null;

	    try {
		inputLine = in.readLine();

		if (inputLine == null) { /* peer closed */
		    System.err.println ("[peer closed] should get decision from TM");
		    return false;
		}

		/* System.out.println (inputLine); */
		tokens = inputLine.split(Setting.theSplit);

		if (tokens == null || tokens.length != 2) 
		    return false;

		r_tid1 = plist.get(0);
		r_tid2 = tokens[0];

		System.out.println("former tid " + r_tid1 
			+ " current tid " + r_tid2);

		/* check tid to avoid re-ordering */
		if(! r_tid1.trim().equals(r_tid2.trim()))
		    return false;

		if (tokens[1].equals(Setting.COMMIT)) {
		    LOG2PC.info("COMMIT: " + plist);
		    dbwk.commit();

		} else if (tokens[1].equals(Setting.ABORT)) {
		    LOG2PC.info("ABORT: " + plist);
		    dbwk.abort();

		} else {
		    System.err.println ("unknown decision from TM");
		    return false;
		}

	    } catch (SocketTimeoutException e) {
		System.err.println ("socket time out");
		return false;
	    }

	    return true;
	}

	public void run() {
	    long threadID = Thread.currentThread().getId();

	    System.out.println ("New DB Service Thread[" + threadID + "] Start");

	    try { 
		out = new PrintWriter(clientSocket.getOutputStream(), true); 
		in = new BufferedReader(new 
			InputStreamReader(clientSocket.getInputStream())); 

		String inputLine;  
		String[] tokens = null;

		while (true) {
		    /* GET transaction parameters */
		    if ((inputLine = in.readLine()) != null) {
			/* System.out.println (inputLine); */

			/* get the participting appsvrs */
			tokens = inputLine.split(Setting.subSplit);
			ArrayList<String> players = new 
			    ArrayList<String>(Arrays.asList(tokens));
			/* removing the parameters */
			players.remove(0);
			
			/* process the parameters for transaction */
			tokens = inputLine.split(Setting.theSplit);
			/* construct sql and submit to DBMS 
			 * token list: tid, optype, param list */
			int ret = dbwk.prep(tokens);

			ArrayList<String> plist = new 
			    ArrayList<String>(Arrays.asList(tokens));
			/* removing the participating appsvrs */
			plist.remove(plist.size() - 1);

			/* RSP result */
			String rsp = tokens[0] + Setting.theSplit;
			boolean wait4dec = false;
			if (ret == Setting.YES) {
			    LOG2PC.info("YES: " + plist + ": " + players);

			    rsp += Setting.SEND_YES;
			    out.println(rsp); 
			    wait4dec = true;

			} else if (ret == Setting.NO) {
			    LOG2PC.info("ABORT: " + plist);

			    rsp += Setting.SEND_NO;
			    out.println(rsp);
			    dbwk.abort();

			} else {
			    System.err.println ("unknown, should be Y/N");
			    break;
			}

			if (wait4dec) {
			    clientSocket.setSoTimeout(Setting.TIMEOUT); 

			    if (! asDecided(plist)) {
				/* need Termination Protocol to 
				 * decide whether to abort or not */

				System.err.println("start termination protocol");
				startTerminator = true;

				break;
			    }

			    clientSocket.setSoTimeout(0); 
			}

		    } else {
			System.err.println ("[peer closed] should get tran from TM");
			break; /* if inputLine is null, peer is closed */
		    }
		}

	    } catch (IOException e) { 
		System.err.println("Problem with Communication");
		/* just stop this thread; System.exit(1); */
	    } 

	    close();
	    System.out.println ("DB Service Thread[" + threadID + "] Stop");
	}

	private void close() {
	    try {
		out.close();
		in.close();
		clientSocket.close(); 

		out = null;
		in  = null;
		clientSocket = null;

	    } catch (Exception e) {
		e.printStackTrace();
	    }
	}
    }

    /* 
     * stay listening to client's connection requests 
     */
    class Listener extends Thread {
	public void run() {
	    ServerSocket serverSocket = null; 

	    try {
		serverSocket = new ServerSocket(svrPort); 

		try { 
		    while (true) {
			System.out.println ("Listening at " + svrPort);

			new DBMFrontend (serverSocket.accept());
		    }
		} catch (IOException e) { 
		    System.err.println("Accept failed."); 
		    System.exit(1); 
		} 

	    } catch (IOException e) { 
		System.err.println("Could not listen on port: 10008."); 
		System.exit(1); 
	    } finally {
		try {
		    serverSocket.close(); 

		} catch (IOException e) { 
		    System.err.println("Could not close port: 10008."); 
		    System.exit(1); 
		} 
	    }
	}
    }

}
