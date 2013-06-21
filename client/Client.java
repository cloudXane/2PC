package client;

import java.io.*;
import java.net.*;
import util.Setting;

public class Client {

    public static void main(String[] args) throws IOException {

	String userName = null;
	int    amount   = 0;
	String fromBK   = null;
	String toBK     = null;

	String svrHost  = Setting.TM_HOST;
	int    svrPort  = Setting.TM_PORT;

	/* cli john 1000 boc ccb 127.0.0.1 8888 */
	if (args.length < 6) {
	    System.out.println("Use it giving params like:");
	    System.out.println("\tjohn 1000 boc ccb 127.0.0.1 8888");

	    System.exit(1);
	}

	userName = args[0];
	amount = Integer.parseInt(args[1]);

	fromBK = args[2];
	toBK   = args[3];

	svrHost = args[4];
	svrPort = Integer.parseInt(args[5]);

	System.out.println("user " + userName + " " 
		+ "transfer " + amount + " " 
		+ "from " + fromBK + " " 
		+ "to " + toBK + "\n" 
		+ "connecting to server " + svrHost + ":"
		+ svrPort);

	Socket sock = null;
	PrintWriter out = null;
	BufferedReader in = null;

	try {
	    sock = new Socket(svrHost, svrPort);

	    sock.setSoTimeout(Setting.TIMEOUT * 2);

	    out = new PrintWriter(sock.getOutputStream(), true);
	    in = new BufferedReader(new InputStreamReader(
			sock.getInputStream()));

	} catch (UnknownHostException e) {
	    System.err.println("Don't know about host: " + svrHost);
	    System.exit(1);

	} catch (IOException e) {
	    System.err.println("Couldn't get I/O for "
		    + "the connection to: " + svrHost);
	    System.exit(1);
	}

	BufferedReader stdIn = new BufferedReader(
		new InputStreamReader(System.in));

	String opType = Setting.TRANSFER;
	String userInput = opType   + Setting.theSplit
			 + userName + Setting.theSplit 
	                 + amount   + Setting.theSplit
		         + fromBK   + Setting.theSplit
			 + toBK;

	out.println(userInput);
	System.out.println("result: " + in.readLine());

	/* System.out.println ("Type Message (\"Bye.\" to quit)");
	   while ((userInput = stdIn.readLine()) != null) {
	   out.println(userInput);
	   if (userInput.equals("Bye.")) break;
	   System.out.println("echo: " + in.readLine()); } */

	out.close();
	in.close();

	stdIn.close();
	sock.close();
    }
}
