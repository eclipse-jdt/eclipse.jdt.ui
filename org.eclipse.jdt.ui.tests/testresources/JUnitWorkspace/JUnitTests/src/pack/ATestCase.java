package pack;
import junit.framework.TestCase;
public class ATestCase extends TestCase {
    public void testSucceed() { }
    public void testSucceed2() throws InterruptedException {
    	Thread.sleep(1000);
    }
}