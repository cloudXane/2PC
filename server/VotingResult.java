package server;

import java.util.*;

public class VotingResult {
    List<AppSvrClt> yes;
    List<AppSvrClt> no;
    Map<AppSvrClt, String> reason;

    int workerNum;

    public VotingResult (int wn) {
	this.workerNum = wn;

	yes = new ArrayList<AppSvrClt>(wn);
	no = new ArrayList<AppSvrClt>(wn);

	/* no need for a ConcurrentHashMap */
	reason = new HashMap<AppSvrClt, String>(wn);
    }

    /* Give a reason when voting no */
    public synchronized void addNo(AppSvrClt as, String rsn) 
	throws InterruptedException {

	no.add(as);
	reason.put(as, rsn);

	if (no.size() + yes.size() == workerNum)
	    notify();
    }

    public synchronized void addNo(AppSvrClt as) 
	throws InterruptedException {
	no.add(as);

	if (no.size() + yes.size() == workerNum)
	    notify();
    }

    public synchronized void addYes(AppSvrClt as) 
	throws InterruptedException {
	yes.add(as);

	if (no.size() + yes.size() == workerNum)
	    notify();
    }

    public synchronized boolean toCommit() 
	throws InterruptedException {
	if (no.size() + yes.size() < workerNum)
	    wait();

	return no.size() == 0;
    }

    public String toString() {
	String str = "";

	if (no.size() + yes.size() == workerNum)
	    str += "voting is done\n";
	else
	    str += "voting continues\n";

	str += "result is: " 
	    + yes.size() + " yes;" 
	    + no.size() + " no.\n";

	/* if (no.size() == 0) str += "to commit\n";
	 * else str += "to abort\n"; */

	return str;
    }
}
