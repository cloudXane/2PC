package server;

import java.util.*;
import util.Setting;

public class OPTransfer extends Operation {

    public static final String TYPE = Setting.TRANSFER;

    /* param should be like 
     * userName, amount, fromBK, toBK;
     * e.g. john 1000 boc ccb */
    public static final int    PCNT = 4;

    public OPTransfer() {
	super(TYPE, PCNT);
    }

    public DTran createDTran(List<String> params,
	    Map<String, AppSvrClt> appsvrs, String auditer, long tID) {

	DTran newDT = null;

	if (params.size() == paramCnt) {
	    newDT = new DTran();
	    newDT.setID(tID);

	    String uname  = params.get(0);
	    int amount    = Integer.parseInt(params.get(1));
	    String fromBK = params.get(2);
	    String toBK   = params.get(3);

	    String players = Setting.theSplit 
		+ Setting.subSplit + fromBK 
		+ Setting.subSplit + toBK 
		+ Setting.subSplit + auditer;

	    /* match sql with specific AppServer's Client */
	    String tran2from  = tID + Setting.theSplit 
		+ Setting.MOVE_OUT + Setting.theSplit
		+ uname + Setting.theSplit
		+ amount + players;
	    AppSvrClt fromAS = appsvrs.get(fromBK);

	    if(fromAS.isDummy()) return null;
	    newDT.addSubtran(fromAS, tran2from);

	    String tran2to   = tID + Setting.theSplit 
		+ Setting.MOVE_IN + Setting.theSplit
		+ uname + Setting.theSplit
		+ amount + players;
	    AppSvrClt toAS   = appsvrs.get(toBK);

	    if(toAS.isDummy()) return null;
	    newDT.addSubtran(toAS, tran2to);

	    String tran2audit = tID + Setting.theSplit 
		+ Setting.MOVE + Setting.theSplit
		+ uname  + Setting.theSplit
		+ amount + Setting.theSplit
		+ fromBK + Setting.theSplit
		+ toBK + players;
	    AppSvrClt auditAS = appsvrs.get(auditer);

	    if(auditAS.isDummy()) return null;
	    newDT.addSubtran(auditAS, tran2audit);

	} else {
	    System.err.println("Wrong parameters list for " + opType);
	}

	return newDT;
    }
}
