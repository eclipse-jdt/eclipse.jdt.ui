package junit.tests;

import junit.framework.*;
import junit.extensions.*;

public class ExceptionTestCaseTest extends junit.framework.TestCase {
/**
 * ExceptionTestCaseTest constructor comment.
 * @param name java.lang.String
 */
public ExceptionTestCaseTest(String name) {
	super(name);
}
public void testExceptionSubclass() {
	ExceptionTestCase test= new ExceptionTestCase("test", Exception.class) {
		public void test() {
			throw new IndexOutOfBoundsException();
		}
	};
	TestResult result= test.run();
	assertEquals(1, result.runCount());
	assert(result.wasSuccessful());
}
public void testExceptionTest() {
	ExceptionTestCase test= new ExceptionTestCase("test", IndexOutOfBoundsException.class) {
		public void test() {
			throw new IndexOutOfBoundsException();
		}
	};
	TestResult result= test.run();
	assertEquals(1, result.runCount());
	assert(result.wasSuccessful());
}
public void testFailure() {
	ExceptionTestCase test= new ExceptionTestCase("test", IndexOutOfBoundsException.class) {
		public void test() {
			throw new RuntimeException();
		}
	};
	TestResult result= test.run();
	assertEquals(1, result.runCount());
	assertEquals(1, result.errorCount());
}
public void testNoException() {
	ExceptionTestCase test= new ExceptionTestCase("test", Exception.class) {
		public void test() {
		}
	};
	TestResult result= test.run();
	assertEquals(1, result.runCount());
	assertEquals(1, result.failureCount());
}
public void testWrongException() {
	ExceptionTestCase test= new ExceptionTestCase("test", IndexOutOfBoundsException.class) {
		public void test() {
			throw new RuntimeException();
		}
	};
	TestResult result= test.run();
	assertEquals(1, result.runCount());
	assertEquals(1, result.errorCount());
}
}