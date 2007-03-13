package pack;
import junit.framework.*;
public class ATestSuite {
	public static Test suite() {
		TestSuite suite= new TestSuite("Test Suite");
		suite.addTestSuite(ATestCase.class);
		return suite;
	}
}