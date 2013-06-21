package server;

import java.util.*;

public abstract class Operation {

    String       opType;
    int          paramCnt;

    public Operation(String op, int pcnt) {
	this.opType = op;
	this.paramCnt = pcnt;
    }

    public int getParamCnt() {
	return this.paramCnt;
    }

    public String getOpType() {
	return this.opType;
    }

    public String toString() {
	String str = (opType + " needs " + 
		paramCnt + " parameters");

	return str;
    }

    /* construct the sql-like operation and map it to specific 
     * AppServer client,
     * auditer is the AppServer responsible to record 
     * each transaction*/
    public abstract DTran createDTran(List<String> params,
	    Map<String, AppSvrClt> appsvrs, String auditer, long tID);
}
