package junit.tests;

import junit.framework.*;

/**
 * TestSuite that runs all the sample tests
 *
 */
public class AllTests {

	private static boolean inVAJava() {
		try {
			Class.forName("com.ibm.uvm.tools.DebugSupport");
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}
	public static void main (String[] args) {
		junit.textui.TestRunner.run(suite());
	}
	public static Test suite ( ) {
		TestSuite suite= new TestSuite("Framework Tests");
		suite.addTest(new TestSuite(ExtensionTest.class));
	    suite.addTest(new TestSuite(TestTest.class));
	    suite.addTest(SuiteTest.suite()); // Tests suite building, so can't use it 
		if (!inVAJava())
			suite.addTest(new TestSuite(TestTestCaseClassLoader.class));
	    return suite;
	}
}