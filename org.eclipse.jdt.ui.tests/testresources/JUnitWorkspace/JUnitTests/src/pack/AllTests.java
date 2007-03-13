package pack;

import junit.framework.Test;
import junit.framework.TestSuite;


public class AllTests {

	public static Test suite() {
		TestSuite suite= new TestSuite("All the Tests");
		suite.addTest(ATestSuite.suite());
		suite.addTestSuite(Failures.class);
		return suite;
	}

}
