package server;

import java.util.*;

import util.Setting;

public class DTran {
    long tranID;

    /* each AppServer get a specific SQL to execute */
    Map<AppSvrClt, String> subTrans;
    List<String> ret;

    public DTran() {
	subTrans = new HashMap<AppSvrClt, String>();
	ret = new ArrayList<String>();
    }

    public void addSubtran(AppSvrClt as, String sub) {
	subTrans.put(as, sub);
    }

    public long getID() {
	return tranID;
    }

    public void setID(long id) {
	this.tranID = id;
    }

    public void commit(VotingResult vr) {
	System.out.println("----------commit----------");

	String cmt = tranID + Setting.theSplit + Setting.COMMIT;

	for (AppSvrClt as: vr.yes) {
	    as.issue(cmt, null);
	}
    }

    public void abort(VotingResult vr) {
	System.out.println("----------abort----------");

	String abt = tranID + Setting.theSplit + Setting.ABORT;

	for (AppSvrClt as: vr.yes) {
	    as.issue(abt, null);
	}
    }

    public void submit(VotingResult vr) {
	System.out.println("----------submit----------");

	for (Map.Entry<AppSvrClt, String> entry : subTrans.entrySet()) {
	    AppSvrClt as = entry.getKey();
	    String sub = entry.getValue();

	    as.issue(sub, vr);
	}
    }

    public void addRes(String res) {
	ret.add(res);
    }

    public List<String> getRes() {
	return ret;
    }

    public void dump() {
	System.out.println("subtrans to exec:\n" + this);

	if (ret.size() > 0) {
	    System.out.println("results:");
	    for (String s : ret)
		System.out.println(s);
	} else 
	    System.out.println("NO results:");
    }

    public String toString() {
	String str = "";

	String[] tokens = null;
	ArrayList<String> plist = null;

	for (Map.Entry<AppSvrClt, String> entry : 
		subTrans.entrySet()) {
	    AppSvrClt as = entry.getKey();
	    String sub = entry.getValue();

	    tokens = sub.split(Setting.theSplit); 
	    plist = new ArrayList<String>(Arrays.asList(tokens));

	    str += (as + "\tDO ");
	    str += (plist + "\n"); 
	}

	return str;
    }
}
