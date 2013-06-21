package server;

import java.net.*; 
import java.io.*; 
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.*;

import util.Setting;
import util.MyLogger;

public class TManager { 

    private TCoordinator   tc = null;
    private SequenceNumber sn = null;

    Logger LOG2PC = null; 

    /* Main thread do the listening stuff */
    public static void main(String[] args) throws IOException { 
	new TManager().start();
    }

    public void start() {
	LOG2PC = MyLogger.getInstance(TManager.class.getName());

	/* start the listener, which will create a TMFrontend 
	 * for each client */
	new Listener().start();

	/* start TCoordinator, which use AppSvrClt to talk with 
	 * AppServers to complete distributed transactions */
	tc = new TCoordinator(); 
	sn = new SequenceNumber();
    }

    /* 
     * the backend for transaction process (2PC is implemented here) 
     * init the distributed transaction and commit by 2PC
     * */
    class TCoordinator extends Thread {
	Queue<DTran>            trans;
	Map<String, AppSvrClt>  appsvrs;

	/* using strategy pattern to support many kinds 
	 * of transcation operations like Transfer */
	Map<String, Operation>  ops;

	private TCoordinator() {
	    trans   = new ConcurrentLinkedQueue<DTran>();

	    appsvrs = new ConcurrentHashMap<String, AppSvrClt>(3);
	    ops     = new HashMap<String, Operation>(1);

	    /* AppSvrClts talks with AppServers */
	    appsvrs.put(Setting.BOC_NAME, 
		    AppSvrCltFactory.create(Setting.BOC_NAME));
	    appsvrs.put(Setting.CCB_NAME,
		    AppSvrCltFactory.create(Setting.CCB_NAME));
	    appsvrs.put(Setting.CBRC_NAME, 
		    AppSvrCltFactory.create(Setting.CBRC_NAME));

	    /* operations supported */
	    ops.put(Setting.TRANSFER, 
		    OPFactory.create(Setting.TRANSFER));

	    new ConnWatcher(appsvrs);

	    start();
	}

	/* as a simple example, here we only handle the 
	 * specific transaction */
	public DTran regDTran(String params[]) {

	    if (params.length < 1) return null;

	    Operation op = ops.get(params[0]);
	    DTran dt;

	    if (op != null) {
		System.out.println("create dist-tran for\n" 
			+ op);

		List<String> ps = new ArrayList<String>(
			Arrays.asList(params));
		ps.remove(0);              

		long tID = sn.peek();
		dt = op.createDTran(ps, appsvrs, Setting.CBRC_NAME, tID);
		if (dt == null) { return null; }

		/* increase the sequence number */
		sn.next();

	    } else {
		System.err.println("Unsupported operation " + params[0]);
		return null;
	    }

	    /* queue is synchronized */
	    System.out.println("----------enqueue----------");
	    trans.add(dt);

	    return dt;
	}

	public void run() {

	    /* get next transaction and execute it */
	    for (;;) {
		/* queue is synchronized */
		if (! trans.isEmpty()) {

		    System.out.println("----------dequeue----------");
		    DTran dt = trans.poll();

		    execBy2PC(dt);

		    /* return to client the results */
		    synchronized (dt) { dt.notify(); }

		} else {
		    try{ Thread.sleep(Setting.WAITING_TIME); } 
		    catch(Exception e) { e.printStackTrace(); }
		}
	    }
	}

	/* the 2PC protocol is implemented here */
	private void execBy2PC(DTran dt) {
	    System.out.println("START-2PC:\n" + dt);

	    /* issue each subtran to specific AppServer
	     * by using the AppSvrClt */
	    int workerNum = Setting.SVR_NUM;
	    VotingResult vr = new VotingResult(workerNum);

	    /* SEND params and RECV voting result */
	    dt.submit(vr);
	    LOG2PC.info("START-2PC " + dt.getID() + "\n" + dt);

	    try {
		/* if voting count is smaller than workerNum 
		 * this thread will wait here */
		boolean vret = vr.toCommit();
		System.out.println("----------voting----------");
		System.out.println(vr);

		/* SEND voting decision and RECV ack */
		if (vret) {
		    LOG2PC.info("COMMIT " + dt.getID() + "\n" + dt);
		    dt.commit(vr);
		    dt.addRes("committed");

		} else {
		    LOG2PC.info("ABORT " + dt.getID() + "\n" + dt);
		    dt.abort(vr);
		    dt.addRes("aborted");

		    /* if there is server down */
		    for (AppSvrClt as: vr.no) {
			String reason = vr.reason.get(as);

			/* TIMEOUT can be handled differently from 
			 * SVR_DOWN, but here treat them equally to 
			 * keep it simple */
			if (reason.equals(Setting.REASON_SVR_DOWN) 
				|| reason.equals(Setting.REASON_TIMEOUT)) 
			{ 
			    /* client socket is closed already */
			    System.out.println("TM lost connect with " 
				    + as.getSvrName());

			    appsvrs.get(as.getSvrName()).beDummy(); 

			    /* then, 
			     * 1) createDTran() will fail, so the following 
			     * tran will not be executed;
			     *
			     * 2) connection can be rebuilt in another thread. 
			     * (a map from appsvr name to its ip:port is needed, or 
			     * use reflection to find the subclass of AppSvrClt. 
			     * */
			} 
		    }

		}

	    } catch(Exception e) { 
		e.printStackTrace(); 
	    }
	}

	/* thread responsible to reconnect */
	class ConnWatcher extends Thread {
	    Map<String, AppSvrClt>  watchee;

	    public ConnWatcher(Map<String, AppSvrClt>  ass) {
		this.watchee = ass;

		start();
	    }

	    public void run() {
		for (;;) {
		    for (Map.Entry<String, AppSvrClt> entry : 
			    watchee.entrySet()) 
		    {
			AppSvrClt as = entry.getValue();
			String asName = entry.getKey();

			if (as.isDummy()) {
			    /* reconnect but wait a longer time to let
			     * packets die*/
			    try{ Thread.sleep(Setting.WAITING_LONGTIME); } 
			    catch(Exception e) { e.printStackTrace(); }

			    watchee.put(asName, 
				    AppSvrCltFactory.create(asName));
			}
		    }

		    /* avoid consuming too much computing resource */
		    try{ Thread.sleep(Setting.WAITING_TIME); } 
		    catch(Exception e) { e.printStackTrace(); }
		}
	    }
	}
    }

    /* 
     * TMFrontend receives transaction requests, registers the 
     * request to TCoordinator and wait to return the result 
     * */
    class TMFrontend extends Thread {

	protected Socket clientSocket;

	PrintWriter out = null;
	BufferedReader in = null;

	private TMFrontend(Socket clientSoc) {
	    clientSocket = clientSoc;
	    start();
	}

	public void run() {
	    long threadID = Thread.currentThread().getId();

	    System.out.println ("New TM Service Thread[" + threadID + "] Start");

	    try { 
		out = new PrintWriter(clientSocket.getOutputStream(), 
			true); 
		in = new BufferedReader(new 
			InputStreamReader(clientSocket.getInputStream())); 

		String inputLine; 

		/* get transaction parameters */
		if ((inputLine = in.readLine()) != null) {
		    /* System.out.println (inputLine); */

		    String[] tokens = inputLine.split(Setting.theSplit);

		    /* register transaction, wait for result */
		    DTran dt = tc.regDTran(tokens);

		    String ret = ""; 

		    if (dt != null) {
			/* wait for operation result */
			synchronized (dt) { dt.wait(); } 
			ret = dt.getRes().toString();

		    } else {

			System.out.println("[syserr] fail to " 
				+ "create transaction");
			ret = "[aborted] syserr, try later";
		    }

		    /* return result */
		    out.println(ret);

		} else {
		    System.out.println ("get nothing from client");
		}

		out.close(); 
		in.close(); 

		clientSocket.close(); 
	    } catch (IOException e) { 
		System.err.println("Problem with Communication");
		/* just stop this thread; System.exit(1); */

	    } catch (InterruptedException ee) { 
		System.err.println("dt.wait() is interrupted");
	    }

	    System.out.println ("Service Thread[" +
		    Thread.currentThread().getId() + "] Stop");
	}
    }

    /* 
     * stay listening to client's connection requests 
     */
    class Listener extends Thread {
	public void run() {
	    ServerSocket serverSocket = null; 
	    int svrPort = Setting.TM_PORT;

	    try {
		serverSocket = new ServerSocket(svrPort); 

		try { 
		    for (;;) {
			System.out.println ("Listening...");

			new TMFrontend (serverSocket.accept());
		    }
		} catch (IOException e) { 
		    System.err.println("Accept failed."); 
		    System.exit(1); 
		} 

	    } catch (IOException e) { 
		System.err.println("Could not listen on port: " + svrPort + "."); 
		System.exit(1); 
	    } finally {
		try {
		    serverSocket.close(); 

		} catch (IOException e) { 
		    System.err.println("Could not close port: " + svrPort + "."); 
		    System.exit(1); 
		} 
	    }
	}
    }

    /* generate sequence number to tag each Transaction 
     * TODO
     * logs should be analyzed when TManager is restarted 
     * so that the sequencenumber will not WRAP AROUND */
    class SequenceNumber { 
	public static final long MIN_VALUE     = 0;
	public static final long DEFAULT_VALUE = 1;
	public static final long MAX_VALUE     = Long.MAX_VALUE;

	private long value;

	public SequenceNumber() {
	    this.value = DEFAULT_VALUE;
	}

	public SequenceNumber(long initialValue) {
	    this.value = initialValue;
	}

	synchronized public long next() {
	    long nextValue = this.value;
	    if (this.value == MAX_VALUE) {
		this.value = DEFAULT_VALUE;
	    } else {
		this.value++;
	    }

	    return nextValue;
	}

	synchronized public long peek() {
	    return this.value;
	}

	synchronized public void reset() {
	    this.value = DEFAULT_VALUE;
	}
    }
} 
