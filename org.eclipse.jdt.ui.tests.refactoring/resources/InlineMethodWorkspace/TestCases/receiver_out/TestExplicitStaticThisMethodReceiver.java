package receiver_out;

public class TestExplicitStaticThisMethodReceiver {
    static Logger3 getLogger() {
    	return Logger3.getLogger("");
    }
    protected Logger3 getLOG() {
        return getLogger();
    }
    public void ref() {
    	getLogger().info("message");
    }
}

class Logger3 {
	public static Logger3 getLogger(String string) {
		return null;
	}
	public void info(String string) {
	}
}