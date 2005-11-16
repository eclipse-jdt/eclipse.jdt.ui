package org.eclipse.jdt.junit.tests;

import junit.framework.Test;
import junit.framework.TestSuite;

public class JUnitJUnitTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Test for org.eclipse.jdt.junit.tests");
		//$JUnit-BEGIN$
		
		// TODO disabled unreliable tests driving the event loop:
//		suite.addTestSuite(WrappingSystemTest.class);
//		suite.addTestSuite(WrappingUnitTest.class);
		
		suite.addTestSuite(TestPriorization.class);
		suite.addTestSuite(TestTestSearchEngine.class);
		//$JUnit-END$
		return suite;
	}

}
