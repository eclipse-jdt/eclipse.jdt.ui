package pack;
import junit.framework.*;
public class ATestSuite {
	public static Test suite() {
		TestSuite suite= new TestSuite("Test Suite");
		suite.addTest(ATestCase.suite());
		return suite;
	}
}