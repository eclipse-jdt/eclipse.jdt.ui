package pack;

import org.junit.Test;
import org.junit.runner.RunWith;

import pack.OrderedRunner.Order;

@RunWith(OrderedRunner.class)
@Order({"testSucceed", "testSucceed2"})
public class ATestCase {
	@Test public void testSucceed() { }
    @Test public void testSucceed2() throws InterruptedException {
    	Thread.sleep(1000);
    }
}