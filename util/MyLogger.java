package util;

import java.util.logging.*;

public class MyLogger {

    public static final String logpath = "./logs";

    public static Logger getInstance(String cname) {
	try {
	    Logger LOGGER = null; 
	    String classname = null;

	    FileHandler logf;
	    SimpleFormatter fmter;

	    classname = cname; 
	    LOGGER = Logger.getLogger(classname);
	    LOGGER.setLevel(Level.INFO); 

	    logf = new FileHandler(logpath + "/" + classname, true);
	    fmter = new SimpleFormatter();
	    logf.setFormatter(fmter);
	    LOGGER.addHandler(logf);

	    LOGGER.setUseParentHandlers(false);

	    return LOGGER;
	} catch (Exception e) {
	    e.printStackTrace();
	}

	return null;
    }


}
