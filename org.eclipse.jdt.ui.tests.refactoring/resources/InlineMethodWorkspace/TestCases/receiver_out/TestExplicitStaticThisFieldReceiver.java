package receiver_out;

public class TestExplicitStaticThisFieldReceiver {
    static Logger2 LOG = Logger2.getLogger("");
    
    protected Logger2 getLOG() {
        return LOG;
    }
    public void ref() {
    	LOG.info("message");
    }
}

class Logger2 {
	public static Logger2 getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}