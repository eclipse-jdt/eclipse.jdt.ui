package junit.swingui;

import junit.framework.*;
import junit.util.*;

/**
 * A convenience class to use TestRunner that can reload classes.
 * Enter the name of a class with a suite method which should return
 * the tests to be run.
 * <pre>
 * Synopsis: java java.swingui.LoadingTestRunner [TestCase]
 * </pre>
 * TestRunner takes as an optional argument the name of the testcase class to be run.
 * @see TestCaseClassLoader
 */
public class LoadingTestRunner {
	/**
	 * main entrypoint
	 */
	public static void main(String[] args) {
		new TestRunner().start(args, new ReloadingTestSuiteLoader());
	}
}