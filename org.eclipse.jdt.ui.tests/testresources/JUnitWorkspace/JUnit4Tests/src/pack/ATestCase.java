package pack;
import org.junit.Test;
public class ATestCase {
	@Test public void testSucceed() { }
    @Test public void testSucceed2() throws InterruptedException {
    	Thread.sleep(1000);
    }
}