package pack;

import junit.framework.*;

public class Failures extends TestCase {
	
	public static Test suite() {
		TestSuite suite= new TestSuite(Failures.class.getName());
		suite.addTest(new Failures("testNasty"));
		suite.addTest(new Failures("testError"));
		suite.addTest(new Failures("testCompare"));
		suite.addTest(new Failures("testCompareNull"));
		return suite;
	}
	public Failures(String name) {
		super(name);
	}

	public void testNasty() throws Exception {
		fail("</failure>");
	}

	public void testError() throws Exception {
		throw new IllegalStateException("</failure>");
	}
	
	public void testCompare() throws Exception {
		assertEquals("\nHello World.\n\n", "\n\nHello my friend.");
	}
	
	public void testCompareNull() throws Exception {
		assertEquals("Hello World.", null);
	}
}
