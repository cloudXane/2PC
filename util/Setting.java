package util;


public class Setting {
    public static final String theSplit = "###";
    public static final String subSplit = "&&&";

    public static final int UNKNOWN_OPTYPE = -1;

    public static final int WAITING_TIME = 512;

    /* if re-ordering happens, sleep longer until 
     * packets die on the net */
    public static final int WAITING_LONGTIME = 4096;

    public static final String DB_NAME = "demo2pc";
    public static final String DB_USER = "xbli";
    public static final String DB_PASS = "xbli";

    /*
    public static final String TM_HOST = "10.1.2.31";
    public static final String BOC_HOST  = "10.1.2.32";
    public static final String CCB_HOST  = "10.1.2.33";
    public static final String CBRC_HOST = "10.1.2.34";
    */

    public static final String TM_HOST = "127.0.0.1";
    public static final String BOC_HOST  = "127.0.0.1"; 
    public static final String CCB_HOST  = "127.0.0.1"; 
    public static final String CBRC_HOST = "127.0.0.1"; 

    public static final int TM_PORT    = 8888;
    public static final int BOC_PORT  = 8881;
    public static final int CCB_PORT  = 8882;
    public static final int CBRC_PORT = 8883;

    public static final int SVR_NUM = 3;
    public static final String BOC_NAME  = "boc";
    public static final String CCB_NAME  = "ccb";
    public static final String CBRC_NAME = "cbrc";
    public static final String DUMMY = "dummy";

    /* operations from client to TM */
    public static final String TRANSFER = "TRANSFER";

    /* time to wait for svr's rsp: used at 
     * Client-TM and AppSvrClt-AppSvr. in msec */
    public static final int TIMEOUT = 4000;

    /* if NO is voted by AppServer, there is a reason:
     * 1) the sub-tran is aborted on AppServer;
     * 2) the AppServer is out of reach (time out). 
     *
     * BTW, YES needs no reason. */
    public static final String REASON_TIMEOUT  = "timeout";
    public static final String REASON_SVR_DOWN = "svrdown";
    public static final String REASON_VOTE_NO  = "voteno";

    public static final String COMMIT   = "CMT";
    public static final String ABORT    = "ABT";

    public static final String MOVE     = "MV";
    public static final String MOVE_OUT = "MVOUT";
    public static final String MOVE_IN  = "MVIN";

    public static final String SEND_YES = "Y";  /* sent to others */
    public static final String SEND_NO  = "N";  /* sent to others */
    /* public static final String SEND_ACK = "A";  sent to others */

    public static final int YES = 1; /* method return value */
    public static final int NO  = 0; /* method return value */
}

