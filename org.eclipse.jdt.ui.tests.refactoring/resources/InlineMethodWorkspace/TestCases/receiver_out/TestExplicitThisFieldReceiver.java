package receiver_out;

public class TestExplicitThisFieldReceiver {
    Logger LOG = Logger.getLogger("");
    
    protected Logger getLOG() {
        return LOG;
    }
    public void ref() {
    	this.LOG.info("message");
    }
}

class Logger {
	public static Logger getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}