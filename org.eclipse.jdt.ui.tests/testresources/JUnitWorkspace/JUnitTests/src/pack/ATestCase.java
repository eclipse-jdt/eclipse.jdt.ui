package pack;
import junit.framework.*;
public class ATestCase extends TestCase {
	public static Test suite() {
		TestSuite suite= new TestSuite(ATestCase.class.getName());
		suite.addTest(new ATestCase("testSucceed"));
		suite.addTest(new ATestCase("testSucceed2"));
		return suite;
	}
	public ATestCase(String name) {
		super(name);
	}
    public void testSucceed() { }
    public void testSucceed2() throws InterruptedException {
    	Thread.sleep(1000);
    }
}