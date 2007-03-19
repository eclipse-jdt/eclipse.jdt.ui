package pack;

import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class FailingSuite extends TestCase {
	private static class MySetup extends TestSetup {
		public MySetup(Test test) {
			super(test);
		}
		protected void setUp() throws Exception {
			super.setUp();
			fail("failure in setUp");
		}
	}
	
	public static Test suite() {
		return new MySetup(new TestSuite(FailingSuite.class));
	}
	
	public void test1() throws Exception {
		
	}
}
